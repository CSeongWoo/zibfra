package com.example.zipfra.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.zipfra.config.PublicDataProperties;
import com.example.zipfra.dto.ingest.RealEstateRow;
import com.example.zipfra.dto.ingest.RegionAgg;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.postgis.IngestionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link IngestionService} 구현. 국토부 아파트 매매 API → 지번주소 지오코딩 → PostGIS 적재.
 * PostGIS 쓰기이므로 spatialTransactionManager(§3·§4 공공데이터 적재본은 PostGIS 전용).
 */
@Slf4j
@Service
public class IngestionServiceImpl implements IngestionService {

    private static final int NUM_OF_ROWS = 1000;
    private static final String SGG_CSV = "data/lawd_sgg.csv";

    /**
     * 전국 시군구 LAWD_CD → [표시명, 지오코딩 주소]. classpath {@code data/lawd_sgg.csv}에서 로드.
     * 정적 참조 데이터(법정동 시군구 코드)라 매 배치마다 외부 API 의존 없이 번들 파일로 관리한다(§9).
     * 삽입 순서 유지(LinkedHashMap)로 SUMMARY 배치가 시도→시군구 순으로 돈다.
     */
    private static final Map<String, String[]> REGIONS = loadRegions();

    private static Map<String, String[]> loadRegions() {
        Map<String, String[]> map = new LinkedHashMap<>();
        try (InputStream in = IngestionServiceImpl.class.getClassLoader().getResourceAsStream(SGG_CSV)) {
            if (in == null) {
                throw new IllegalStateException("시군구 코드 파일 없음: " + SGG_CSV);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("code,")) continue;
                String[] f = line.split(",", 3);
                if (f.length < 3) continue;
                map.put(f[0].trim(), new String[] { f[1].trim(), f[2].trim() });
            }
        } catch (Exception e) {
            throw new IllegalStateException("시군구 코드 로드 실패: " + e.getMessage(), e);
        }
        return map;
    }

    private final RestTemplate publicDataRestTemplate;
    private final PublicDataProperties props;
    private final GeocodingService geocodingService;
    private final IngestionMapper ingestionMapper;
    /** 전국 배치에서 시군구별 @Transactional 경계를 살리기 위한 자기 프록시(self-invocation 우회). */
    private final ObjectProvider<IngestionService> selfProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestionServiceImpl(RestTemplate publicDataRestTemplate,
                                PublicDataProperties props,
                                GeocodingService geocodingService,
                                IngestionMapper ingestionMapper,
                                ObjectProvider<IngestionService> selfProvider) {
        this.publicDataRestTemplate = publicDataRestTemplate;
        this.props = props;
        this.geocodingService = geocodingService;
        this.ingestionMapper = ingestionMapper;
        this.selfProvider = selfProvider;
    }

    /** 실거래가 소스 6종: 종류(APT/OFFICETEL/ROW_HOUSE) × 거래(매매/전월세). pathKey 는 application.yml paths. */
    private enum Source {
        APT_TRADE("APT_TRADE", "APT", false),
        APT_RENT("APT_RENT", "APT", true),
        OFFI_TRADE("OFFI_TRADE", "OFFICETEL", false),
        OFFI_RENT("OFFI_RENT", "OFFICETEL", true),
        RH_TRADE("RH_TRADE", "ROW_HOUSE", false),
        RH_RENT("RH_RENT", "ROW_HOUSE", true);

        final String pathKey;
        final String propertyType;
        final boolean rent;

        Source(String pathKey, String propertyType, boolean rent) {
            this.pathKey = pathKey;
            this.propertyType = propertyType;
            this.rent = rent;
        }

        static Source of(String name) {
            for (Source s : values()) {
                if (s.name().equalsIgnoreCase(name)) return s;
            }
            throw new ApiException(ErrorCode.INVALID_PARAM, "지원하지 않는 source: " + name);
        }
    }

    /** 국토부 실거래가 API 호출 → JsonNode(응답 Content-Type 불안정 → String 받아 파싱). */
    private JsonNode fetch(String pathKey, String lawdCd, String dealYmd) {
        String path = (props.getPaths() != null) ? props.getPaths().get(pathKey) : null;
        if (path == null) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, pathKey + " 엔드포인트 미설정");
        }
        URI uri = UriComponentsBuilder.fromUriString(props.getBaseUrl() + path)
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("pageNo", 1)
                .encode()
                .build()
                .toUri();
        try {
            String body = publicDataRestTemplate.getForObject(uri, String.class);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UPSTREAM_CLIENT_ERROR, "국토부 API 호출/파싱 실패: " + e.getMessage());
        }
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public int ingestRealEstate(String source, String lawdCd, String dealYmd) {
        Source s = Source.of(source);
        JsonNode resp = fetch(s.pathKey, lawdCd, dealYmd);
        JsonNode items = resp.path("response").path("body").path("items").path("item");

        // 재적재 멱등: 동일 지역·월·종류의 거래유형 삭제(전월세 소스는 JEONSE/WOLSE 둘 다)
        if (s.rent) {
            ingestionMapper.deleteRealEstate(lawdCd, dealYmd, "JEONSE", s.propertyType);
            ingestionMapper.deleteRealEstate(lawdCd, dealYmd, "WOLSE", s.propertyType);
        } else {
            ingestionMapper.deleteRealEstate(lawdCd, dealYmd, "SALE", s.propertyType);
        }

        // 단지 좌표 캐싱: 같은 단지(name|jibun)의 여러 거래는 좌표 동일 → 지오코딩 1회만.
        Map<String, double[]> coordCache = new HashMap<>();
        int ingested = 0;
        int skipped = 0;
        if (items != null && items.isArray()) {
            for (JsonNode it : items) {
                if (ingestOne(it, s, lawdCd, dealYmd, coordCache)) ingested++;
                else skipped++;
            }
        } else if (items != null && items.isObject()) {
            if (ingestOne(items, s, lawdCd, dealYmd, coordCache)) ingested++;
            else skipped++;
        }
        log.info("[ingest] src={} lawd={} ym={} 적재={} 스킵={}", source, lawdCd, dealYmd, ingested, skipped);
        return ingested;
    }

    private boolean ingestOne(JsonNode it, Source s, String lawdCd, String dealYmd, Map<String, double[]> coordCache) {
        try {
            String umd = text(it, "umdNm");
            String jibun = text(it, "jibun");
            // 단지명: 종류별 필드명 상이(아파트 aptNm / 오피스텔 offiNm / 연립 mhouseNm)
            String name = firstNonBlank(text(it, "aptNm"), text(it, "offiNm"), text(it, "mhouseNm"));
            // 지오코딩 주소: estateAgentSggNm 있으면 사용, 비면 CSV 풀주소(REGIONS)로 보강(빌라는 비는 경우 많음)
            String sgg = text(it, "estateAgentSggNm");
            String prefix = !sgg.isBlank() ? sgg : regionAddr(lawdCd);
            String address = (prefix + " " + umd + " " + jibun).trim();

            String cacheKey = name + "|" + jibun;
            double[] coord;
            if (coordCache.containsKey(cacheKey)) {
                coord = coordCache.get(cacheKey);     // null = 이전 지오코딩 실패한 단지
            } else {
                coord = geocodingService.geocode(address).orElse(null);
                coordCache.put(cacheKey, coord);
            }
            if (coord == null) {
                return false;
            }

            Long dealAmount = null, deposit = null;
            Integer monthlyRent = null;
            String dealType;
            if (s.rent) {
                deposit = parseAmount(text(it, "deposit"));
                Long m = parseAmount(text(it, "monthlyRent"));
                monthlyRent = (m != null) ? m.intValue() : 0;
                dealType = (monthlyRent > 0) ? "WOLSE" : "JEONSE";
            } else {
                dealAmount = parseAmount(text(it, "dealAmount"));
                dealType = "SALE";
            }

            RealEstateRow row = RealEstateRow.builder()
                    .buildingName(name)
                    .dealType(dealType)
                    .propertyType(s.propertyType)
                    .dealAmount(dealAmount)
                    .deposit(deposit)
                    .monthlyRent(monthlyRent)
                    .exclusiveArea(it.path("excluUseAr").asDouble())
                    .floorNo(it.path("floor").asInt())
                    .buildYear(it.path("buildYear").asInt())
                    .dealYm(dealYmd)
                    .lawdCd(lawdCd)
                    .jibun(jibun)
                    .lng(coord[0])
                    .lat(coord[1])
                    .build();
            ingestionMapper.insertRealEstate(row);
            return true;
        } catch (Exception e) {
            log.warn("[ingest] 행 적재 실패: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int ingestRealEstateNationwide(String dealYmd, String sourcesCsv) {
        IngestionService self = selfProvider.getObject();   // 프록시 경유 → (시군구,소스)별 @Transactional 적용
        List<Source> sources = parseSources(sourcesCsv);
        int total = 0;
        int size = REGIONS.size();
        for (Source s : sources) {
            int srcTotal = 0;
            int i = 0;
            for (Map.Entry<String, String[]> e : REGIONS.entrySet()) {
                i++;
                String code = e.getKey();
                try {
                    int n = self.ingestRealEstate(s.name(), code, dealYmd);
                    srcTotal += n;
                    total += n;
                } catch (Exception ex) {
                    log.warn("[nationwide] {} ({}/{}) {} 실패: {}", s, i, size, code, ex.getMessage());
                }
                if (i % 50 == 0 || i == size) {
                    log.info("[nationwide] {} ({}/{}) 누적={}", s, i, size, srcTotal);
                }
            }
            log.info("[nationwide] {} 완료: {}건", s, srcTotal);
        }
        log.info("[nationwide] 전국 매물 적재 완료: 총 {}건 (sources={})", total, sources);
        return total;
    }

    /** sourcesCsv 가 비면 6종 전부, 아니면 지정 소스만. */
    private static List<Source> parseSources(String sourcesCsv) {
        if (sourcesCsv == null || sourcesCsv.isBlank()) {
            return List.of(Source.values());
        }
        List<Source> out = new ArrayList<>();
        for (String t : sourcesCsv.split(",")) {
            if (!t.isBlank()) out.add(Source.of(t.trim()));
        }
        return out;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private static String regionAddr(String lawdCd) {
        String[] r = REGIONS.get(lawdCd);
        return (r != null) ? r[1] : "";
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public int refreshRegionSummary() {
        ingestionMapper.clearRegionSummary();
        List<RegionAgg> aggs = ingestionMapper.aggregateRealEstateByLawd();
        for (RegionAgg a : aggs) {
            String[] r = REGIONS.get(a.getLawdCd());
            String name = (r != null) ? r[0] : a.getLawdCd();
            ingestionMapper.insertRegionSummary(
                    a.getLawdCd(), name, a.getCenterLon(), a.getCenterLat(),
                    a.getDealCount(), a.getAvgAmount(), a.getMaxAmount());
        }
        log.info("[region-summary] 실집계 완료: {} 개 시군구", aggs.size());
        return aggs.size();
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public int ingestRegionSummaryFromApi(String dealYmd) {
        ingestionMapper.clearRegionSummary();
        int n = 0;
        for (Map.Entry<String, String[]> e : REGIONS.entrySet()) {
            String sggCd = e.getKey();
            String name = e.getValue()[0];
            String addr = e.getValue()[1];
            JsonNode resp;
            try {
                resp = fetch("APT_TRADE", sggCd, dealYmd);
            } catch (Exception ex) {
                log.warn("[region-summary-api] {} 호출 실패: {}", sggCd, ex.getMessage());
                continue;
            }
            JsonNode body = resp.path("response").path("body");
            int totalCount = body.path("totalCount").asInt(0);
            if (totalCount == 0) {
                continue;
            }
            // 받은 페이지(numOfRows)로 평균·최고가 집계
            JsonNode items = body.path("items").path("item");
            long sum = 0;
            long max = 0;
            int cnt = 0;
            if (items.isArray()) {
                for (JsonNode it : items) {
                    Long a = parseAmount(text(it, "dealAmount"));
                    if (a != null) { sum += a; max = Math.max(max, a); cnt++; }
                }
            } else if (items.isObject()) {
                Long a = parseAmount(text(items, "dealAmount"));
                if (a != null) { sum = a; max = a; cnt = 1; }
            }
            Long avg = (cnt > 0) ? sum / cnt : null;
            Long maxAmount = (cnt > 0) ? max : null;

            Optional<double[]> coord = geocodingService.geocode(addr);
            if (coord.isEmpty()) {
                log.warn("[region-summary-api] {} 중심 지오코딩 실패", addr);
                continue;
            }
            ingestionMapper.insertRegionSummary(sggCd, name, coord.get()[0], coord.get()[1], totalCount, avg, maxAmount);
            n++;
        }
        log.info("[region-summary-api] {} 개 시군구 집계 완료", n);
        return n;
    }

    private static String text(JsonNode n, String field) {
        return n.path(field).asText("").trim();
    }

    private static Long parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw.replaceAll("[, ]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

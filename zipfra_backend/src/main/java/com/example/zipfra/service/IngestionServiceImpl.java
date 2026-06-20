package com.example.zipfra.service;

import java.net.URI;
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

    /** 서울 25개 자치구 시군구코드 → 이름(SUMMARY 풍선 라벨). 미등록 코드는 코드 그대로 표시. */
    private static final Map<String, String> SGG_NAME = Map.ofEntries(
            Map.entry("11110", "종로구"), Map.entry("11140", "중구"), Map.entry("11170", "용산구"),
            Map.entry("11200", "성동구"), Map.entry("11215", "광진구"), Map.entry("11230", "동대문구"),
            Map.entry("11260", "중랑구"), Map.entry("11290", "성북구"), Map.entry("11305", "강북구"),
            Map.entry("11320", "도봉구"), Map.entry("11350", "노원구"), Map.entry("11380", "은평구"),
            Map.entry("11410", "서대문구"), Map.entry("11440", "마포구"), Map.entry("11470", "양천구"),
            Map.entry("11500", "강서구"), Map.entry("11530", "구로구"), Map.entry("11545", "금천구"),
            Map.entry("11560", "영등포구"), Map.entry("11590", "동작구"), Map.entry("11620", "관악구"),
            Map.entry("11650", "서초구"), Map.entry("11680", "강남구"), Map.entry("11710", "송파구"),
            Map.entry("11740", "강동구"));

    private final RestTemplate publicDataRestTemplate;
    private final PublicDataProperties props;
    private final GeocodingService geocodingService;
    private final IngestionMapper ingestionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestionServiceImpl(RestTemplate publicDataRestTemplate,
                                PublicDataProperties props,
                                GeocodingService geocodingService,
                                IngestionMapper ingestionMapper) {
        this.publicDataRestTemplate = publicDataRestTemplate;
        this.props = props;
        this.geocodingService = geocodingService;
        this.ingestionMapper = ingestionMapper;
    }

    /** 국토부 아파트 매매 API 호출 → JsonNode(응답 Content-Type 불안정 → String 받아 파싱). */
    private JsonNode fetchAptTrade(String lawdCd, String dealYmd) {
        String path = (props.getPaths() != null) ? props.getPaths().get("MOLIT_APT") : null;
        if (path == null) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "MOLIT_APT 엔드포인트 미설정");
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
    public int ingestAptTrade(String lawdCd, String dealYmd) {
        JsonNode resp = fetchAptTrade(lawdCd, dealYmd);
        JsonNode items = resp.path("response").path("body").path("items").path("item");

        // 재적재 멱등: 기존 동일 지역·월 SALE/APT 삭제
        ingestionMapper.deleteAptTrade(lawdCd, dealYmd);

        int ingested = 0;
        int skipped = 0;
        if (items != null && items.isArray()) {
            for (JsonNode it : items) {
                if (ingestOne(it, lawdCd, dealYmd)) ingested++;
                else skipped++;
            }
        } else if (items != null && items.isObject()) {
            if (ingestOne(items, lawdCd, dealYmd)) ingested++;
            else skipped++;
        }
        log.info("[ingest] lawd={} ym={} 적재={} 스킵(지오코딩실패)={}", lawdCd, dealYmd, ingested, skipped);
        return ingested;
    }

    private boolean ingestOne(JsonNode it, String lawdCd, String dealYmd) {
        try {
            String umd = text(it, "umdNm");
            String jibun = text(it, "jibun");
            String sgg = text(it, "estateAgentSggNm"); // 예: "서울 강남구"
            String address = (sgg + " " + umd + " " + jibun).trim();

            Optional<double[]> coord = geocodingService.geocode(address);
            if (coord.isEmpty()) {
                return false;
            }
            RealEstateRow row = RealEstateRow.builder()
                    .buildingName(text(it, "aptNm"))
                    .dealAmount(parseAmount(text(it, "dealAmount")))
                    .exclusiveArea(it.path("excluUseAr").asDouble())
                    .floorNo(it.path("floor").asInt())
                    .buildYear(it.path("buildYear").asInt())
                    .dealYm(dealYmd)
                    .lawdCd(lawdCd)
                    .jibun(jibun)
                    .lng(coord.get()[0])
                    .lat(coord.get()[1])
                    .build();
            ingestionMapper.insertAptTrade(row);
            return true;
        } catch (Exception e) {
            log.warn("[ingest] 행 적재 실패: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public int refreshRegionSummary() {
        ingestionMapper.clearRegionSummary();
        List<RegionAgg> aggs = ingestionMapper.aggregateRealEstateByLawd();
        for (RegionAgg a : aggs) {
            String name = SGG_NAME.getOrDefault(a.getLawdCd(), a.getLawdCd());
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
        for (Map.Entry<String, String> e : SGG_NAME.entrySet()) {
            String sggCd = e.getKey();
            String name = e.getValue();
            JsonNode resp;
            try {
                resp = fetchAptTrade(sggCd, dealYmd);
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

            Optional<double[]> coord = geocodingService.geocode("서울 " + name);
            if (coord.isEmpty()) {
                log.warn("[region-summary-api] {} 중심 지오코딩 실패", name);
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

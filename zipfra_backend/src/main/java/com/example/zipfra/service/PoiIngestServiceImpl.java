package com.example.zipfra.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.zipfra.dto.ingest.GridCell;
import com.example.zipfra.dto.map.Bbox;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.postgis.IngestionMapper;
import com.example.zipfra.util.BboxValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link PoiIngestService} 구현. 카카오 카테고리 검색을 bbox 격자로 순회하며 POI 적재.
 * 우리 카테고리(대문자) ↔ 카카오 category_group_code 매핑. BUS_STOP 은 카카오 미지원이라 제외.
 */
@Slf4j
@Service
public class PoiIngestServiceImpl implements PoiIngestService {

    /** 우리 poi.category(대문자) → 카카오 category_group_code. */
    private static final Map<String, String> CATEGORY_MAP = Map.ofEntries(
            Map.entry("SUBWAY", "SW8"),
            Map.entry("SCHOOL", "SC4"),
            Map.entry("ACADEMY", "AC5"),
            Map.entry("RESTAURANT", "FD6"),
            Map.entry("CAFE", "CE7"),
            Map.entry("MART", "MT1"),
            Map.entry("CINEMA", "CT1"),
            Map.entry("CONVENIENCE_STORE", "CS2"),
            Map.entry("HOSPITAL", "HP8"),
            Map.entry("PHARMACY", "PM9"),
            Map.entry("BANK", "BK9"));

    private static final double STEP = 0.02;  // 격자 약 2km
    private static final int MAX_PAGE = 3;    // 카카오 카테고리 검색 최대 45건(15×3)

    private final RestTemplate publicDataRestTemplate;
    private final IngestionMapper ingestionMapper;
    private final String restApiKey;
    private final String categoryUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PoiIngestServiceImpl(RestTemplate publicDataRestTemplate,
                                IngestionMapper ingestionMapper,
                                @Value("${kakao.rest-api-key:}") String restApiKey,
                                @Value("${kakao.category-url:https://dapi.kakao.com/v2/local/search/category.json}") String categoryUrl) {
        this.publicDataRestTemplate = publicDataRestTemplate;
        this.ingestionMapper = ingestionMapper;
        this.restApiKey = (restApiKey != null) ? restApiKey.trim() : "";
        this.categoryUrl = categoryUrl;
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public int ingestPois(String bboxRaw) {
        Bbox b = BboxValidator.parse(bboxRaw);
        ingestionMapper.clearPoi(); // 더미/기존 제거 후 실데이터 재적재

        Set<String> seen = new HashSet<>();
        int total = 0;
        for (double x = b.minLng(); x < b.maxLng(); x += STEP) {
            for (double y = b.minLat(); y < b.maxLat(); y += STEP) {
                double x2 = Math.min(x + STEP, b.maxLng());
                double y2 = Math.min(y + STEP, b.maxLat());
                String rect = x + "," + y + "," + x2 + "," + y2;
                total += fetchAllCategories(rect, seen);
            }
        }
        log.info("[poi-ingest] bbox={} 적재 {} 건", bboxRaw, total);
        return total;
    }

    @Override
    public int ingestPoisNationwide() {
        List<GridCell> cells = ingestionMapper.findPropertyGridCells();
        ingestionMapper.clearPoi();   // 1회 비움 후 매물 격자만 적재

        Set<String> seen = new HashSet<>();
        int total = 0;
        int i = 0;
        int size = cells.size();
        for (GridCell c : cells) {
            i++;
            double x = c.getMinLng();
            double y = c.getMinLat();
            String rect = x + "," + y + "," + (x + STEP) + "," + (y + STEP);
            total += fetchAllCategories(rect, seen);
            if (i % 100 == 0 || i == size) {
                log.info("[poi-nationwide] ({}/{}) 격자 처리, 누적 {} 건", i, size, total);
            }
        }
        log.info("[poi-nationwide] 전국 POI 적재 완료: 격자 {} 개, 총 {} 건", size, total);
        return total;
    }

    /** 버스정류장 CSV 경로(국토부 전국 버스정류장 위치정보, data.go.kr 15067528). UTF-8 인코딩 필수. */
    private static final String BUS_CSV = "data/bus_stops.csv";
    /** 버스 중복제거 격자(도). 同名 + ~44m 격자 = 같은 물리적 정류장(방향·노선별 중복 등록) → 1개만. */
    private static final double BUS_DEDUP_GRID = 0.0004;

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public int ingestBusStops() {
        // clearPoi 아님 — BUS_STOP 만 비우고 재적재(카카오 POI 보존, 멱등).
        ingestionMapper.deletePoiByCategory("BUS_STOP");

        int ingested = 0;
        int skipped = 0;
        int duped = 0;
        // 同名 + 좌표격자 키로 중복 행 제거(방향·노선별 중복 등록 → 1개만). 재현 가능하게 코드에 내장.
        Set<String> seen = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(openBusCsv(), StandardCharsets.UTF_8))) {
            String header = nextDataLine(br);
            if (header == null) {
                throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, BUS_CSV + " 가 비어 있습니다(헤더 없음).");
            }
            String[] cols = splitCsv(header);
            int iName = idxOf(cols, "정류장명", "정류소명", "명칭", "name");
            int iLat = idxOf(cols, "위도", "lat", "y");
            int iLng = idxOf(cols, "경도", "lng", "lon", "x");
            if (iLat < 0 || iLng < 0) {
                throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR,
                        BUS_CSV + " 헤더에서 위도/경도 컬럼을 찾지 못했습니다: " + header);
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] f = splitCsv(line);
                Double lat = parseCoord(f, iLat);
                Double lng = parseCoord(f, iLng);
                if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
                    skipped++;
                    continue;   // 좌표 결측·0 행 skip
                }
                String name = (iName >= 0 && iName < f.length) ? f[iName].trim() : "";
                // 중복제거: 同名 + ~44m 격자 = 같은 정류장 → 첫 1개만 적재
                String key = name + "|" + (long) Math.floor(lng / BUS_DEDUP_GRID)
                        + "|" + (long) Math.floor(lat / BUS_DEDUP_GRID);
                if (!seen.add(key)) {
                    duped++;
                    continue;
                }
                ingestionMapper.insertPoi("BUS_STOP", name, lng, lat);
                ingested++;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "버스정류장 CSV 적재 실패: " + e.getMessage());
        }
        log.info("[bus-stop] 버스정류장 적재 {} 건 (중복제거 {}, 좌표결측 스킵 {})", ingested, duped, skipped);
        return ingested;
    }

    /** classpath 의 버스 CSV 스트림. 없으면 안내 메시지와 함께 예외. */
    private InputStream openBusCsv() {
        InputStream in = getClass().getClassLoader().getResourceAsStream(BUS_CSV);
        if (in == null) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR,
                    BUS_CSV + " 파일이 없습니다. data.go.kr 15067528 CSV 를 UTF-8 로 저장해 넣으세요.");
        }
        return in;
    }

    /** 다음 비주석·비공백 라인(헤더 탐색용). 선두 BOM 제거. */
    private static String nextDataLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty() && line.charAt(0) == '﻿') {
                line = line.substring(1);   // UTF-8 BOM 제거
            }
            if (!line.isBlank() && !line.startsWith("#")) {
                return line;
            }
        }
        return null;
    }

    /** 헤더에서 키워드(부분일치, 대소문자 무시) 컬럼 인덱스. 없으면 -1. */
    private static int idxOf(String[] header, String... keys) {
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim().toLowerCase();
            for (String k : keys) {
                if (h.contains(k.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static Double parseCoord(String[] f, int idx) {
        if (idx < 0 || idx >= f.length) {
            return null;
        }
        try {
            return Double.parseDouble(f[idx].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 최소 CSV 분리 — 따옴표(") 안의 콤마는 보존(정류장명에 콤마가 있는 경우 대비). */
    private static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }

    /** 한 격자(rect)에 대해 전 카테고리 검색·적재. */
    private int fetchAllCategories(String rect, Set<String> seen) {
        int added = 0;
        for (Map.Entry<String, String> e : CATEGORY_MAP.entrySet()) {
            added += fetchCategory(e.getKey(), e.getValue(), rect, seen);
        }
        return added;
    }

    private int fetchCategory(String ourCategory, String code, String rect, Set<String> seen) {
        int added = 0;
        for (int page = 1; page <= MAX_PAGE; page++) {
            try {
                URI uri = UriComponentsBuilder.fromUriString(categoryUrl)
                        .queryParam("category_group_code", code)
                        .queryParam("rect", rect)
                        .queryParam("size", 15)
                        .queryParam("page", page)
                        .encode()
                        .build()
                        .toUri();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "KakaoAK " + restApiKey);
                ResponseEntity<String> resp = publicDataRestTemplate.exchange(
                        uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                JsonNode root = objectMapper.readTree(resp.getBody());

                for (JsonNode d : root.path("documents")) {
                    double lng = d.path("x").asDouble();
                    double lat = d.path("y").asDouble();
                    String name = d.path("place_name").asText("");
                    String key = ourCategory + ":" + Math.round(lng * 1e5) + ":" + Math.round(lat * 1e5);
                    if (lng != 0 && lat != 0 && seen.add(key)) {
                        ingestionMapper.insertPoi(ourCategory, name, lng, lat);
                        added++;
                    }
                }
                if (root.path("meta").path("is_end").asBoolean(true)) {
                    break;
                }
            } catch (Exception e) {
                log.warn("[poi-ingest] {} {} p{} 실패: {}", ourCategory, rect, page, e.getMessage());
                break;
            }
        }
        return added;
    }
}

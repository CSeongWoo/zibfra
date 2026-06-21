package com.example.zipfra.service;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.zipfra.dto.ingest.GridCell;
import com.example.zipfra.dto.map.Bbox;
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

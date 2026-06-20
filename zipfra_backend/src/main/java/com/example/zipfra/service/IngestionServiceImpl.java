package com.example.zipfra.service;

import java.net.URI;
import java.util.Optional;

import com.example.zipfra.config.PublicDataProperties;
import com.example.zipfra.dto.ingest.RealEstateRow;
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

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public int ingestAptTrade(String lawdCd, String dealYmd) {
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

        JsonNode resp;
        try {
            // 국토부 응답 Content-Type 이 일정치 않아 String 으로 받아 직접 파싱
            String body = publicDataRestTemplate.getForObject(uri, String.class);
            resp = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UPSTREAM_CLIENT_ERROR, "국토부 API 호출/파싱 실패: " + e.getMessage());
        }
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

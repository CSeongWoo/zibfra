package com.example.zipfra.controller;

import java.util.Map;

import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.service.IngestionService;
import com.example.zipfra.service.PoiIngestService;
import com.example.zipfra.service.PropertyScoreBatch;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 적재 운영 트리거 (§9, Protected). 데모/초기 적재용 수동 엔드포인트.
 * 정식 심야 배치 전 단계 — 한 지역·월 단위로 실거래가를 적재한다.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final IngestionService ingestionService;
    private final PoiIngestService poiIngestService;
    private final PropertyScoreBatch propertyScoreBatch;

    public AdminController(IngestionService ingestionService,
                          PoiIngestService poiIngestService,
                          PropertyScoreBatch propertyScoreBatch) {
        this.ingestionService = ingestionService;
        this.poiIngestService = poiIngestService;
        this.propertyScoreBatch = propertyScoreBatch;
    }

    @PostMapping("/ingest/real-estate")
    public ResponseEntity<Map<String, Object>> ingestRealEstate(
            @RequestParam String lawdCd,
            @RequestParam String dealYmd) {

        if (!lawdCd.matches("\\d{5}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "lawdCd 는 숫자 5자리여야 합니다: " + lawdCd);
        }
        if (!dealYmd.matches("\\d{6}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "dealYmd 는 YYYYMM 6자리여야 합니다: " + dealYmd);
        }
        int ingested = ingestionService.ingestAptTrade(lawdCd, dealYmd);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("lawdCd", lawdCd, "dealYmd", dealYmd, "ingested", ingested));
    }

    /** POI 실데이터 적재(카카오 카테고리 검색, §9). bbox=minLng,minLat,maxLng,maxLat */
    @PostMapping("/ingest/poi")
    public ResponseEntity<Map<String, Object>> ingestPoi(@RequestParam String bbox) {
        int ingested = poiIngestService.ingestPois(bbox);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("bbox", bbox, "ingested", ingested));
    }

    /** 매물 점수 전체 재계산(§5.1). 매물·POI 적재 후 호출 — 최신 POI 기준 base 갱신. */
    @PostMapping("/recompute-scores")
    public ResponseEntity<Map<String, Object>> recomputeScores() {
        int recomputed = propertyScoreBatch.recompute();
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("recomputed", recomputed));
    }
}

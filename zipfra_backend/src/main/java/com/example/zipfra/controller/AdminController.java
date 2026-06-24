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
            @RequestParam(defaultValue = "APT_TRADE") String source,
            @RequestParam String lawdCd,
            @RequestParam String dealYmd) {

        if (!lawdCd.matches("\\d{5}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "lawdCd 는 숫자 5자리여야 합니다: " + lawdCd);
        }
        if (!dealYmd.matches("\\d{6}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "dealYmd 는 YYYYMM 6자리여야 합니다: " + dealYmd);
        }
        int ingested = ingestionService.ingestRealEstate(source, lawdCd, dealYmd);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("source", source, "lawdCd", lawdCd, "dealYmd", dealYmd, "ingested", ingested));
    }

    /** 실거래가 추이 적재(좌표·지오코딩 없음). 한 시군구의 fromYmd~toYmd 월 순회 × 소스 → building_price_history. */
    @PostMapping("/ingest/price-history")
    public ResponseEntity<Map<String, Object>> ingestPriceHistory(
            @RequestParam String lawdCd,
            @RequestParam String fromYmd,
            @RequestParam String toYmd,
            @RequestParam(required = false) String sources) {

        if (!lawdCd.matches("\\d{5}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "lawdCd 는 숫자 5자리여야 합니다: " + lawdCd);
        }
        if (!fromYmd.matches("\\d{6}") || !toYmd.matches("\\d{6}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "fromYmd/toYmd 는 YYYYMM 6자리여야 합니다");
        }
        int rows = ingestionService.ingestPriceHistoryRange(lawdCd, fromYmd, toYmd, sources);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("lawdCd", lawdCd, "fromYmd", fromYmd, "toYmd", toYmd, "rows", rows));
    }

    /** 전국 추이 적재(250 시군구 × fromYmd~toYmd × 소스). 지오코딩 없음. 수십 분~시간·MOLIT 쿼터 주의. 멱등(재실행 이어서). */
    @PostMapping("/ingest/price-history/nationwide")
    public ResponseEntity<Map<String, Object>> ingestPriceHistoryNationwide(
            @RequestParam String fromYmd,
            @RequestParam String toYmd,
            @RequestParam(required = false) String sources) {

        if (!fromYmd.matches("\\d{6}") || !toYmd.matches("\\d{6}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "fromYmd/toYmd 는 YYYYMM 6자리여야 합니다");
        }
        int rows = ingestionService.ingestPriceHistoryNationwide(fromYmd, toYmd, sources);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("fromYmd", fromYmd, "toYmd", toYmd, "sources", sources != null ? sources : "ALL", "rows", rows));
    }

    /** 전국 244개 시군구 실거래가 DETAIL 일괄 적재(§9). sources 비면 6종 전부, 지정 시 해당 소스만. 수 분~수십 분 소요. */
    @PostMapping("/ingest/real-estate/nationwide")
    public ResponseEntity<Map<String, Object>> ingestRealEstateNationwide(
            @RequestParam String dealYmd,
            @RequestParam(required = false) String sources) {
        if (!dealYmd.matches("\\d{6}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "dealYmd 는 YYYYMM 6자리여야 합니다: " + dealYmd);
        }
        int ingested = ingestionService.ingestRealEstateNationwide(dealYmd, sources);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("dealYmd", dealYmd, "sources", sources != null ? sources : "ALL", "ingested", ingested));
    }

    /** POI 실데이터 적재(카카오 카테고리 검색, §9). bbox=minLng,minLat,maxLng,maxLat */
    @PostMapping("/ingest/poi")
    public ResponseEntity<Map<String, Object>> ingestPoi(@RequestParam String bbox) {
        int ingested = poiIngestService.ingestPois(bbox);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("bbox", bbox, "ingested", ingested));
    }

    /** 전국 POI 적재(§9). 매물 존재 격자만 순회(빈 격자 낭비 회피). 카카오 쿼터·시간 소요 — 수십 분~수 시간. */
    @PostMapping("/ingest/poi/nationwide")
    public ResponseEntity<Map<String, Object>> ingestPoiNationwide() {
        int ingested = poiIngestService.ingestPoisNationwide();
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("ingested", ingested));
    }

    /** 매물 점수 전체 재계산(§5.1). 매물·POI 적재 후 호출 — 최신 POI 기준 base 갱신. */
    @PostMapping("/recompute-scores")
    public ResponseEntity<Map<String, Object>> recomputeScores() {
        int recomputed = propertyScoreBatch.recompute();
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("recomputed", recomputed));
    }

    /** region_summary 실집계 갱신(§7 줌아웃 SUMMARY). 적재 매물 기준 시군구 집계. */
    @PostMapping("/refresh-summary")
    public ResponseEntity<Map<String, Object>> refreshSummary() {
        int regions = ingestionService.refreshRegionSummary();
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("regions", regions));
    }

    /** 전국(서울 25구) SUMMARY 배치(§7). 국토부 시군구 집계 — DETAIL 적재 없이 region_summary 채움. */
    @PostMapping("/ingest/region-summary")
    public ResponseEntity<Map<String, Object>> ingestRegionSummary(@RequestParam String dealYmd) {
        if (!dealYmd.matches("\\d{6}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "dealYmd 는 YYYYMM 6자리여야 합니다: " + dealYmd);
        }
        int regions = ingestionService.ingestRegionSummaryFromApi(dealYmd);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .body(Map.of("dealYmd", dealYmd, "regions", regions));
    }
}

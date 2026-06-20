package com.example.zipfra.service;

/**
 * 실거래가 적재 (§9). 국토부 OpenAPI → 카카오 지오코딩 → PostGIS {@code real_estate_sales} 적재.
 * 수동 트리거(admin). 동일 지역·월 재적재 시 멱등(삭제 후 insert).
 */
public interface IngestionService {

    /**
     * 아파트 매매 실거래가 적재.
     * @param lawdCd  법정동 코드 앞 5자리(예 강남구 11680)
     * @param dealYmd 계약년월 YYYYMM
     * @return 적재 성공 건수(지오코딩 실패분 제외)
     */
    int ingestAptTrade(String lawdCd, String dealYmd);

    /**
     * region_summary 실집계 갱신(§7 줌아웃 SUMMARY). 적재된 real_estate_sales 를 시군구별 집계해
     * 거래건수·평균/최고가·중심좌표를 region_summary 에 재생성한다.
     * @return 집계된 시군구 수
     */
    int refreshRegionSummary();

    /**
     * 국토부 API 시군구별 집계로 region_summary 채움(§7 줌아웃 SUMMARY, 전국용).
     * DETAIL 적재(지오코딩 대량) 없이 건수·평균/최고가만 집계, 중심좌표는 시군구명 지오코딩 1회.
     * @param dealYmd 계약년월 YYYYMM
     * @return 집계된 시군구 수
     */
    int ingestRegionSummaryFromApi(String dealYmd);
}

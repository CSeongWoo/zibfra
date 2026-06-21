package com.example.zipfra.service;

/**
 * 실거래가 적재 (§9). 국토부 OpenAPI → 카카오 지오코딩 → PostGIS {@code real_estate_sales} 적재.
 * 수동 트리거(admin). 동일 지역·월 재적재 시 멱등(삭제 후 insert).
 */
public interface IngestionService {

    /**
     * 실거래가 1종·1시군구 적재. 종류·거래유형 일반화(아파트/오피스텔/연립다세대 × 매매/전월세).
     * @param source  소스 키(APT_TRADE/APT_RENT/OFFI_TRADE/OFFI_RENT/RH_TRADE/RH_RENT)
     * @param lawdCd  법정동 코드 앞 5자리(예 강남구 11680)
     * @param dealYmd 계약년월 YYYYMM
     * @return 적재 성공 건수(지오코딩 실패분 제외)
     */
    int ingestRealEstate(String source, String lawdCd, String dealYmd);

    /**
     * 전국 244개 시군구 실거래가 일괄 적재(DETAIL 전국 확대, §9).
     * (시군구,소스)별 독립 트랜잭션(중간 실패 보존, 재실행 멱등). 동일 단지 좌표 캐싱은 시군구 내 적용.
     * @param dealYmd    계약년월 YYYYMM
     * @param sourcesCsv 적재할 소스 CSV(비면 6종 전부). 예 "RH_TRADE,OFFI_TRADE"
     * @return 적재 성공 건수 합계(지오코딩 실패분 제외)
     */
    int ingestRealEstateNationwide(String dealYmd, String sourcesCsv);

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

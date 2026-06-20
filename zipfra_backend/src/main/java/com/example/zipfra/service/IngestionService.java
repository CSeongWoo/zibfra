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
}

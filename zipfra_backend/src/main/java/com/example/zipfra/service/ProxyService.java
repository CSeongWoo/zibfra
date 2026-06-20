package com.example.zipfra.service;

/**
 * PUB-02 공공데이터 실시간 중계 (§8.1). 외부 국토부 실거래가 OpenAPI 를 직접 호출해 원본을 중계한다.
 * 배치 미반영 당일 단건 전용(대량 금지). 키 은닉·CORS 차단 목적.
 */
public interface ProxyService {

    /**
     * @param source    MOLIT_APT | MOLIT_ROW | COMMERCE (application.yml paths 키)
     * @param dealYear  계약 연도(YYYY)
     * @param dealMonth 계약 월(1~12)
     * @param lawdCd    법정동 코드 앞 5자리
     * @return 외부 API 원본 응답 본문(XML)
     * @throws com.example.zipfra.exception.ApiException
     *         INVALID_PARAM / UPSTREAM_TIMEOUT(504) / UPSTREAM_RATE_LIMIT(503) / UPSTREAM_CLIENT_ERROR(502)
     */
    String getRealtime(String source, int dealYear, int dealMonth, String lawdCd);
}

package com.example.zipfra.service;

import java.util.Map;

/**
 * 공공데이터 프록시 (§8.1). PUB-01(적재본 조회) + PUB-02(실시간 중계). 키 은닉·CORS 차단 목적.
 */
public interface ProxyService {

    /**
     * PUB-01 배치 적재본 조회.
     * @param type       REAL_ESTATE | COMMERCE
     * @param regionCode 법정동 10자리(REAL_ESTATE 는 앞 5자리=lawd_cd 로 필터)
     * @param page       0-based(생략 0)
     * @param size       max 100, 생략 20
     * @return {type, regionCode, content[], page, size, totalCount, hasNext}
     */
    Map<String, Object> getPublicData(String type, String regionCode, Integer page, Integer size);

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

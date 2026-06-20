package com.example.zipfra.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.zipfra.config.PublicDataProperties;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.postgis.PublicDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link ProxyService} 구현. RestTemplate 로 국토부 OpenAPI 직접 호출.
 * 타임아웃/연결 실패 → retry 2회(1초) 후 504, HTTP 429 → 503, 4xx → 502 (§8.1).
 */
@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    private static final int MAX_ATTEMPTS = 3;     // 최초 1 + retry 2 (§8.1)
    private static final long RETRY_DELAY_MS = 1000L;
    private static final int NUM_OF_ROWS = 100;
    private static final int PUB_MAX_SIZE = 100;
    private static final int PUB_DEF_SIZE = 20;

    private final RestTemplate publicDataRestTemplate;
    private final PublicDataProperties props;
    private final PublicDataMapper publicDataMapper;

    public ProxyServiceImpl(RestTemplate publicDataRestTemplate,
                            PublicDataProperties props,
                            PublicDataMapper publicDataMapper) {
        this.publicDataRestTemplate = publicDataRestTemplate;
        this.props = props;
        this.publicDataMapper = publicDataMapper;
    }

    @Override
    @Transactional(transactionManager = "spatialTransactionManager", readOnly = true, propagation = Propagation.SUPPORTS)
    public Map<String, Object> getPublicData(String type, String regionCode, Integer pageParam, Integer sizeParam) {
        boolean realEstate = "REAL_ESTATE".equals(type);
        if (!realEstate && !"COMMERCE".equals(type)) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "type 은 REAL_ESTATE|COMMERCE 여야 합니다: " + type);
        }
        if (regionCode == null || !regionCode.matches("\\d{10}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "regionCode 는 숫자 10자리여야 합니다: " + regionCode);
        }
        int size = (sizeParam != null) ? sizeParam : PUB_DEF_SIZE;
        if (size > PUB_MAX_SIZE) {
            throw new ApiException(ErrorCode.PAGE_SIZE_EXCEEDED, "size 는 최대 " + PUB_MAX_SIZE + " 입니다: " + size);
        }
        int page = (pageParam != null) ? pageParam : 0;
        long offset = (long) page * size;

        long total;
        List<?> content;
        if (realEstate) {
            String lawdCd = regionCode.substring(0, 5);
            total = publicDataMapper.countRealEstateByLawd(lawdCd);
            content = publicDataMapper.findRealEstateByLawd(lawdCd, size, offset);
        } else {
            total = publicDataMapper.countCommerce();
            content = publicDataMapper.findCommerce(size, offset);
        }
        boolean hasNext = (long) (page + 1) * size < total;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("regionCode", regionCode);
        result.put("content", content);
        result.put("page", page);
        result.put("size", size);
        result.put("totalCount", total);
        result.put("hasNext", hasNext);
        return result;
    }

    @Override
    public String getRealtime(String source, int dealYear, int dealMonth, String lawdCd) {
        String path = (props.getPaths() != null) ? props.getPaths().get(source) : null;
        if (path == null) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "지원하지 않는 source 입니다: " + source);
        }
        if (dealMonth < 1 || dealMonth > 12) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "dealMonth 는 1~12 여야 합니다: " + dealMonth);
        }
        if (lawdCd == null || !lawdCd.matches("\\d{5}")) {
            throw new ApiException(ErrorCode.INVALID_PARAM, "lawd_cd 는 숫자 5자리여야 합니다: " + lawdCd);
        }

        String dealYmd = String.format("%04d%02d", dealYear, dealMonth);
        URI uri = UriComponentsBuilder.fromUriString(props.getBaseUrl() + path)
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("pageNo", 1)
                .encode()
                .build()
                .toUri();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return publicDataRestTemplate.getForEntity(uri, String.class).getBody();
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status == 429) {
                    throw new ApiException(ErrorCode.UPSTREAM_RATE_LIMIT, "외부 API 호출 한도 초과(429).");
                }
                if (e.getStatusCode().is4xxClientError()) {
                    throw new ApiException(ErrorCode.UPSTREAM_CLIENT_ERROR, "외부 API 4xx 응답: " + status);
                }
                // 5xx → 재시도 대상
                if (attempt == MAX_ATTEMPTS) {
                    throw new ApiException(ErrorCode.UPSTREAM_CLIENT_ERROR, "외부 API 5xx 응답: " + status);
                }
            } catch (ResourceAccessException e) {
                // 연결 실패·read 타임아웃(5초) → 재시도, 소진 시 504
                if (attempt == MAX_ATTEMPTS) {
                    throw new ApiException(ErrorCode.UPSTREAM_TIMEOUT, "외부 API 응답 지연(타임아웃).");
                }
            }
            sleep();
        }
        throw new ApiException(ErrorCode.UPSTREAM_TIMEOUT, "외부 API 호출 실패.");
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

package com.example.zipfra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.example.zipfra.config.PublicDataProperties;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.mapper.postgis.PublicDataMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ProxyServiceTest {

    @Mock
    private RestTemplate publicDataRestTemplate;
    @Mock
    private PublicDataProperties props;
    @Mock
    private PublicDataMapper publicDataMapper;

    private ProxyServiceImpl service() {
        return new ProxyServiceImpl(publicDataRestTemplate, props, publicDataMapper);
    }

    private void stubProps() {
        lenient().when(props.getPaths()).thenReturn(Map.of("MOLIT_APT", "/p/getApt"));
        lenient().when(props.getBaseUrl()).thenReturn("http://upstream");
        lenient().when(props.getServiceKey()).thenReturn("KEY");
    }

    // ===== PUB-02 getRealtime (T-5) =====

    @Test
    @DisplayName("PUB-02: 정상 응답 → 원본 body 중계")
    void realtime_ok() {
        stubProps();
        when(publicDataRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("UPSTREAM_BODY"));
        assertThat(service().getRealtime("MOLIT_APT", 2024, 5, "11680")).isEqualTo("UPSTREAM_BODY");
    }

    @Test
    @DisplayName("T-5: 외부 429 → 503 UPSTREAM_RATE_LIMIT")
    void realtime_429() {
        stubProps();
        when(publicDataRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "rate", HttpHeaders.EMPTY, null, null));
        assertThatThrownBy(() -> service().getRealtime("MOLIT_APT", 2024, 5, "11680"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UPSTREAM_RATE_LIMIT);
    }

    @Test
    @DisplayName("T-5: 외부 4xx → 502 UPSTREAM_CLIENT_ERROR")
    void realtime_4xx() {
        stubProps();
        when(publicDataRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY, null, null));
        assertThatThrownBy(() -> service().getRealtime("MOLIT_APT", 2024, 5, "11680"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UPSTREAM_CLIENT_ERROR);
    }

    @Test
    @DisplayName("T-5: 타임아웃(retry 소진) → 504 UPSTREAM_TIMEOUT")
    void realtime_timeout() {
        stubProps();
        when(publicDataRestTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("read timed out"));
        assertThatThrownBy(() -> service().getRealtime("MOLIT_APT", 2024, 5, "11680"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UPSTREAM_TIMEOUT);
    }

    @Test
    @DisplayName("PUB-02: 지원 안 하는 source → INVALID_PARAM")
    void realtime_badSource() {
        when(props.getPaths()).thenReturn(Map.of("MOLIT_APT", "/p"));
        assertThatThrownBy(() -> service().getRealtime("NOPE", 2024, 5, "11680"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAM);
    }

    // ===== PUB-01 getPublicData =====

    @Test
    @DisplayName("PUB-01: type 불량 → INVALID_PARAM")
    void pub01_badType() {
        assertThatThrownBy(() -> service().getPublicData("XXX", "1168000000", null, null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAM);
    }

    @Test
    @DisplayName("PUB-01: regionCode 10자리 아님 → INVALID_PARAM")
    void pub01_badRegion() {
        assertThatThrownBy(() -> service().getPublicData("REAL_ESTATE", "123", null, null))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAM);
    }

    @Test
    @DisplayName("PUB-01: size>100 → PAGE_SIZE_EXCEEDED")
    void pub01_sizeExceeded() {
        assertThatThrownBy(() -> service().getPublicData("REAL_ESTATE", "1168000000", 0, 101))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAGE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("PUB-01: REAL_ESTATE → lawd_cd(앞5) 필터 + totalCount/hasNext")
    void pub01_realEstate() {
        when(publicDataMapper.countRealEstateByLawd(eq("11680"))).thenReturn(342L);
        when(publicDataMapper.findRealEstateByLawd(eq("11680"), anyInt(), anyLong())).thenReturn(List.of());

        Map<String, Object> res = service().getPublicData("REAL_ESTATE", "1168000000", 0, 100);
        assertThat(res.get("type")).isEqualTo("REAL_ESTATE");
        assertThat(res.get("totalCount")).isEqualTo(342L);
        assertThat(res.get("hasNext")).isEqualTo(true);
    }

    @Test
    @DisplayName("PUB-01: COMMERCE → poi 상업 카테고리 조회")
    void pub01_commerce() {
        when(publicDataMapper.countCommerce()).thenReturn(10L);
        when(publicDataMapper.findCommerce(anyInt(), anyLong())).thenReturn(List.of());

        Map<String, Object> res = service().getPublicData("COMMERCE", "1168000000", 0, 20);
        assertThat(res.get("type")).isEqualTo("COMMERCE");
        assertThat(res.get("totalCount")).isEqualTo(10L);
        assertThat(res.get("hasNext")).isEqualTo(false);
    }
}

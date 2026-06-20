package com.example.zipfra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import com.example.zipfra.mapper.postgis.IngestionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PoiIngestServiceTest {

    @Mock
    private RestTemplate publicDataRestTemplate;
    @Mock
    private IngestionMapper ingestionMapper;

    // 카카오 카테고리 검색 응답(1건, is_end)
    private static final String CAT_JSON =
            "{\"documents\":[{\"x\":\"127.05\",\"y\":\"37.51\",\"place_name\":\"테스트장소\"}],"
            + "\"meta\":{\"is_end\":true}}";

    private PoiIngestServiceImpl service() {
        return new PoiIngestServiceImpl(publicDataRestTemplate, ingestionMapper,
                "KAKAO_KEY", "https://dapi.kakao.com/v2/local/search/category.json");
    }

    @Test
    @DisplayName("POI 적재: clearPoi 후 카테고리별 insertPoi")
    void ingestPois() {
        when(publicDataRestTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(CAT_JSON));

        // 1격자(작은 bbox)
        int n = service().ingestPois("127.04,37.50,127.05,37.51");

        assertThat(n).isGreaterThan(0); // 11개 카테고리 × 1건
        verify(ingestionMapper).clearPoi();
        verify(ingestionMapper, atLeastOnce()).insertPoi(any(), any(), anyDouble(), anyDouble());
    }
}

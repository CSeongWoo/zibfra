package com.example.zipfra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import com.example.zipfra.config.PublicDataProperties;
import com.example.zipfra.mapper.postgis.IngestionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

    @Mock
    private RestTemplate publicDataRestTemplate;
    @Mock
    private PublicDataProperties props;
    @Mock
    private GeocodingService geocodingService;
    @Mock
    private IngestionMapper ingestionMapper;

    private static final String ONE_ITEM_JSON =
            "{\"response\":{\"body\":{\"items\":{\"item\":[{"
            + "\"aptNm\":\"테스트아파트\",\"dealAmount\":\"690,000\",\"excluUseAr\":84.5,"
            + "\"floor\":3,\"buildYear\":2020,\"umdNm\":\"역삼동\",\"jibun\":\"736\","
            + "\"estateAgentSggNm\":\"서울 강남구\"}]}}}}";

    private IngestionServiceImpl service() {
        return new IngestionServiceImpl(publicDataRestTemplate, props, geocodingService, ingestionMapper);
    }

    private void stubApi() {
        when(props.getPaths()).thenReturn(Map.of("MOLIT_APT", "/p/getApt"));
        when(props.getBaseUrl()).thenReturn("http://upstream");
        when(props.getServiceKey()).thenReturn("KEY");
        when(publicDataRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(ONE_ITEM_JSON);
    }

    @Test
    @DisplayName("적재: 지오코딩 성공 → delete 후 1건 insert")
    void ingest_geocoded() {
        stubApi();
        when(geocodingService.geocode(any())).thenReturn(Optional.of(new double[] { 127.03, 37.50 }));

        int n = service().ingestAptTrade("11680", "202405");

        assertThat(n).isEqualTo(1);
        verify(ingestionMapper).deleteAptTrade("11680", "202405");
        verify(ingestionMapper).insertAptTrade(any());
    }

    @Test
    @DisplayName("적재: 지오코딩 실패 → 스킵(insert 안 함)")
    void ingest_geocodeFail_skip() {
        stubApi();
        when(geocodingService.geocode(any())).thenReturn(Optional.empty());

        int n = service().ingestAptTrade("11680", "202405");

        assertThat(n).isZero();
        verify(ingestionMapper).deleteAptTrade("11680", "202405");
        verify(ingestionMapper, never()).insertAptTrade(any());
    }
}

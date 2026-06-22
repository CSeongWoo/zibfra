package com.example.zipfra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.beans.factory.ObjectProvider;
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
    @Mock
    private ObjectProvider<IngestionService> selfProvider;

    private static final String ONE_ITEM_JSON =
            "{\"response\":{\"body\":{\"items\":{\"item\":[{"
            + "\"aptNm\":\"테스트아파트\",\"dealAmount\":\"690,000\",\"excluUseAr\":84.5,"
            + "\"floor\":3,\"buildYear\":2020,\"umdNm\":\"역삼동\",\"jibun\":\"736\","
            + "\"estateAgentSggNm\":\"서울 강남구\"}]}}}}";

    // 같은 단지(aptNm+jibun) 2건 + 다른 단지 1건 → 지오코딩은 2회만 기대(캐시 적중)
    private static final String DUP_ITEMS_JSON =
            "{\"response\":{\"body\":{\"items\":{\"item\":["
            + "{\"aptNm\":\"래미안\",\"dealAmount\":\"690,000\",\"excluUseAr\":84.5,\"floor\":3,\"buildYear\":2020,\"umdNm\":\"역삼동\",\"jibun\":\"736\",\"estateAgentSggNm\":\"서울 강남구\"},"
            + "{\"aptNm\":\"래미안\",\"dealAmount\":\"700,000\",\"excluUseAr\":84.5,\"floor\":9,\"buildYear\":2020,\"umdNm\":\"역삼동\",\"jibun\":\"736\",\"estateAgentSggNm\":\"서울 강남구\"},"
            + "{\"aptNm\":\"자이\",\"dealAmount\":\"800,000\",\"excluUseAr\":99.0,\"floor\":5,\"buildYear\":2018,\"umdNm\":\"역삼동\",\"jibun\":\"800\",\"estateAgentSggNm\":\"서울 강남구\"}"
            + "]}}}}";

    private IngestionServiceImpl service() {
        return new IngestionServiceImpl(publicDataRestTemplate, props, geocodingService, ingestionMapper, selfProvider);
    }

    // 전월세(아파트) 응답: 전세(monthlyRent 0) + 월세(monthlyRent>0)
    private static final String RENT_ITEMS_JSON =
            "{\"response\":{\"body\":{\"items\":{\"item\":["
            + "{\"aptNm\":\"래미안\",\"deposit\":\"50,000\",\"monthlyRent\":\"0\",\"excluUseAr\":84.5,\"floor\":3,\"buildYear\":2020,\"umdNm\":\"역삼동\",\"jibun\":\"736\",\"estateAgentSggNm\":\"서울 강남구\"},"
            + "{\"aptNm\":\"자이\",\"deposit\":\"10,000\",\"monthlyRent\":\"150\",\"excluUseAr\":59.0,\"floor\":5,\"buildYear\":2018,\"umdNm\":\"역삼동\",\"jibun\":\"800\",\"estateAgentSggNm\":\"서울 강남구\"}"
            + "]}}}}";

    private void stubApi(String json) {
        when(props.getPaths()).thenReturn(Map.of("APT_TRADE", "/p/trade", "APT_RENT", "/p/rent"));
        when(props.getBaseUrl()).thenReturn("http://upstream");
        when(props.getServiceKey()).thenReturn("KEY");
        when(publicDataRestTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(json);
    }

    @Test
    @DisplayName("적재(매매): 지오코딩 성공 → SALE/APT delete 후 1건 insert")
    void ingest_geocoded() {
        stubApi(ONE_ITEM_JSON);
        when(geocodingService.geocode(any())).thenReturn(Optional.of(new double[] { 127.03, 37.50 }));

        int n = service().ingestRealEstate("APT_TRADE", "11680", "202405");

        assertThat(n).isEqualTo(1);
        verify(ingestionMapper).deleteRealEstate("11680", "202405", "SALE", "APT");
        verify(ingestionMapper).insertRealEstate(any());
    }

    @Test
    @DisplayName("적재: 지오코딩 실패 → 스킵(insert 안 함)")
    void ingest_geocodeFail_skip() {
        stubApi(ONE_ITEM_JSON);
        when(geocodingService.geocode(any())).thenReturn(Optional.empty());

        int n = service().ingestRealEstate("APT_TRADE", "11680", "202405");

        assertThat(n).isZero();
        verify(ingestionMapper).deleteRealEstate("11680", "202405", "SALE", "APT");
        verify(ingestionMapper, never()).insertRealEstate(any());
    }

    @Test
    @DisplayName("적재: 동일 단지(name|jibun) 중복 거래는 좌표 캐싱 → 지오코딩 1회만")
    void ingest_dedupGeocodeByComplex() {
        stubApi(DUP_ITEMS_JSON);
        when(geocodingService.geocode(any())).thenReturn(Optional.of(new double[] { 127.03, 37.50 }));

        int n = service().ingestRealEstate("APT_TRADE", "11680", "202405");

        assertThat(n).isEqualTo(3);                         // 3건 모두 적재
        verify(ingestionMapper, times(3)).insertRealEstate(any());
        verify(geocodingService, times(2)).geocode(any());   // 단지 2개 → 2회만(래미안 캐시 적중)
    }

    @Test
    @DisplayName("적재(전월세): JEONSE/WOLSE delete + monthlyRent 로 전세/월세 분류")
    void ingest_rent_classifiesJeonseWolse() {
        stubApi(RENT_ITEMS_JSON);
        when(geocodingService.geocode(any())).thenReturn(Optional.of(new double[] { 127.03, 37.50 }));

        int n = service().ingestRealEstate("APT_RENT", "11680", "202405");

        assertThat(n).isEqualTo(2);
        verify(ingestionMapper).deleteRealEstate("11680", "202405", "JEONSE", "APT");
        verify(ingestionMapper).deleteRealEstate("11680", "202405", "WOLSE", "APT");
        // 전세 1건(monthlyRent 0) + 월세 1건(monthlyRent>0)
        verify(ingestionMapper).insertRealEstate(org.mockito.ArgumentMatchers.argThat(
                r -> "JEONSE".equals(r.getDealType()) && r.getMonthlyRent() == 0 && r.getDealAmount() == null));
        verify(ingestionMapper).insertRealEstate(org.mockito.ArgumentMatchers.argThat(
                r -> "WOLSE".equals(r.getDealType()) && r.getMonthlyRent() == 150));
    }
}

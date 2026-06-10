package com.example.zipfra.service;

import com.example.zipfra.dto.location.PoiDistanceDTO;
import com.example.zipfra.dto.location.ScoreRequest;
import com.example.zipfra.dto.location.ScoreResponse;
import com.example.zipfra.mapper.postgis.PoiMapper;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LOC-01 입지 점수 서비스 검증 단위테스트 (AGENTS.md §2.1 검증 케이스).
 *
 * <p>여기서 PoiMapper 는 진짜 PostGIS 대신 Mockito 로 만든 '가짜(Mock)'다.
 * 미리 정해둔 값을 돌려주므로 DB 없이 검증 로직만 떼서 테스트할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class LocationScoreServiceImplTest {

    @Mock
    private PoiMapper poiMapper;   // ← 가짜 부품(Mock): 진짜 PostGIS 안 띄움

    // 계산기는 순수 객체라 Mock 이 아니라 실물을 주입한다
    private final LocationScoreCalculator calculator = new LocationScoreCalculator();

    private LocationScoreServiceImpl service() {
        return new LocationScoreServiceImpl(poiMapper, calculator);
    }

    private ScoreRequest request(Double lon, Double lat, Integer radius, Map<String, Double> weights) {
        ScoreRequest req = new ScoreRequest();
        req.setLon(lon);
        req.setLat(lat);
        req.setRadiusMeters(radius);
        req.setWeights(weights);
        return req;
    }

    @Test
    @DisplayName("정상: radiusMeters 생략 시 기본 1500, Mock POI 로 점수 산출")
    void score_defaultRadius_ok() {
        // Mock 이 "약국 320m 1개 있다"고 응답하도록 미리 지정
        when(poiMapper.isInSeoul(anyDouble(), anyDouble())).thenReturn(false);
        PoiDistanceDTO pharmacy = new PoiDistanceDTO();
        pharmacy.setCategory("PHARMACY");
        pharmacy.setDistanceMeters(320);
        when(poiMapper.findWithin(anyDouble(), anyDouble(), anyInt())).thenReturn(List.of(pharmacy));

        ScoreResponse response = service().score(
                request(127.0533, 37.5072, null, Map.of("pharmacy", 0.8)));

        assertThat(response.radiusMeters()).isEqualTo(1500);     // 기본값 적용
        assertThat(response.finalScore()).isEqualTo(0.800);
        verify(poiMapper).findWithin(127.0533, 37.5072, 1500);   // 기본 반경으로 호출됐는지 검증
    }

    @Test
    @DisplayName("radiusMeters=99 → RADIUS_OUT_OF_RANGE (DB 조회 전 차단)")
    void score_radiusTooSmall() {
        assertThatThrownBy(() -> service().score(
                request(127.0, 37.5, 99, Map.of())))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RADIUS_OUT_OF_RANGE);
        verifyNoInteractions(poiMapper);   // 검증 실패 시 Mock 도 호출 안 됨
    }

    @Test
    @DisplayName("radiusMeters=3001 → RADIUS_OUT_OF_RANGE")
    void score_radiusTooLarge() {
        assertThatThrownBy(() -> service().score(
                request(127.0, 37.5, 3001, Map.of())))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RADIUS_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("weights 값 1.1 → WEIGHT_OUT_OF_RANGE")
    void score_weightOutOfRange() {
        assertThatThrownBy(() -> service().score(
                request(127.0, 37.5, 1500, Map.of("mart", 1.1))))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WEIGHT_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("lat=91 → COORD_OUT_OF_RANGE")
    void score_coordOutOfRange() {
        assertThatThrownBy(() -> service().score(
                request(127.0, 91.0, 1500, Map.of())))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COORD_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("lon 누락 → INVALID_PARAM")
    void score_missingLon() {
        assertThatThrownBy(() -> service().score(
                request(null, 37.5, 1500, Map.of())))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PARAM);
    }
}

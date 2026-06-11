package com.example.zipfra.service;

import com.example.zipfra.dto.location.PoiDistanceDTO;
import com.example.zipfra.dto.location.ScoreResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * LOC-01 입지 점수 감쇠·합산 단위테스트 (AGENTS.md §2.1 T-7, §5).
 * 순수 계산기라 DB·Spring 없이 검증한다.
 */
class LocationScoreCalculatorTest {

    private final LocationScoreCalculator calculator = new LocationScoreCalculator();

    private static PoiDistanceDTO poi(String category, double meters) {
        PoiDistanceDTO p = new PoiDistanceDTO();
        p.setCategory(category);
        p.setDistanceMeters(meters);
        return p;
    }

    @Test
    @DisplayName("T-7: pharmacy 1개 320m, w=0.8 → contribution 0.800")
    void essential_oneIsEnough_pharmacy() {
        var result = calculator.calculate(
                List.of(poi("PHARMACY", 320)),
                Map.of("pharmacy", 0.8),
                false);

        ScoreResponse.CategoryScore pharmacy =
                result.breakdown().essential().categories().get("pharmacy");
        assertThat(pharmacy.base()).isEqualTo(1.0);             // t=4.0 ≤ 5 → W=1
        assertThat(pharmacy.contribution()).isEqualTo(0.800);
        assertThat(result.finalScore()).isEqualTo(0.800);
    }

    @Test
    @DisplayName("T-7: mart 1개 850m, w=0.6 → contribution 0.133")
    void essential_oneIsEnough_mart() {
        var result = calculator.calculate(
                List.of(poi("MART", 850)),
                Map.of("mart", 0.6),
                false);

        ScoreResponse.CategoryScore mart =
                result.breakdown().essential().categories().get("mart");
        // t=10.625 > 5 → W = 1/(2.125)² ≈ 0.2214
        assertThat(mart.base()).isCloseTo(0.2214, within(0.0001));
        assertThat(mart.contribution()).isEqualTo(0.133);
    }

    @Test
    @DisplayName("T-7: restaurant 2개[200m,600m], w=0.9 → contribution 1.300 (more_is_better ΣW)")
    void leisure_moreIsBetter_restaurant() {
        var result = calculator.calculate(
                List.of(poi("RESTAURANT", 200), poi("RESTAURANT", 600)),
                Map.of("restaurant", 0.9),
                false);

        ScoreResponse.CategoryScore restaurant =
                result.breakdown().leisure().categories().get("restaurant");
        assertThat(restaurant.count()).isEqualTo(2);
        // W=[1.0, 0.4444] → Σ=1.4444 → ×0.9 = 1.300
        assertThat(restaurant.base()).isCloseTo(1.4444, within(0.0001));
        assertThat(restaurant.contribution()).isEqualTo(1.300);
    }

    @Test
    @DisplayName("경계: t=5.0(=400m) 정확히 → W=1.0 (≤5 분기)")
    void decayBoundary_exactlyFiveMinutes() {
        var result = calculator.calculate(
                List.of(poi("PHARMACY", 400)),
                Map.of("pharmacy", 1.0),
                false);
        assertThat(result.breakdown().essential().categories().get("pharmacy").base())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("essential one_is_enough: 같은 카테고리 여러개여도 가장 가까운 1개의 W만")
    void oneIsEnough_usesNearestOnly() {
        var result = calculator.calculate(
                List.of(poi("PHARMACY", 320), poi("PHARMACY", 1200)),
                Map.of("pharmacy", 1.0),
                false);
        ScoreResponse.CategoryScore pharmacy =
                result.breakdown().essential().categories().get("pharmacy");
        assertThat(pharmacy.count()).isEqualTo(2);
        assertThat(pharmacy.nearestMeters()).isEqualTo(320);
        assertThat(pharmacy.base()).isEqualTo(1.0);   // 320m 만 반영, 1200m 무시
    }

    @Test
    @DisplayName("ENV_PENALTY: 서울 좌표 + noise 1개 → contribution 음수, applicable=true")
    void envPenalty_inSeoul_negativeContribution() {
        var result = calculator.calculate(
                List.of(poi("NOISE", 320)),
                Map.of("noise", 0.5),
                true);

        ScoreResponse.GroupResult env = result.breakdown().envPenalty();
        assertThat(env.applicable()).isTrue();
        ScoreResponse.CategoryScore noise = env.categories().get("noise");
        assertThat(noise.contribution()).isEqualTo(-0.500);   // base 1.0 × 0.5, 음수
        assertThat(result.finalScore()).isEqualTo(-0.500);
    }

    @Test
    @DisplayName("ENV_PENALTY: 서울 외 좌표 → 항목 제외(applicable=false), finalScore 에서 빠짐(0 아님)")
    void envPenalty_outsideSeoul_excluded() {
        var result = calculator.calculate(
                List.of(poi("NOISE", 100), poi("PHARMACY", 320)),
                Map.of("noise", 1.0, "pharmacy", 0.8),
                false);

        ScoreResponse.GroupResult env = result.breakdown().envPenalty();
        assertThat(env.applicable()).isFalse();
        assertThat(env.categories()).isNull();          // 항목 제외
        assertThat(result.finalScore()).isEqualTo(0.800); // pharmacy 만, noise 감점 미포함
    }

    @Test
    @DisplayName("TRANSIT: subway one_is_enough + bus_stop more_is_better, 서울 외에도 전국 적용")
    void transit_subwayAndBusStop_appliedNationwide() {
        var result = calculator.calculate(
                List.of(poi("SUBWAY", 320), poi("BUS_STOP", 200), poi("BUS_STOP", 600)),
                Map.of("subway", 1.0, "bus_stop", 0.9),
                false);   // 서울 외 좌표여도 TRANSIT 은 산출됨(ENV_PENALTY 와 대비)

        ScoreResponse.GroupResult transit = result.breakdown().transit();
        // subway: t=4.0 ≤ 5 → W=1.0, w=1.0 → contribution 1.000
        assertThat(transit.categories().get("subway").base()).isEqualTo(1.0);
        assertThat(transit.categories().get("subway").contribution()).isEqualTo(1.000);
        // bus_stop: W=[1.0, 0.4444] Σ=1.4444, w=0.9 → contribution 1.300
        ScoreResponse.CategoryScore bus = transit.categories().get("bus_stop");
        assertThat(bus.count()).isEqualTo(2);
        assertThat(bus.base()).isCloseTo(1.4444, within(0.0001));
        assertThat(bus.contribution()).isEqualTo(1.300);
        // 그룹 합 + finalScore (서울 외에도 빠지지 않음)
        assertThat(transit.score()).isEqualTo(2.300);
        assertThat(result.finalScore()).isEqualTo(2.300);
    }

    @Test
    @DisplayName("생략된 가중치는 0.0 으로 간주되어 contribution 0")
    void omittedWeight_treatedAsZero() {
        var result = calculator.calculate(
                List.of(poi("BANK", 100)),
                Map.of(),   // 가중치 전부 생략
                false);
        ScoreResponse.CategoryScore bank =
                result.breakdown().essential().categories().get("bank");
        assertThat(bank.weight()).isEqualTo(0.0);
        assertThat(bank.contribution()).isEqualTo(0.0);
        assertThat(result.finalScore()).isEqualTo(0.0);
    }
}

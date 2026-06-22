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
 * 4분류(교통/교육/상업/편의) + <b>그룹 단위 가중치</b>. 순수 계산기라 DB·Spring 없이 검증한다.
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
    @DisplayName("T-7: subway 1개 320m → base 1.000 (one_is_enough, t=4.0≤5→W=1)")
    void transit_oneIsEnough_subway() {
        var result = calculator.calculate(
                List.of(poi("SUBWAY", 320)),
                Map.of("transit", 1.0));

        ScoreResponse.GroupResult transit = result.breakdown().get("transit");
        assertThat(transit.categories().get("subway").base()).isEqualTo(1.0);
        assertThat(transit.score()).isEqualTo(1.000);
        assertThat(result.finalScore()).isEqualTo(1.000);
    }

    @Test
    @DisplayName("T-7: mart 1개 850m → base 0.2214 (more_is_better), commerce w=0.6 → score 0.133")
    void commerce_moreIsBetter_mart_withGroupWeight() {
        var result = calculator.calculate(
                List.of(poi("MART", 850)),
                Map.of("commerce", 0.6));

        ScoreResponse.GroupResult commerce = result.breakdown().get("commerce");
        // t=10.625 > 5 → W = 1/(2.125)² ≈ 0.2214
        assertThat(commerce.categories().get("mart").base()).isCloseTo(0.2214, within(0.0001));
        assertThat(commerce.base()).isCloseTo(0.2214, within(0.0001));
        assertThat(commerce.score()).isEqualTo(0.133);     // 0.2214 × 0.6
        assertThat(result.finalScore()).isEqualTo(0.133);
    }

    @Test
    @DisplayName("T-7: academy 2개[200m,600m] → base 1.4444 (more_is_better ΣW), score 1.444")
    void education_moreIsBetter_academy() {
        var result = calculator.calculate(
                List.of(poi("ACADEMY", 200), poi("ACADEMY", 600)),
                Map.of("education", 1.0));

        ScoreResponse.GroupResult education = result.breakdown().get("education");
        ScoreResponse.CategoryScore academy = education.categories().get("academy");
        assertThat(academy.count()).isEqualTo(2);
        // W=[1.0, 0.4444] → Σ=1.4444
        assertThat(academy.base()).isCloseTo(1.4444, within(0.0001));
        assertThat(education.score()).isEqualTo(1.444);
    }

    @Test
    @DisplayName("경계: t=5.0(=400m) 정확히 → W=1.0 (≤5 분기)")
    void decayBoundary_exactlyFiveMinutes() {
        var result = calculator.calculate(
                List.of(poi("SUBWAY", 400)),
                Map.of("transit", 1.0));
        assertThat(result.breakdown().get("transit").categories().get("subway").base())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("one_is_enough: 같은 카테고리 여러개여도 가장 가까운 1개의 W만 (subway)")
    void oneIsEnough_usesNearestOnly() {
        var result = calculator.calculate(
                List.of(poi("SUBWAY", 320), poi("SUBWAY", 1200)),
                Map.of("transit", 1.0));

        ScoreResponse.CategoryScore subway =
                result.breakdown().get("transit").categories().get("subway");
        assertThat(subway.count()).isEqualTo(2);
        assertThat(subway.nearestMeters()).isEqualTo(320);
        assertThat(subway.base()).isEqualTo(1.0);   // 320m 만 반영, 1200m 무시
    }

    @Test
    @DisplayName("그룹 가중치는 소속 카테고리 base 합 전체에 곱해진다 (convenience: pharmacy+bank)")
    void groupWeight_appliesToGroupSum() {
        // pharmacy 320m(W=1.0) + bank 320m(W=1.0) → convenience base=2.0, w=0.5 → score 1.000
        var result = calculator.calculate(
                List.of(poi("PHARMACY", 320), poi("BANK", 320)),
                Map.of("convenience", 0.5));

        ScoreResponse.GroupResult convenience = result.breakdown().get("convenience");
        assertThat(convenience.base()).isEqualTo(2.0);
        assertThat(convenience.weight()).isEqualTo(0.5);
        assertThat(convenience.score()).isEqualTo(1.000);
        assertThat(result.finalScore()).isEqualTo(1.000);
    }

    @Test
    @DisplayName("전국 적용: breakdown 은 항상 4그룹, 서울 개념/감점 없음")
    void allGroups_nationwide_noEnvPenalty() {
        var result = calculator.calculate(
                List.of(poi("SUBWAY", 320), poi("RESTAURANT", 200), poi("RESTAURANT", 600)),
                Map.of("transit", 1.0, "commerce", 0.9));

        assertThat(result.breakdown().keySet())
                .containsExactlyInAnyOrder("transit", "education", "commerce", "convenience");
        // transit: subway W=1.0 → 1.000
        assertThat(result.breakdown().get("transit").score()).isEqualTo(1.000);
        // commerce: restaurant Σ=1.4444, w=0.9 → 1.300
        assertThat(result.breakdown().get("commerce").categories().get("restaurant").base())
                .isCloseTo(1.4444, within(0.0001));
        assertThat(result.breakdown().get("commerce").score()).isEqualTo(1.300);
        assertThat(result.finalScore()).isEqualTo(2.300);
    }

    @Test
    @DisplayName("생략된 그룹 가중치는 0.0 으로 간주되어 score 0")
    void omittedWeight_treatedAsZero() {
        var result = calculator.calculate(
                List.of(poi("BANK", 100)),
                Map.of());   // 가중치 전부 생략

        ScoreResponse.GroupResult convenience = result.breakdown().get("convenience");
        assertThat(convenience.weight()).isEqualTo(0.0);
        assertThat(convenience.score()).isEqualTo(0.0);
        assertThat(result.finalScore()).isEqualTo(0.0);
    }
}

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
    @DisplayName("T-7: subway 320m → 밴드 평탄대 W=1.0, 그룹 기여 ×40 = 40.0")
    void transit_oneIsEnough_subway() {
        var result = calculator.calculate(
                List.of(poi("SUBWAY", 320)),
                Map.of("transit", 1.0));

        ScoreResponse.GroupResult transit = result.breakdown().get("transit");
        // 320m 은 300~800m 평탄대 → W=1.0 (밴드형, 2026-06-25)
        assertThat(transit.categories().get("subway").base()).isCloseTo(1.0, within(0.0001));
        // 그룹 기여 = base × 지하철 가중 40 → 40.0
        assertThat(transit.score()).isCloseTo(40.0, within(0.01));
        assertThat(result.finalScore()).isCloseTo(40.0, within(0.01));
    }

    @Test
    @DisplayName("지하철 밴드형: 0m W=0.5(소음) / 150m 0.75 / 800m 1.0 / 1200m 0.444")
    void subwayBandDecay() {
        // 역 앞 0m → 소음 감점 W=0.5
        assertThat(calculator.calculate(List.of(poi("SUBWAY", 0)), Map.of("transit", 1.0))
                .breakdown().get("transit").categories().get("subway").base())
                .isCloseTo(0.5, within(0.0001));
        // 150m → 0.5 + 0.5×(150/300) = 0.75
        assertThat(calculator.calculate(List.of(poi("SUBWAY", 150)), Map.of("transit", 1.0))
                .breakdown().get("transit").categories().get("subway").base())
                .isCloseTo(0.75, within(0.0001));
        // 800m → 평탄대 끝 W=1.0
        assertThat(calculator.calculate(List.of(poi("SUBWAY", 800)), Map.of("transit", 1.0))
                .breakdown().get("transit").categories().get("subway").base())
                .isCloseTo(1.0, within(0.0001));
        // 1200m → t=15, 1/(15/10)² = 0.4444
        assertThat(calculator.calculate(List.of(poi("SUBWAY", 1200)), Map.of("transit", 1.0))
                .breakdown().get("transit").categories().get("subway").base())
                .isCloseTo(0.4444, within(0.0001));
    }

    @Test
    @DisplayName("교통: 지하철 ×40 가중 + 버스 ΣW 상한 15 (역세권 우대·버스 인플레 방지)")
    void transit_subwayWeight_busCap() {
        java.util.List<PoiDistanceDTO> pois = new java.util.ArrayList<>();
        pois.add(poi("SUBWAY", 300));                          // W=1.0 → ×40 = 40
        for (int i = 0; i < 20; i++) pois.add(poi("BUS_STOP", 100)); // 20개×W1.0=ΣW 20 → 상한 15
        var result = calculator.calculate(pois, Map.of("transit", 1.0));
        // 그룹 base = 지하철 40 + 버스 min(15, 20) = 55
        assertThat(result.breakdown().get("transit").base()).isCloseTo(55.0, within(0.01));
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
    @DisplayName("경계: 교통 300m·일반 400m 가 각 그룹의 flat 경계(W=1.0)")
    void decayBoundaries_perGroup() {
        var result = calculator.calculate(
                List.of(poi("SUBWAY", 300), poi("MART", 400)),
                Map.of("transit", 1.0, "commerce", 1.0));
        // 지하철 밴드: 300m=평탄대 시작 경계 → W=1.0
        assertThat(result.breakdown().get("transit").categories().get("subway").base())
                .isEqualTo(1.0);
        // 일반(상업) flat 5분(400m): 400m=경계 → W=1.0 (교통 외엔 종전 유지)
        assertThat(result.breakdown().get("commerce").categories().get("mart").base())
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
        assertThat(subway.base()).isCloseTo(1.0, within(0.0001));   // 320m=평탄대 W=1.0 만 반영, 1200m 무시
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
        // transit: subway 320m 평탄대 W=1.0 × 지하철가중 40 = 40.0
        assertThat(result.breakdown().get("transit").score()).isCloseTo(40.0, within(0.01));
        // commerce: restaurant Σ=1.4444(일반 flat 400m·가중 1 유지), w=0.9 → 1.300
        assertThat(result.breakdown().get("commerce").categories().get("restaurant").base())
                .isCloseTo(1.4444, within(0.0001));
        assertThat(result.breakdown().get("commerce").score()).isEqualTo(1.300);
        assertThat(result.finalScore()).isCloseTo(41.300, within(0.01));   // 40.0 + 1.300
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

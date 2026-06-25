package com.example.zipfra.service;

import com.example.zipfra.dto.location.PoiDistanceDTO;
import com.example.zipfra.dto.location.ScoreResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 입지 점수 감쇠·합산 계산기 (AGENTS.md §5, 2026-06-13 4분류 재편).
 * 외부 의존성이 없는 순수 계산 컴포넌트라 DB 없이 단위테스트 가능하다.
 *
 * <pre>
 *   t(분) = 거리(m) / 80                       // 도보 80m/분
 *   W(t)  = 1.0              (t ≤ 5)           // 일반(교육·상업·편의), 버스는 t ≤ 3.75
 *         = 1 / (t/5)²        (t > 5)
 *   지하철은 밴드형: 0~300m W=0.5→1.0 / 300~800m W=1.0 / 800m+ 1/(t/10)²  (subwayDecay)
 *   base[category]  = W(min t)                 // ONE_IS_ENOUGH (가장 가까운 1개)
 *                   = Σ W                       // MORE_IS_BETTER (반경 내 전부)
 *   group_base      = Σ base[category in group]
 *   score[group]    = group_base × weight[group]   // 가중치는 그룹 단위
 *   finalScore      = Σ score[group]                // 전부 양수, 전국 적용
 * </pre>
 */
@Component
public class LocationScoreCalculator {

    private static final double WALK_METERS_PER_MIN = 80.0;
    private static final double DECAY_FLAT_MINUTES = 5.0;        // 일반: t ≤ 5분(400m) → W=1
    // 버스정류장은 감쇠 시작을 300m(3.75분)로 앞당김 — 정류장 변별력(2026-06-25).
    private static final double TRANSIT_FLAT_MINUTES = 3.75;     // 300m / 80m·분⁻¹ (버스)
    // 지하철 밴드형(2026-06-25): 0~300m 소음 감점(W 0.5→1.0 선형), 100~800m 만점, 800m+ 거리 감쇠.
    private static final double SUBWAY_NOISE_METERS = 100.0;     // 소음 감점 구간 상한
    private static final double SUBWAY_PEAK_END_METERS = 800.0;  // 평탄대(만점) 끝 → 이후 감쇠
    private static final double SUBWAY_NEAR_FLOOR = 0.5;         // 역 바로 앞(0m)의 W

    /** 계산 결과 묶음 (breakdown 맵 + finalScore). */
    public record CalcResult(Map<String, ScoreResponse.GroupResult> breakdown, double finalScore) {}

    public CalcResult calculate(List<PoiDistanceDTO> pois, Map<String, Double> weights) {
        Map<Category, List<Double>> distancesByCategory = groupDistances(pois);

        Map<Category.Group, Map<String, ScoreResponse.CategoryScore>> categoriesByGroup =
                new EnumMap<>(Category.Group.class);
        Map<Category.Group, Double> baseByGroup = new EnumMap<>(Category.Group.class);

        for (Category cat : Category.values()) {
            List<Double> distances = distancesByCategory.get(cat);
            double base = base(cat, distances);

            Integer nearestMeters = distances.isEmpty() ? null
                    : (int) Math.round(Collections.min(distances));

            ScoreResponse.CategoryScore categoryScore = new ScoreResponse.CategoryScore(
                    distances.size(),
                    nearestMeters,
                    round(base, 4)
            );

            categoriesByGroup
                    .computeIfAbsent(cat.group(), g -> new LinkedHashMap<>())
                    .put(cat.key(), categoryScore);
            // 그룹 기여 = min(상한, base) × 가중치 (지하철 ×40 / 버스 상한 15, 나머지 그대로). categoryScore 는 raw base 유지.
            double contribution = Math.min(cat.cap(), base) * cat.weight();
            baseByGroup.merge(cat.group(), contribution, Double::sum);
        }

        Map<String, ScoreResponse.GroupResult> breakdown = new LinkedHashMap<>();
        double finalScore = 0.0;
        for (Category.Group group : Category.Group.values()) {
            double groupBase = baseByGroup.getOrDefault(group, 0.0);
            double weight = weightOf(weights, group.key());
            double contribution = groupBase * weight;
            finalScore += contribution;

            breakdown.put(group.key(), new ScoreResponse.GroupResult(
                    round(groupBase, 4),
                    round(weight, 4),
                    round(contribution, 3),
                    categoriesByGroup.getOrDefault(group, Collections.emptyMap())
            ));
        }

        return new CalcResult(breakdown, round(finalScore, 3));
    }

    private Map<Category, List<Double>> groupDistances(List<PoiDistanceDTO> pois) {
        Map<Category, List<Double>> map = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            map.put(c, new ArrayList<>());
        }
        for (PoiDistanceDTO poi : pois) {
            Category.fromDbCategory(poi.getCategory())
                    .ifPresent(c -> map.get(c).add(poi.getDistanceMeters()));
        }
        return map;
    }

    private double base(Category cat, List<Double> distances) {
        if (distances.isEmpty()) {
            return 0.0;
        }
        // 지하철: 밴드형 — 0~300m 소음 감점, 300~800m 만점, 800m+ 감쇠 (ONE_IS_ENOUGH).
        if (cat == Category.SUBWAY) {
            return subwayDecay(Collections.min(distances));
        }
        // 버스는 300m(3.75분) 플랫, 나머지(교육·상업·편의)는 400m(5분) 플랫 후 감쇠.
        double flatMin = (cat.group() == Category.Group.TRANSIT) ? TRANSIT_FLAT_MINUTES : DECAY_FLAT_MINUTES;
        if (cat.model() == Category.Model.ONE_IS_ENOUGH) {
            return decay(Collections.min(distances), flatMin);   // 가장 가까운 1개의 W만
        }
        double sum = 0.0;
        for (double meters : distances) {
            sum += decay(meters, flatMin);
        }
        return sum;
    }

    /**
     * 지하철 밴드형 감쇠(2026-06-25). 역 바로 앞은 소음으로 감점, 적당히 떨어진 곳이 최적.
     * <pre>
     *   0~300m : W = 0.5 → 1.0 선형 (역 앞 소음 감점)
     *   300~800m: W = 1.0           (역세권 최적 평탄대)
     *   800m+   : W = 1 / (t/10)²    (t=m/80, 거리 감쇠)
     * </pre>
     */
    private double subwayDecay(double meters) {
        if (meters < SUBWAY_NOISE_METERS) {
            return SUBWAY_NEAR_FLOOR + (1.0 - SUBWAY_NEAR_FLOOR) * (meters / SUBWAY_NOISE_METERS);
        }
        return decay(meters, SUBWAY_PEAK_END_METERS / WALK_METERS_PER_MIN);   // 플랫=10분(800m)
    }

    private double decay(double meters, double flatMinutes) {
        double t = meters / WALK_METERS_PER_MIN;
        if (t <= flatMinutes) {
            return 1.0;
        }
        double ratio = t / flatMinutes;
        return 1.0 / (ratio * ratio);
    }

    private double weightOf(Map<String, Double> weights, String groupKey) {
        Double w = weights.get(groupKey);
        return w == null ? 0.0 : w;   // 생략된 그룹은 0.0
    }

    private static double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}

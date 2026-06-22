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
 *   W(t)  = 1.0              (t ≤ 5)
 *         = 1 / (t/5)²        (t > 5)
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
    private static final double DECAY_FLAT_MINUTES = 5.0;   // t ≤ 5 → W=1

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
            baseByGroup.merge(cat.group(), base, Double::sum);
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
        if (cat.model() == Category.Model.ONE_IS_ENOUGH) {
            return decay(Collections.min(distances));   // 가장 가까운 1개의 W만
        }
        double sum = 0.0;
        for (double meters : distances) {
            sum += decay(meters);
        }
        return sum;
    }

    private double decay(double meters) {
        double t = meters / WALK_METERS_PER_MIN;
        if (t <= DECAY_FLAT_MINUTES) {
            return 1.0;
        }
        double ratio = t / DECAY_FLAT_MINUTES;
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

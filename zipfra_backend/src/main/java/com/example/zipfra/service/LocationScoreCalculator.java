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
 * 입지 점수 감쇠·합산 계산기 (AGENTS.md §5).
 * 외부 의존성이 없는 순수 계산 컴포넌트라 DB 없이 단위테스트 가능하다.
 *
 * <pre>
 *   t(분) = 거리(m) / 80           // 도보 80m/분
 *   W(t)  = 1.0           (t ≤ 5)
 *         = 1 / (t/5)²     (t > 5)
 *   base  = W(min t)               // ONE_IS_ENOUGH (가장 가까운 1개)
 *         = Σ W            // MORE_IS_BETTER / PENALTY
 *   contribution = base × weight   // PENALTY 는 음수
 * </pre>
 */
@Component
public class LocationScoreCalculator {

    private static final double WALK_METERS_PER_MIN = 80.0;
    private static final double DECAY_FLAT_MINUTES = 5.0;   // t ≤ 5 → W=1

    /** 계산 결과 묶음 (breakdown + finalScore). */
    public record CalcResult(ScoreResponse.Breakdown breakdown, double finalScore) {}

    public CalcResult calculate(List<PoiDistanceDTO> pois, Map<String, Double> weights, boolean inSeoul) {
        Map<Category, List<Double>> distancesByCategory = groupDistances(pois);

        Map<Category.Group, Map<String, ScoreResponse.CategoryScore>> categoriesByGroup =
                new EnumMap<>(Category.Group.class);
        Map<Category.Group, Double> scoreByGroup = new EnumMap<>(Category.Group.class);

        for (Category cat : Category.values()) {
            // ENV_PENALTY: 서울 외 좌표는 항목 자체 제외 (0점 처리 금지)
            if (cat.group() == Category.Group.ENV_PENALTY && !inSeoul) {
                continue;
            }

            List<Double> distances = distancesByCategory.get(cat);
            double weight = weightOf(weights, cat.key());
            double base = base(cat, distances);
            double contribution = base * weight;
            if (cat.model() == Category.Model.PENALTY) {
                contribution = -contribution;
            }

            Integer nearestMeters = distances.isEmpty() ? null
                    : (int) Math.round(Collections.min(distances));

            ScoreResponse.CategoryScore categoryScore = new ScoreResponse.CategoryScore(
                    distances.size(),
                    nearestMeters,
                    round(base, 4),
                    round(weight, 4),
                    round(contribution, 3)
            );

            categoriesByGroup
                    .computeIfAbsent(cat.group(), g -> new LinkedHashMap<>())
                    .put(cat.key(), categoryScore);
            scoreByGroup.merge(cat.group(), contribution, Double::sum);
        }

        ScoreResponse.GroupResult essential =
                groupResult(Category.Group.ESSENTIAL, categoriesByGroup, scoreByGroup, null);
        ScoreResponse.GroupResult leisure =
                groupResult(Category.Group.LEISURE, categoriesByGroup, scoreByGroup, null);
        ScoreResponse.GroupResult envPenalty = inSeoul
                ? groupResult(Category.Group.ENV_PENALTY, categoriesByGroup, scoreByGroup, Boolean.TRUE)
                : new ScoreResponse.GroupResult(Boolean.FALSE, null, null);

        double finalScore = round(
                scoreByGroup.getOrDefault(Category.Group.ESSENTIAL, 0.0)
                        + scoreByGroup.getOrDefault(Category.Group.LEISURE, 0.0)
                        + (inSeoul ? scoreByGroup.getOrDefault(Category.Group.ENV_PENALTY, 0.0) : 0.0),
                3);

        return new CalcResult(new ScoreResponse.Breakdown(essential, leisure, envPenalty), finalScore);
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

    private ScoreResponse.GroupResult groupResult(
            Category.Group group,
            Map<Category.Group, Map<String, ScoreResponse.CategoryScore>> categoriesByGroup,
            Map<Category.Group, Double> scoreByGroup,
            Boolean applicable) {

        Map<String, ScoreResponse.CategoryScore> categories =
                categoriesByGroup.getOrDefault(group, Collections.emptyMap());
        double score = round(scoreByGroup.getOrDefault(group, 0.0), 3);
        return new ScoreResponse.GroupResult(applicable, score, categories);
    }

    private double weightOf(Map<String, Double> weights, String key) {
        Double w = weights.get(key);
        return w == null ? 0.0 : w;   // 생략된 카테고리는 0.0
    }

    private static double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}

package com.example.zipfra.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.example.zipfra.dto.location.PoiDistanceDTO;
import com.example.zipfra.dto.location.ScoreResponse;
import com.example.zipfra.dto.score.PropertyGeom;
import com.example.zipfra.mapper.postgis.PoiMapper;
import com.example.zipfra.mapper.postgis.PropertyScoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매물별 점수 사전계산 배치 (AGENTS.md §5.1).
 * 기동 시 {@code property_score} 가 비어 있으면 전체 매물에 대해 §5 그룹 base 를 산출·적재한다.
 * 무거운 POI 반경쿼리·감쇠합산은 여기서 1회, 가중치 곱셈은 프론트 실시간(#6)으로 분리한다.
 * 정식 심야 배치(§9)는 후속.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PropertyScoreBatch implements ApplicationRunner {

    /** §5.1 기본 반경(§5: 기본 1,500m). */
    private static final int RADIUS_METERS = 1500;

    private final PropertyScoreMapper propertyScoreMapper;
    private final PoiMapper poiMapper;
    private final LocationScoreCalculator calculator;

    @Override
    @Transactional(transactionManager = "spatialTransactionManager")
    public void run(ApplicationArguments args) {
        if (propertyScoreMapper.count() > 0) {
            log.info("[property-score] 이미 적재됨 — 배치 skip");
            return;
        }

        List<PropertyGeom> properties = propertyScoreMapper.findAllPropertiesForScoring();
        for (PropertyGeom p : properties) {
            List<PoiDistanceDTO> pois = poiMapper.findWithin(p.getLon(), p.getLat(), RADIUS_METERS);
            // 가중치는 미적용(빈 맵) — base 만 추출. 가중치 곱셈은 프론트 실시간(§5.1).
            Map<String, ScoreResponse.GroupResult> breakdown =
                    calculator.calculate(pois, Collections.emptyMap()).breakdown();

            propertyScoreMapper.upsert(
                    p.getId(),
                    groupBase(breakdown, "transit"),
                    groupBase(breakdown, "education"),
                    groupBase(breakdown, "commerce"),
                    groupBase(breakdown, "convenience"));
        }
        log.info("[property-score] 배치 완료: {} 건 적재", properties.size());
    }

    private double groupBase(Map<String, ScoreResponse.GroupResult> breakdown, String groupKey) {
        ScoreResponse.GroupResult g = breakdown.get(groupKey);
        return g == null ? 0.0 : g.base();
    }
}

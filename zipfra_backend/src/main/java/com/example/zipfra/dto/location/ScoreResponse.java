package com.example.zipfra.dto.location;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * LOC-01 입지 점수 응답 (AGENTS.md §5·§8.1, 2026-06-13 4분류 재편).
 *
 * <p>breakdown 은 그룹 키(`transit`/`education`/`commerce`/`convenience`) → {@link GroupResult} 맵.
 * 가중치는 <b>그룹 단위</b>로 적용되며(카테고리별 아님), 전부 양수다(감점 그룹 없음).
 * finalScore = Σ(그룹 score), 전국 적용(서울 제한 없음).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScoreResponse(
        double lon,
        double lat,
        int radiusMeters,
        double finalScore,
        Map<String, GroupResult> breakdown
) {

    /** 그룹 결과: base=소속 카테고리 base 합, score=base×weight(그룹 기여). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GroupResult(
            double base,
            double weight,
            double score,
            Map<String, CategoryScore> categories
    ) {}

    /** 카테고리 상세: 감쇠 base + 개수/최근접 거리. 가중치는 그룹 레벨이라 여기엔 없다. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CategoryScore(
            int count,
            Integer nearestMeters,                 // 가장 가까운 POI 거리(m), 없으면 null
            double base
    ) {}
}

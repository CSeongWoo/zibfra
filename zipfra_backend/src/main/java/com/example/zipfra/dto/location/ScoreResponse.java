package com.example.zipfra.dto.location;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * LOC-01 입지 점수 응답 (AGENTS.md §5·§8.1).
 *
 * <p>breakdown 키는 essential/leisure/transit/env_penalty(snake_case 고정).
 * transit 은 전국 적용(서울 제한 없음). env_penalty 는 서울(법정동 11*) 좌표일 때만
 * 산출되며, 서울 외에서는 applicable=false 로 표기하고 finalScore 합산에서 제외한다(0점 처리 금지).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScoreResponse(
        double lon,
        double lat,
        int radiusMeters,
        double finalScore,
        Breakdown breakdown
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Breakdown(
            GroupResult essential,
            GroupResult leisure,
            GroupResult transit,
            @JsonProperty("env_penalty") GroupResult envPenalty
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GroupResult(
            Boolean applicable,                    // env_penalty 전용(서울 외=false). 그 외 null→생략
            Double score,
            Map<String, CategoryScore> categories
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CategoryScore(
            int count,
            Integer nearestMeters,                 // 가장 가까운 POI 거리(m), 없으면 null
            double base,
            double weight,
            double contribution
    ) {}
}

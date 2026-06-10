package com.example.zipfra.dto.location;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;

/**
 * LOC-01 입지 점수 요청 (AGENTS.md §8.1).
 * GET+Body 금지 → POST JSON body. 범위 검증은 서비스 계층에서 수행한다.
 */
@Getter
@Setter
public class ScoreRequest {

    /** 경도 [-180, 180] (필수) */
    private Double lon;

    /** 위도 [-90, 90] (필수) */
    private Double lat;

    /** 반경(m) [100, 3000], 생략 시 1500 */
    private Integer radiusMeters;

    /** 카테고리별 가중치 [0.0, 1.0]. 생략된 카테고리는 0.0 으로 간주. */
    private Map<String, Double> weights;

    public Map<String, Double> weightsOrEmpty() {
        return weights == null ? Collections.emptyMap() : weights;
    }
}

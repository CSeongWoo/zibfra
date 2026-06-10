package com.example.zipfra.service;

import com.example.zipfra.dto.location.ScoreRequest;
import com.example.zipfra.dto.location.ScoreResponse;

/**
 * LOC-01 입지 점수 산출 서비스 (AGENTS.md §5·§8.1).
 *
 * <p>좌표·반경·가중치를 검증하고, PostGIS 에서 반경 내 POI 를 추출한 뒤
 * 거리 감쇠·카테고리 합산(메모리)으로 입지 점수를 산출한다.
 * 팀 컨벤션 §5 에 따라 인터페이스/구현을 분리한다.
 */
public interface LocationScoreService {

    /**
     * 입지 점수를 산출한다.
     *
     * @param request lon/lat(필수), radiusMeters([100,3000] 생략=1500), weights([0,1] 생략=0.0)
     * @return 카테고리별 breakdown + finalScore
     * @throws com.example.zipfra.exception.ApiException COORD/RADIUS/WEIGHT 범위 위반 시
     */
    ScoreResponse score(ScoreRequest request);
}

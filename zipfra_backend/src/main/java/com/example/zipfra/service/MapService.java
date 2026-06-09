package com.example.zipfra.service;

import com.example.zipfra.dto.map.MarkerResponse;

/**
 * MAP-01 지도 마커 조회 기능 명세 (팀 규칙 #5: 구현 전 인터페이스로 세부 명세).
 *
 * <p>서버 권위 줌 판정(§7.1)과 거대 bbox 가드(§7.2)를 적용해 DETAIL/SUMMARY 전략을 결정한다.</p>
 *
 * <h3>판정 규칙</h3>
 * <ul>
 *   <li>zoom &lt; 1 또는 &gt; 21 → {@code ZOOM_OUT_OF_RANGE}</li>
 *   <li>zoom ≤ 14 → SUMMARY, zoom ≥ 15 → DETAIL (임계 IN=15, OUT=14)</li>
 *   <li>대각 &gt; 150km + DETAIL → {@code BBOX_TOO_LARGE_FOR_DETAIL}</li>
 *   <li>대각 &gt; 150km + SUMMARY → 강제 SUMMARY 유지({@code bboxOversized=true})</li>
 *   <li>size &gt; 200 → {@code PAGE_SIZE_EXCEEDED} (def size=100, page=0)</li>
 * </ul>
 */
public interface MapService {

    /**
     * @param bbox  "minLng,minLat,maxLng,maxLat" (EPSG:4326)
     * @param zoom  클라이언트 줌(서버가 임계값으로 재판정)
     * @param page  0-based 페이지(생략 시 0, DETAIL 에서만 의미)
     * @param size  페이지 크기(생략 시 100, 최대 200, DETAIL 에서만 의미)
     * @return DETAIL 또는 SUMMARY 응답
     * @throws com.example.zipfra.web.ApiException 검증 실패 시(위 판정 규칙)
     */
    MarkerResponse getMarkers(String bbox, int zoom, Integer page, Integer size);
}

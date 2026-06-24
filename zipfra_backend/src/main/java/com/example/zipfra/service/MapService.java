package com.example.zipfra.service;

import java.util.List;

import com.example.zipfra.dto.map.MarkerFilter;
import com.example.zipfra.dto.map.MarkerResponse;
import com.example.zipfra.dto.map.PoiMarkerDTO;
import com.example.zipfra.dto.map.PriceTrendPoint;
import com.example.zipfra.dto.map.PropertySearchDTO;

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
     * @param filter 검색 필터(§8.1.1, DETAIL 에서만 적용; SUMMARY 는 무시). null 허용
     * @return DETAIL 또는 SUMMARY 응답
     * @throws com.example.zipfra.exception.ApiException 검증 실패 시(위 판정 규칙 + 필터 유형 불량 시 INVALID_PARAM)
     */
    MarkerResponse getMarkers(String bbox, int zoom, Integer page, Integer size, MarkerFilter filter);

    /**
     * MAP-02 POI 오버레이 조회 (인프라 표시 토글, §8.1). 로컬 {@code poi} 테이블만 조회(외부 API 아님).
     *
     * @param bbox   "minLng,minLat,maxLng,maxLat" (EPSG:4326)
     * @param groups 그룹 CSV(transit/education/commerce/convenience). null·빈값 → 빈 목록
     * @return bbox 내 선택 그룹 POI 목록(없으면 빈 목록)
     */
    List<PoiMarkerDTO> getPois(String bbox, String groups);

    /**
     * MAP-03 단지 검색 (§8.1). 우리 {@code real_estate_sales} 의 단지명 부분일치 검색.
     * 지역(시·구·동) 검색은 프론트 카카오 addressSearch 가 담당 — 본 API 는 단지만 반환.
     *
     * @param q     검색어. 2자 미만(공백 제외)이면 빈 목록(seq-scan 낭비·과다결과 방지)
     * @param limit 결과 상한(생략 시 20, 최대 50 초과 시 {@code INVALID_PARAM})
     * @return 단지 목록(dealCount 내림차순), 없으면 빈 목록
     */
    List<PropertySearchDTO> searchProperties(String q, Integer limit);

    /** 실거래가 추이: 매물 id 의 단지·거래유형 월별 평균가 시계열(없으면 빈 목록). */
    List<PriceTrendPoint> getPriceTrend(long propertyId);
}

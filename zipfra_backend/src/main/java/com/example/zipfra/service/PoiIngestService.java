package com.example.zipfra.service;

/**
 * POI 실데이터 적재 (§9). 카카오 로컬 카테고리 검색으로 bbox 내 POI 를 수집해 PostGIS {@code poi} 적재.
 * 더미 POI 를 실데이터로 교체 — MAP-02 오버레이 + 입지 점수(POI 반경)의 정확도 확보.
 */
public interface PoiIngestService {

    /**
     * @param bbox "minLng,minLat,maxLng,maxLat" (EPSG:4326)
     * @return 적재된 POI 건수(중복 제외)
     */
    int ingestPois(String bbox);

    /**
     * 전국 POI 적재(§9). 빈 격자 낭비를 피해 <b>매물이 존재하는 0.02도 격자만</b> 순회하며 카카오 카테고리 검색.
     * {@code clearPoi()} 1회 후 격자×카테고리 적재(전역 중복제거). 점수(POI 반경) 전국 정확도 확보용.
     * @return 적재된 POI 건수(중복 제외)
     */
    int ingestPoisNationwide();
}

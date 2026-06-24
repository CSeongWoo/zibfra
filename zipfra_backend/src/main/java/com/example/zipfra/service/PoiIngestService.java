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

    /**
     * 버스정류장 공공데이터 적재(§9, A 방식 — CSV 번들). 카카오 카테고리 그룹 코드에 버스정류장이 없어
     * (지하철 SW8 만 존재) 별도 소스가 필요하다. {@code resources/data/bus_stops.csv}
     * (국토부 전국 버스정류장 위치정보, data.go.kr 15067528)를 읽어 {@code poi(BUS_STOP)} 로 적재한다.
     *
     * <p>{@code clearPoi()} 를 호출하지 않고 {@code deletePoiByCategory("BUS_STOP")} 후 insert(멱등) —
     * 카카오 POI 를 보존하므로 카카오 전국 적재 <b>이후</b> 실행한다. 적재 후 {@code recompute-scores} 로
     * {@code property_score.transit_base} 갱신 → 교통 점수가 지하철+버스로 완성된다.
     *
     * @return 적재된 버스정류장 건수
     */
    int ingestBusStops();
}

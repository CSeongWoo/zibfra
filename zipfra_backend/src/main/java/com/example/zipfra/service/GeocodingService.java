package com.example.zipfra.service;

import java.util.Optional;

/**
 * 지번/도로명 주소 → 좌표 지오코딩 (카카오 로컬 REST, §9 실거래가 적재용).
 * 국토부 실거래가는 좌표를 주지 않으므로 적재 단계에서 주소를 좌표로 변환한다.
 */
public interface GeocodingService {

    /**
     * @param address 검색 주소(예: "서울 강남구 역삼동 736")
     * @return {@code [lng, lat]} (EPSG:4326). 결과 없거나 실패 시 empty.
     */
    Optional<double[]> geocode(String address);
}

package com.example.zipfra.dto.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MAP-02 POI 오버레이 마커 (인프라 표시 토글, §8.1).
 * PostGIS {@code poi} 행을 MyBatis 가 매핑(ST_X→lng, ST_Y→lat, §4 좌표 매핑 표준).
 * {@code category}/{@code group} 은 서비스가 소문자 key 로 정규화한다(프론트 색칠용).
 */
@Getter
@Setter
@NoArgsConstructor
public class PoiMarkerDTO {

    private double lat;
    private double lng;
    private String category; // 소문자 key (subway, cafe …)
    private String group;    // 그룹 key (transit/education/commerce/convenience)
    private String name;
}

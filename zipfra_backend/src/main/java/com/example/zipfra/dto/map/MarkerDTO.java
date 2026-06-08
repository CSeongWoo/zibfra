package com.example.zipfra.dto.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DETAIL 개별 매물 마커 read-model (MAP-01).
 * PostGIS {@code real_estate_sales} 행을 MyBatis 가 매핑한다
 * (ST_X(geom) AS lng, ST_Y(geom) AS lat — §4 좌표 매핑 표준).
 *
 * MyBatis 매핑을 위해 no-arg 생성자 + setter 를 둔다(read-model 예외).
 */
@Getter
@Setter
@NoArgsConstructor
public class MarkerDTO {

    private Long id;
    private double lat;
    private double lng;
    private String buildingName;
    private Long dealAmount;       // 거래금액(만원)
    private Double exclusiveArea;  // 전용면적(㎡)
    private Integer floorNo;
}

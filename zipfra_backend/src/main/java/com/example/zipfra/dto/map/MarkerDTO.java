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
    private String dealType;       // SALE | JEONSE | WOLSE
    private String propertyType;   // APT | OFFICETEL | ROW_HOUSE
    private Long dealAmount;       // 매매가(만원), 전월세는 null
    private Long deposit;          // 보증금(만원), 전월세
    private Integer monthlyRent;   // 월세(만원), 월세만
    private Double exclusiveArea;  // 전용면적(㎡)
    private Integer floorNo;
    private Integer buildYear;     // 건축년도
    // 매물별 점수 4그룹 base (§5.1, property_score LEFT JOIN; 미계산 매물 0). 가중치 미적용.
    private Double transitBase;
    private Double educationBase;
    private Double commerceBase;
    private Double convenienceBase;
}

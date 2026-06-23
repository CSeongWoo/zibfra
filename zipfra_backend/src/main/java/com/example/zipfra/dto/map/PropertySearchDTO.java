package com.example.zipfra.dto.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MAP-03 단지 검색 결과 read-model (AGENTS.md §8.1).
 * {@code real_estate_sales} 를 단지(building_name + lawd_cd) 단위로 묶은 집계 행을 매핑한다:
 * 대표 좌표는 단지 거래들의 {@code ST_Centroid}, 면적은 범위(min/max), {@code dealCount} 는 거래 건수.
 *
 * <p>MyBatis 매핑을 위해 no-arg 생성자 + setter 를 둔다(read-model 예외).
 */
@Getter
@Setter
@NoArgsConstructor
public class PropertySearchDTO {

    private long id;             // 단지 대표 매물 id(MIN(id)) — 클릭 시 마커 식별용
    private String buildingName;
    private double lat;
    private double lng;
    private String propertyType; // 대표 유형(APT | OFFICETEL | ROW_HOUSE)
    private int dealCount;       // 단지 내 거래 건수
    private Double minArea;      // 전용면적 최소(㎡)
    private Double maxArea;      // 전용면적 최대(㎡)
    private Integer buildYear;   // 준공년
}

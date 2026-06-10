package com.example.zipfra.dto.location;

import lombok.Getter;
import lombok.Setter;

/**
 * PostGIS poi 반경 조회 결과 (read-model).
 * MyBatis resultMap 자동 매핑(map-underscore-to-camel-case) 대상이라 기본 생성자 + setter 보유.
 */
@Getter
@Setter
public class PoiDistanceDTO {

    /** poi.category 원본(PHARMACY|MART|BANK|RESTAURANT|CAFE|CINEMA|NOISE|WASTE) */
    private String category;

    /** ST_Distance(::geography) 결과(미터) */
    private double distanceMeters;
}

package com.example.zipfra.dto.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SUMMARY 시·군·구 사전 집계 read-model (MAP-01, 줌아웃 ≤ 14).
 * PostGIS {@code region_summary} 행을 MyBatis 가 매핑한다
 * (center_lat AS lat, center_lon AS lng — 사전 집계 좌표 직접 사용).
 *
 * MyBatis 매핑을 위해 no-arg 생성자 + setter 를 둔다(read-model 예외).
 */
@Getter
@Setter
@NoArgsConstructor
public class RegionSummaryDTO {

    private String regionCd;   // 시군구코드(5)
    private String regionName;
    private double lat;
    private double lng;
    private int dealCount;
    private Long avgAmount;     // 평균 거래금액(만원)
    private Long maxAmount;     // 최고 거래금액(만원)
}

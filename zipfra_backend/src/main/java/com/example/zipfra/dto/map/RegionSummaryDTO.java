package com.example.zipfra.dto.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SUMMARY 시·군·구 집계 read-model (MAP-01, 줌아웃 ≤ 14).
 * 이름·중심좌표·평균/최고가는 {@code region_summary} 에서, {@code propertyCount} 는
 * bbox+필터에 매칭되는 {@code real_estate_sales} 행 수를 실시간 COUNT 한다(기능5, §8.1.1).
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
    private int propertyCount; // bbox+필터 매칭 매물 갯수(거래 행 수, 실시간 COUNT)
    private Long avgAmount;     // 평균 거래금액(만원)
    private Long maxAmount;     // 최고 거래금액(만원)
}

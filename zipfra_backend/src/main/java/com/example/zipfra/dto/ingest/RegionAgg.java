package com.example.zipfra.dto.ingest;

import lombok.Getter;
import lombok.Setter;

/**
 * region_summary 실집계 결과 1행 (§7, 줌아웃 SUMMARY). real_estate_sales 를 시군구(lawd_cd)별 집계.
 */
@Getter
@Setter
public class RegionAgg {

    private String lawdCd;      // 시군구 5자리
    private double centerLon;   // 매물 평균 경도
    private double centerLat;   // 매물 평균 위도
    private int dealCount;      // 거래 건수
    private Long avgAmount;     // 평균 매매가(만원)
    private Long maxAmount;     // 최고 매매가(만원)
}

package com.example.zipfra.dto.ingest;

import lombok.Builder;
import lombok.Getter;

/**
 * 실거래가 적재 1행 (§9). 국토부 API item + 카카오 지오코딩 좌표를 합친 insert 모델.
 * 종류·거래유형 일반화: dealType(SALE/JEONSE/WOLSE) · propertyType(APT/OFFICETEL/ROW_HOUSE).
 * 매매=dealAmount, 전월세=deposit/monthlyRent(매매 시 null).
 */
@Getter
@Builder
public class RealEstateRow {

    private String buildingName;  // aptNm | offiNm | mhouseNm
    private String dealType;      // SALE | JEONSE | WOLSE
    private String propertyType;  // APT | OFFICETEL | ROW_HOUSE
    private Long dealAmount;      // 매매가(만원, 콤마 제거). 전월세는 null
    private Long deposit;         // 보증금(만원). 매매는 null
    private Integer monthlyRent;  // 월세(만원). 매매·전세는 0/null
    private Double exclusiveArea; // excluUseAr
    private Integer floorNo;      // floor
    private Integer buildYear;    // buildYear
    private String dealYm;        // YYYYMM
    private String lawdCd;        // sggCd 5자리
    private String jibun;
    private double lng;           // 지오코딩 결과
    private double lat;
}

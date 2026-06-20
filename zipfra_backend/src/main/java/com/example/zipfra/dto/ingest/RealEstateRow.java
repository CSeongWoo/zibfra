package com.example.zipfra.dto.ingest;

import lombok.Builder;
import lombok.Getter;

/**
 * 실거래가 적재 1행 (§9). 국토부 API item + 카카오 지오코딩 좌표를 합친 insert 모델.
 * deal_type=SALE, property_type=APT 는 매퍼에서 고정(아파트 매매 적재).
 */
@Getter
@Builder
public class RealEstateRow {

    private String buildingName;  // aptNm
    private Long dealAmount;      // 만원 (콤마 제거)
    private Double exclusiveArea; // excluUseAr
    private Integer floorNo;      // floor
    private Integer buildYear;    // buildYear
    private String dealYm;        // YYYYMM
    private String lawdCd;        // sggCd 5자리
    private String jibun;
    private double lng;           // 지오코딩 결과
    private double lat;
}

package com.example.zipfra.dto.ai;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * AI-01 getPropertyDetail Tool 반환 Resource DTO (AGENTS.md §6.4.1).
 *
 * <p>PostGIS real_estate_sales 조회 결과.
 * 좌표는 ST_X/ST_Y로 double 분리(§4 좌표 매핑 표준).
 * MyBatis map-underscore-to-camel-case=true 자동 매핑.
 */
@Getter
@Setter
@NoArgsConstructor
public class PropertyDetailDTO {

    /** real_estate_sales.id */
    private Long id;

    /** 건물명 (apt_name 등) */
    private String buildingName;

    /** 거래금액(만원). 전월세 매물은 null 가능 (§8.1.1). */
    private Long dealAmount;

    /** 보증금(만원). 매매 매물은 null. */
    private Long deposit;

    /** 월세(만원). WOLSE 만 유효. */
    private Long monthlyRent;

    /** 전용면적(㎡). */
    private Double area;

    /** 층. */
    private Integer floor;

    /** 법정동명 (지역 레이블). */
    private String beopjungdong;

    /** 거래유형: SALE | JEONSE | WOLSE */
    private String dealType;

    /** 매물유형: APT | OFFICETEL | ROW_HOUSE */
    private String propertyType;

    /** 거래연월 (YYYYMM 형식 또는 LocalDate). */
    private String dealYm;

    /** 건축연도. */
    private Integer buildYear;

    /** 경도 (ST_X(location)). */
    private Double lon;

    /** 위도 (ST_Y(location)). */
    private Double lat;

    /** LLM 프롬프트 컨텍스트용 텍스트 요약. */
    public String toPromptContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("건물명: ").append(buildingName != null ? buildingName : "미상").append("\n");
        sb.append("법정동: ").append(beopjungdong != null ? beopjungdong : "미상").append("\n");
        sb.append("매물유형: ").append(propertyType != null ? propertyType : "미상").append("\n");
        sb.append("거래유형: ").append(dealType != null ? dealType : "미상").append("\n");
        if (dealAmount != null) {
            sb.append("거래금액: ").append(dealAmount).append("만원\n");
        }
        if (deposit != null) {
            sb.append("보증금: ").append(deposit).append("만원\n");
        }
        if (monthlyRent != null && monthlyRent > 0) {
            sb.append("월세: ").append(monthlyRent).append("만원\n");
        }
        sb.append("전용면적: ").append(area != null ? area + "㎡" : "미상").append("\n");
        sb.append("층: ").append(floor != null ? floor + "층" : "미상").append("\n");
        sb.append("거래연월: ").append(dealYm != null ? dealYm : "미상").append("\n");
        if (buildYear != null) {
            sb.append("건축연도: ").append(buildYear).append("년\n");
        }
        return sb.toString();
    }
}

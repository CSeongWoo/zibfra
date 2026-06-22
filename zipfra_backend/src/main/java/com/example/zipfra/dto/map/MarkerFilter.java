package com.example.zipfra.dto.map;

import lombok.Getter;

/**
 * MAP-01 DETAIL 검색 필터 (AGENTS.md §8.1.1).
 * 전부 optional·AND 결합이며, MyBatis 동적 WHERE(&lt;if&gt;)로 생략 가능하다.
 * 값 유효성(거래/매물 유형 enum)은 {@code MapServiceImpl} 에서 검증한다.
 *
 * <p>가격 필터 대상 컬럼은 {@code dealType} 별로 분기한다(§8.1.1):
 * {@code SALE}→{@code deal_amount}, {@code JEONSE}/{@code WOLSE}→{@code deposit},
 * {@code dealType} 미지정→{@code deal_amount}(매매 기본).
 */
@Getter
public class MarkerFilter {

    private final String dealType;       // SALE | JEONSE | WOLSE (생략 시 전체)
    private final String propertyType;   // APT | OFFICETEL | ROW_HOUSE (생략 시 전체)
    private final Integer priceMin;      // 만원
    private final Integer priceMax;      // 만원

    public MarkerFilter(String dealType, String propertyType, Integer priceMin, Integer priceMax) {
        this.dealType = dealType;
        this.propertyType = propertyType;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
    }
}

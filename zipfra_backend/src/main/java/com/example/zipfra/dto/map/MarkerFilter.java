package com.example.zipfra.dto.map;

import java.util.List;

import lombok.Getter;

/**
 * MAP-01 DETAIL 검색 필터 (AGENTS.md §8.1).
 * 거래/매물 유형은 <b>다중선택</b>(콤마 구분 → SQL {@code IN}). 전부 optional·AND 결합이며,
 * MyBatis 동적 WHERE(&lt;if&gt;/&lt;foreach&gt;)로 빈 리스트는 조건 생략한다.
 * 값 유효성(거래/매물 유형 enum)은 {@code MapServiceImpl} 에서 원소별 검증한다.
 *
 * <p>가격 필터 대상 컬럼은 <b>행별</b>로 분기한다: 매매행→{@code deal_amount}, 전월세행→{@code deposit}.
 * {@code real_estate_sales} 는 둘이 상호배타(SALE=deal_amount/deposit null, 전월세=deposit/deal_amount null)
 * 이므로 {@code COALESCE(deal_amount, deposit)} 가 행의 대표가가 된다 — 거래유형을 혼합 선택해도
 * 각 행이 자기 컬럼 기준으로 걸러진다.
 */
@Getter
public class MarkerFilter {

    private final List<String> dealTypes;       // SALE | JEONSE | WOLSE (null·빈 = 전체)
    private final List<String> propertyTypes;   // APT | OFFICETEL | ROW_HOUSE (null·빈 = 전체)
    private final Integer priceMin;             // 만원
    private final Integer priceMax;             // 만원

    public MarkerFilter(List<String> dealTypes, List<String> propertyTypes, Integer priceMin, Integer priceMax) {
        this.dealTypes = dealTypes;
        this.propertyTypes = propertyTypes;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
    }
}

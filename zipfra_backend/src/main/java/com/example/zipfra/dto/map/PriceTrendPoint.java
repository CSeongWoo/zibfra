package com.example.zipfra.dto.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 실거래가 추이 차트 한 점(월별). {@code building_price_history} 한 행을 매핑.
 * dealYm=YYYYMM, avgAmount=그 달 대표가 평균(만원), dealCount=거래 건수.
 */
@Getter
@Setter
@NoArgsConstructor
public class PriceTrendPoint {
    private String dealYm;
    private long avgAmount;
    private int dealCount;
}

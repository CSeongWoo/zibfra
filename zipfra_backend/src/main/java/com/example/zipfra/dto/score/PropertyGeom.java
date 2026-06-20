package com.example.zipfra.dto.score;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 매물별 점수 배치(§5.1) 입력 — real_estate_sales 의 좌표 read-model.
 * ST_X(geom) AS lon / ST_Y(geom) AS lat 로 매핑.
 */
@Getter
@Setter
@NoArgsConstructor
public class PropertyGeom {
    private long id;
    private double lon;
    private double lat;
}

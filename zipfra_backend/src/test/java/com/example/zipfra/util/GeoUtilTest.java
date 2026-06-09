package com.example.zipfra.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.zipfra.dto.map.Bbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GeoUtilTest {

    @Test
    @DisplayName("haversine: 위도 37.5에서 경도 0.009도 ≈ 790m")
    void haversine_eastWest() {
        double meters = GeoUtil.haversineMeters(37.5, 127.0, 37.5, 127.009);
        assertThat(meters).isCloseTo(790.0, org.assertj.core.data.Offset.offset(30.0));
    }

    @Test
    @DisplayName("거대 bbox 가드: 작은 bbox 대각은 150km 미만")
    void diagonal_smallBbox_underThreshold() {
        Bbox bbox = new Bbox(126.9, 37.4, 127.1, 37.6);
        assertThat(bbox.diagonalMeters()).isLessThan(150_000.0);
    }

    @Test
    @DisplayName("거대 bbox 가드: 넓은 bbox 대각은 150km 초과")
    void diagonal_hugeBbox_overThreshold() {
        Bbox bbox = new Bbox(120.0, 33.0, 130.0, 39.0);
        assertThat(bbox.diagonalMeters()).isGreaterThan(150_000.0);
    }
}

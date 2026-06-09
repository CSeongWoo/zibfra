package com.example.zipfra.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.zipfra.dto.map.Bbox;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BboxValidatorTest {

    @Test
    @DisplayName("T-1: 정상 bbox 파싱")
    void parse_valid() {
        Bbox bbox = BboxValidator.parse("126.9,37.4,127.1,37.6");
        assertThat(bbox.minLng()).isEqualTo(126.9);
        assertThat(bbox.minLat()).isEqualTo(37.4);
        assertThat(bbox.maxLng()).isEqualTo(127.1);
        assertThat(bbox.maxLat()).isEqualTo(37.6);
    }

    @Test
    @DisplayName("T-1: lat/lng 뒤바뀜 → BBOX_COORD_SWAPPED")
    void parse_swapped() {
        assertThatThrownBy(() -> BboxValidator.parse("37.4,126.9,37.6,127.1"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BBOX_COORD_SWAPPED);
    }

    @Test
    @DisplayName("T-1: minLng>maxLng → BBOX_INVALID_RANGE")
    void parse_invalidRange() {
        assertThatThrownBy(() -> BboxValidator.parse("127.1,37.4,126.9,37.6"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BBOX_INVALID_RANGE);
    }

    @Test
    @DisplayName("T-1: 숫자 아님 → BBOX_PARSE_ERROR")
    void parse_notNumber() {
        assertThatThrownBy(() -> BboxValidator.parse("abc,37.4,127.1,37.6"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BBOX_PARSE_ERROR);
    }

    @Test
    @DisplayName("T-1: 숫자 개수 부족 → BBOX_PARSE_ERROR")
    void parse_wrongCount() {
        assertThatThrownBy(() -> BboxValidator.parse("126.9,37.4,127.1"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BBOX_PARSE_ERROR);
    }
}

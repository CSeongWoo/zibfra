package com.example.zipfra.util;

import com.example.zipfra.dto.map.Bbox;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;

/**
 * bbox 문자열 파싱·검증 (MAP-01, §8 공통 규약 + §8.3 에러).
 * 입력: {@code "minLng,minLat,maxLng,maxLat"} (EPSG:4326). lng/lat 순서 불변.
 */
public final class BboxValidator {

    private static final int BBOX_PART_COUNT = 4;
    private static final double LAT_ABS_MAX = 90.0;

    private BboxValidator() {
    }

    /**
     * 검증 순서(§2.1 T-1): 형식 → 좌표 뒤바뀜 → 범위.
     *
     * @throws ApiException BBOX_PARSE_ERROR / BBOX_COORD_SWAPPED /
     *                      BBOX_INVALID_RANGE
     */
    public static Bbox parse(String raw) {
        if (raw == null) {
            throw new ApiException(ErrorCode.BBOX_PARSE_ERROR, "bbox 파라미터가 없습니다.");
        }
        String[] parts = raw.split(",");
        if (parts.length != BBOX_PART_COUNT) {
            throw new ApiException(ErrorCode.BBOX_PARSE_ERROR,
                    "bbox 는 쉼표로 구분된 숫자 4개여야 합니다: " + raw);
        }

        double minLng;
        double minLat;
        double maxLng;
        double maxLat;
        try {
            minLng = Double.parseDouble(parts[0].trim());
            minLat = Double.parseDouble(parts[1].trim());
            maxLng = Double.parseDouble(parts[2].trim());
            maxLat = Double.parseDouble(parts[3].trim());
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.BBOX_PARSE_ERROR,
                    "bbox 좌표를 숫자로 해석할 수 없습니다: " + raw);
        }

        if (Math.abs(minLat) > LAT_ABS_MAX || Math.abs(maxLat) > LAT_ABS_MAX) {
            throw new ApiException(ErrorCode.BBOX_COORD_SWAPPED,
                    "위도(lat) 절댓값이 90을 초과합니다. lng/lat 순서를 확인하세요: " + raw);
        }
        if (minLng >= maxLng || minLat >= maxLat) {
            throw new ApiException(ErrorCode.BBOX_INVALID_RANGE,
                    "min 좌표가 max 이상입니다: " + raw);
        }

        return new Bbox(minLng, minLat, maxLng, maxLat);
    }
}

package com.example.zipfra.util;

/**
 * 좌표 계산 유틸. 거대 bbox 가드(§7.2)의 대각 거리 계산에 사용.
 * 두 점 거리이므로 DB 왕복(ST_Distance) 없이 메모리에서 haversine 으로 계산한다
 * (§4 "ST_Distance 단독 금지"는 인덱스 미사용 테이블 쿼리 한정).
 */
public final class GeoUtil {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoUtil() {
    }

    /** 두 위경도 좌표 간 대권 거리(미터). */
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}

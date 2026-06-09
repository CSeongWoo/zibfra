package com.example.zipfra.dto.map;

import com.example.zipfra.util.GeoUtil;

/**
 * 검증을 통과한 Bounding Box 값 객체 (EPSG:4326, 순서 minLng,minLat,maxLng,maxLat).
 * 파싱·검증은 {@link com.example.zipfra.util.BboxValidator}가 담당한다.
 */
public record Bbox(double minLng, double minLat, double maxLng, double maxLat) {

    /** 남서(SW)~북동(NE) 대각 거리(미터). 거대 bbox 가드(§7.2)용. */
    public double diagonalMeters() {
        return GeoUtil.haversineMeters(minLat, minLng, maxLat, maxLng);
    }
}

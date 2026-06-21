package com.example.zipfra.dto.ingest;

import lombok.Data;

/**
 * 매물이 존재하는 0.02도(≈2km) 격자의 좌하단 좌표. 전국 POI 격자 적재 대상(§9).
 * bbox = [minLng, minLat, minLng+STEP, minLat+STEP].
 */
@Data
public class GridCell {
    private double minLng;
    private double minLat;
}

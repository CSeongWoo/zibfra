package com.example.zipfra.controller;

import java.time.Duration;

import com.example.zipfra.dto.map.MarkerResponse;
import com.example.zipfra.service.MapService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MAP-01 지도 마커 조회 (Public, §8.1).
 * 캐시 헤더: SUMMARY = ETag + max-age=300, SWR60 / DETAIL = no-store.
 */
@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    private static final String SUMMARY = "SUMMARY";

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/markers")
    public ResponseEntity<MarkerResponse> getMarkers(
            @RequestParam String bbox,
            @RequestParam int zoom,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        MarkerResponse body = mapService.getMarkers(bbox, zoom, page, size);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().header("X-Api-Version", "1");
        if (SUMMARY.equals(body.getStrategy())) {
            builder.cacheControl(CacheControl.maxAge(Duration.ofSeconds(300))
                            .staleWhileRevalidate(Duration.ofSeconds(60)))
                    .eTag("\"" + Integer.toHexString(body.getRegions().hashCode()) + "\"");
            if (body.isBboxOversized()) {
                builder.header("Warning", "199 - \"bbox-oversized\"");
            }
        } else {
            builder.cacheControl(CacheControl.noStore());
        }
        return builder.body(body);
    }
}

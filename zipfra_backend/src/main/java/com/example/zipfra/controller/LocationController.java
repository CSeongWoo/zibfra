package com.example.zipfra.controller;

import com.example.zipfra.dto.location.ScoreRequest;
import com.example.zipfra.dto.location.ScoreResponse;
import com.example.zipfra.service.LocationScoreService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LOC-01 입지 점수 API (AGENTS.md §8.1).
 * Protected(Bearer) — SecurityConfig 의 anyRequest().authenticated() 로 보호된다.
 * GET+Body 금지 → POST JSON body. 응답은 no-store, X-Api-Version: 1.
 */
@RestController
@RequestMapping("/api/v1/location")
public class LocationController {

    private final LocationScoreService locationScoreService;

    public LocationController(LocationScoreService locationScoreService) {
        this.locationScoreService = locationScoreService;
    }

    @PostMapping("/score")
    public ResponseEntity<ScoreResponse> score(@RequestBody ScoreRequest request) {
        ScoreResponse response = locationScoreService.score(request);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}

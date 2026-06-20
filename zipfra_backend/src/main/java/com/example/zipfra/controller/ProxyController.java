package com.example.zipfra.controller;

import com.example.zipfra.service.ProxyService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PUB-02 공공데이터 실시간 프록시 (Protected, §8.1).
 * 국토부 실거래가 OpenAPI 를 중계. 응답은 외부 원본(XML) 그대로, no-store.
 */
@RestController
@RequestMapping("/api/v1/proxy")
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/public-data/realtime")
    public ResponseEntity<String> realtime(
            @RequestParam String source,
            @RequestParam int dealYear,
            @RequestParam int dealMonth,
            @RequestParam("lawd_cd") String lawdCd) {

        String body = proxyService.getRealtime(source, dealYear, dealMonth, lawdCd);
        return ResponseEntity.ok()
                .header("X-Api-Version", "1")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON) // 국토부 OpenAPI 기본 응답 = JSON
                .body(body);
    }
}

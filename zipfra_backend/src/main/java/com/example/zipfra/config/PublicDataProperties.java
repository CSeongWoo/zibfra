package com.example.zipfra.config;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PUB-02 공공데이터 프록시 설정 (application.yml {@code public-data.*}, §8.1).
 * serviceKey 는 .env(PUBLIC_DATA_SERVICE_KEY)에서 주입 — 코드/로그/응답 노출 금지.
 */
@Component
@ConfigurationProperties(prefix = "public-data")
@Getter
@Setter
public class PublicDataProperties {

    private String serviceKey;
    private String baseUrl;
    /** source(MOLIT_APT/MOLIT_ROW/COMMERCE) → 엔드포인트 path */
    private Map<String, String> paths;
}

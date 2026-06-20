package com.example.zipfra.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * PUB-02 외부 공공데이터 API 호출용 RestTemplate (§3 RestTemplate 직접 호출).
 * read 타임아웃 5초 → 초과 시 ResourceAccessException → UPSTREAM_TIMEOUT(504, §8.1).
 * (Spring Boot 4 모듈 재편으로 RestTemplateBuilder 대신 코어 RequestFactory 로 타임아웃 설정)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate publicDataRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return new RestTemplate(factory);
    }
}

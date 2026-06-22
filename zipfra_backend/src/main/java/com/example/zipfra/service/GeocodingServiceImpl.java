package com.example.zipfra.service;

import java.net.URI;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link GeocodingService} 구현. 카카오 로컬 주소검색 REST API 호출.
 * 응답은 String 으로 받아 ObjectMapper 로 파싱(Spring Boot 4 컨버터가 JsonNode 직접 역직렬화 불가).
 * 인증: {@code Authorization: KakaoAK {key}}.
 */
@Slf4j
@Service
public class GeocodingServiceImpl implements GeocodingService {

    private final RestTemplate publicDataRestTemplate;
    private final String restApiKey;
    private final String localUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeocodingServiceImpl(RestTemplate publicDataRestTemplate,
                                @Value("${kakao.rest-api-key:}") String restApiKey,
                                @Value("${kakao.local-url:https://dapi.kakao.com/v2/local/search/address.json}") String localUrl) {
        this.publicDataRestTemplate = publicDataRestTemplate;
        this.restApiKey = (restApiKey != null) ? restApiKey.trim() : "";
        this.localUrl = localUrl;
    }

    @Override
    public Optional<double[]> geocode(String address) {
        if (address == null || address.isBlank() || restApiKey.isBlank()) {
            return Optional.empty();
        }
        URI uri = UriComponentsBuilder.fromUriString(localUrl)
                .queryParam("query", address)
                .encode()
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);

        try {
            ResponseEntity<String> resp = publicDataRestTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode docs = objectMapper.readTree(resp.getBody()).path("documents");
            if (docs.isArray() && !docs.isEmpty()) {
                double lng = docs.get(0).path("x").asDouble();
                double lat = docs.get(0).path("y").asDouble();
                if (lng != 0 && lat != 0) {
                    return Optional.of(new double[] { lng, lat });
                }
            }
        } catch (Exception e) {
            log.warn("[geocode] 실패 address={} : {}", address, e.getMessage());
        }
        return Optional.empty();
    }
}

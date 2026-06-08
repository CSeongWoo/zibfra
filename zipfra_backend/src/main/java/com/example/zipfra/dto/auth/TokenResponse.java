package com.example.zipfra.dto.auth;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그인/재발급 응답 DTO.
 * §10: accessToken은 Body, refreshToken은 HttpOnly Cookie로 전달.
 * 이 DTO는 accessToken만 담는다.
 */
@Getter
@Builder
public class TokenResponse {

    private String accessToken;
}

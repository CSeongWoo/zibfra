package com.example.zipfra.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * AUTH-02 로그인 요청 DTO.
 * §8.2: 자격 실패 시 열거 방지용 공통 에러 반환(TOKEN_INVALID 401).
 */
@Getter
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}

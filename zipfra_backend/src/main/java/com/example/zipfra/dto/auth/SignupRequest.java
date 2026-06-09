package com.example.zipfra.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * AUTH-01 회원가입 요청 DTO.
 * §8.2: email(RFC5322,≤255), password(≥8·영문+숫자+특수 각1), nickname(2~20)
 */
@Getter
public class SignupRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 각 1자 이상 포함해야 합니다."
    )
    private String password;

    @NotBlank
    @Size(min = 2, max = 20)
    private String nickname;
}

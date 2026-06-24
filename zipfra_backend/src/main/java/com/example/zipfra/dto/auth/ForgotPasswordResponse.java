package com.example.zipfra.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ForgotPasswordResponse {
    private String tempPassword;
}

package com.example.zipfra.web;

import com.example.zipfra.dto.auth.ForgotPasswordRequest;
import com.example.zipfra.dto.auth.ForgotPasswordResponse;
import com.example.zipfra.dto.auth.LoginRequest;
import com.example.zipfra.dto.auth.SignupRequest;
import com.example.zipfra.dto.auth.TokenDto;
import com.example.zipfra.dto.auth.TokenResponse;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.security.ZipfraPrincipal;
import com.example.zipfra.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * AUTH-01 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * AUTH-02 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenDto tokenDto = authService.login(request);
        setRefreshTokenCookie(response, tokenDto.getRefreshToken(), 14 * 24 * 60 * 60);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(tokenDto.getAccessToken())
                .build();

        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * AUTH-03 Refresh Token 재발급 (RTR)
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        log.info("Refresh 시작");
        if (refreshToken == null) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        TokenDto tokenDto = authService.refresh(refreshToken);
        setRefreshTokenCookie(response, tokenDto.getRefreshToken(), 14 * 24 * 60 * 60);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(tokenDto.getAccessToken())
                .build();

        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * AUTH-04 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response,
                                       @AuthenticationPrincipal ZipfraPrincipal principal) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException(ErrorCode.TOKEN_MISSING);
        }
        String accessToken = authHeader.substring(7);

        if (principal != null) {
            authService.logout(accessToken, principal.getId());
        }

        // Refresh Token 쿠키 삭제
        setRefreshTokenCookie(response, "", 0);

        return ResponseEntity.noContent().build();
    }

    /**
     * AUTH-05 비밀번호 찾기 (임시 비밀번호 발급)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String tempPassword = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(new ForgotPasswordResponse(tempPassword));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // 로컬 개발용(HTTP 통신 허용), 실운영 환경에서는 true 권장
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

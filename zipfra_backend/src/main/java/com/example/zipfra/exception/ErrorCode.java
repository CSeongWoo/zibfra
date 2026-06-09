package com.example.zipfra.exception;

import org.springframework.http.HttpStatus;

/**
 * Zipfra 도메인 에러 코드 열거형.
 * §8.3 에러 코드 테이블과 1:1 매핑.
 */
public enum ErrorCode {

    // 인증/JWT
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.FORBIDDEN, "토큰 서명이 유효하지 않거나 형식이 잘못되었습니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "이미 로그아웃 처리된 토큰입니다."),

    // Refresh Token
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh Token이 유효하지 않거나 탈취가 의심됩니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),

    // 회원
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // ==== [Map 도메인 - main] ====
    MAP_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 지도 데이터를 찾을 수 없습니다."),

    // ==== [공통] ====
    INVALID_PARAM(HttpStatus.BAD_REQUEST, "잘못된 요청 파라미터입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}

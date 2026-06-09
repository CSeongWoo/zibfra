package com.example.zipfra.web;

import java.time.Instant;

/**
 * 공통 에러 응답 body (AGENTS.md §8 공통 규약: {@code { error, message, timestamp }}).
 * timestamp 는 ISO 8601(UTC).
 */
public record ErrorResponse(String error, String message, String timestamp) {

    /** 정적 팩토리: ErrorCode + 메시지로 응답을 만든다(생성 시각 자동 기록). */
    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message, Instant.now().toString());
    }
}

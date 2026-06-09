package com.example.zipfra.exception;

import com.example.zipfra.exception.ErrorCode;
import java.time.Instant;

/**
 * 공통 에러 응답 DTO (main 브랜치 규격 채택)
 */
public record ErrorResponse(
        String error,
        String message,
        String timestamp
) {
    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(
                code.name(),
                message != null ? message : code.getMessage(),
                Instant.now().toString()
        );
    }
}
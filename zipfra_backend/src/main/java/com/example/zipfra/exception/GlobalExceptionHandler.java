package com.example.zipfra.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 공통 예외 핸들러 (AGENTS.md 단일 진실 원천 규격)
 * - User, Map 도메인의 모든 예외를 중앙 제어
 * - X-Api-Version: 1 필수 응답 헤더 포함
 * - 4xx(Warn), 5xx(Error) 자동 분기 로깅
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** * 통합 비즈니스 예외 처리 
     * (기존 ZipfraException과 ApiException을 통합)
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        return build(e.getErrorCode(), e.getMessage());
    }

    /** [User] DTO @Valid 유효성 검증 실패 처리 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return build(ErrorCode.INVALID_PARAM, detail);
    }

    /** [Map/공통] 필수 파라미터 누락 및 타입 불일치 처리 */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadParam(Exception e) {
        return build(ErrorCode.INVALID_PARAM, "요청 파라미터가 누락되었거나 타입이 올바르지 않습니다.");
    }

    /** 서버 내부 미처리 예외 (500) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 서버 예외 발생: ", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .header("X-Api-Version", "1")
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    /** 공통 응답 빌더 (헤더 주입 및 로깅 분기) */
    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message) {
        if (code.getStatus().is5xxServerError()) {
            log.error("[{}] {}", code.name(), message);
        } else {
            log.warn("[{}] {}", code.name(), message);
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(code.getStatus())
                .header("X-Api-Version", "1");
        if (code.getRetryAfterSeconds() != null) {
            builder.header("Retry-After", String.valueOf(code.getRetryAfterSeconds()));
        }
        return builder.body(ErrorResponse.of(code, message));
    }
}
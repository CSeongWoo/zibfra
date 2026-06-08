package com.example.zipfra.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 공통 예외 핸들러 (MAP-01에서 최초 도입, AGENTS.md §8.3).
 * 모든 에러 응답에 {@code X-Api-Version: 1} 헤더와 공통 body 를 부여한다.
 *
 * 팀 규칙(로그): 예외 발생 시 무조건 로그. 여기서 중앙 처리한다 — 4xx=warn, 5xx=error.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return build(e.getErrorCode(), e.getMessage());
    }

    /** 필수 파라미터 누락·타입 불일치(zoom 비숫자 등) → 400 INVALID_PARAM. */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadParam(Exception e) {
        return build(ErrorCode.INVALID_PARAM, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleEtc(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.status())
                .header("X-Api-Version", "1")
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."));
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message) {
        if (code.status().is5xxServerError()) {
            log.error("[{}] {}", code, message);
        } else {
            log.warn("[{}] {}", code, message);
        }
        return ResponseEntity.status(code.status())
                .header("X-Api-Version", "1")
                .body(ErrorResponse.of(code, message));
    }
}

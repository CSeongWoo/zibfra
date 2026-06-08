package com.example.zipfra.web;

import lombok.Getter;

/**
 * 도메인 예외. {@link GlobalExceptionHandler}가 {@link ErrorCode}의 HTTP 상태로 변환한다.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.name());
    }
}

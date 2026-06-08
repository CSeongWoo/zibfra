package com.example.zipfra.exception;

import org.springframework.http.HttpStatus;

/**
 * Zipfra 도메인 예외 최상위 클래스.
 * ErrorCode를 감싸서 GlobalExceptionHandler가 일관된 응답을 만든다.
 */
public class ZipfraException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus customStatus;

    public ZipfraException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customStatus = null;
    }

    public ZipfraException(ErrorCode errorCode, HttpStatus customStatus) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customStatus = customStatus;
    }

    public ZipfraException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.customStatus = null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return customStatus != null ? customStatus : errorCode.getStatus();
    }
}

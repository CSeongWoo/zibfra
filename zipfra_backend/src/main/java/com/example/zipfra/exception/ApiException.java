package com.example.zipfra.exception;

/**
 * Zipfra 프로젝트 통합 비즈니스 예외 클래스
 * 기존 ZipfraException과 ApiException을 하나로 통일했습니다.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
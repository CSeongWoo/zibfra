package com.example.zipfra.web;

import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.exception.ZipfraException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 공통 에러 응답 핸들러.
 * §8 공통 에러 body: { "error", "message", "timestamp" }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ZipfraException.class)
    public ResponseEntity<Map<String, Object>> handleZipfraException(ZipfraException e) {
        return buildResponse(e.getStatus().value(),
                e.getErrorCode().name(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(400, ErrorCode.INVALID_PARAM.name(), detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception e) {
        return buildResponse(500, ErrorCode.INTERNAL_SERVER_ERROR.name(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}

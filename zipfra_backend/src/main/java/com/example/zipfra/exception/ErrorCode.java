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

    // ==== [MAP-01 마커 조회 - kangwon] §7·§8.3 (web/ErrorCode 에서 이관) ====
    /** kangwon — bbox 가 쉼표로 구분된 숫자 4개가 아님 */
    BBOX_PARSE_ERROR(HttpStatus.BAD_REQUEST, "bbox 는 쉼표로 구분된 숫자 4개여야 합니다."),
    /** kangwon — 위도(lat) 절댓값 > 90. lng/lat 순서 뒤바뀜 감지 */
    BBOX_COORD_SWAPPED(HttpStatus.BAD_REQUEST, "위도(lat) 절댓값이 90을 초과합니다. lng/lat 순서를 확인하세요."),
    /** kangwon — min 좌표가 max 이상(minLng>=maxLng 또는 minLat>=maxLat) */
    BBOX_INVALID_RANGE(HttpStatus.BAD_REQUEST, "min 좌표가 max 이상입니다."),
    /** kangwon — bbox 대각 > 150km 인데 줌인(DETAIL) 요청 */
    BBOX_TOO_LARGE_FOR_DETAIL(HttpStatus.BAD_REQUEST, "상세 조회 bbox 대각이 150km 를 초과했습니다."),
    /** kangwon — zoom 이 1~21 범위를 벗어남 */
    ZOOM_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "zoom 은 1~21 범위여야 합니다."),
    /** kangwon — 페이지 size 가 상한(200)을 초과 */
    PAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "size 가 상한(200)을 초과했습니다."),

    // ==== [LOC-01 입지 점수 - kangwon] §8.3 ====
    /** kangwon — lon/lat 좌표가 허용 범위를 벗어남 (lon[-180,180], lat[-90,90]) */
    COORD_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "lon/lat 좌표가 허용 범위를 벗어났습니다."),
    /** kangwon — radiusMeters 가 [100,3000] 범위를 벗어남 */
    RADIUS_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "radiusMeters 는 100~3000 범위여야 합니다."),
    /** kangwon — weights 값이 [0.0,1.0] 범위를 벗어남 */
    WEIGHT_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "weights 값은 0.0~1.0 범위여야 합니다."),

    // ==== [PUB-02 공공데이터 실시간 프록시 - kangwon] §8.3 ====
    /** kangwon — 외부 API 응답 > 5초 */
    UPSTREAM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 API 응답이 지연되었습니다."),
    /** kangwon — 외부 API HTTP 429 (Retry-After: 60) */
    UPSTREAM_RATE_LIMIT(HttpStatus.SERVICE_UNAVAILABLE, "외부 API 호출 한도를 초과했습니다.", 60),
    /** kangwon — 외부 API HTTP 4xx */
    UPSTREAM_CLIENT_ERROR(HttpStatus.BAD_GATEWAY, "외부 API 가 오류를 반환했습니다."),

    // ==== [AI 요약 - AGENTS.md §6.9] ====
    /** AI-01: propertyId에 해당하는 매물 없음 (getPropertyDetail Tool 오류) */
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 매물입니다."),
    /** AI-01/02: 일일 AI 쿼터 초과 (Retry-After: 86400) */
    AI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "일일 AI 요약 쿼터를 초과했습니다.", 86400),

    // ==== [리뷰] ====
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리뷰입니다."),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 작성한 리뷰만 수정/삭제할 수 있습니다."),

    // ==== [즐겨찾기] ====
    FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 즐겨찾기에 추가된 매물입니다."),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "즐겨찾기 항목을 찾을 수 없습니다."),

    // ==== [공통] ====
    INVALID_PARAM(HttpStatus.BAD_REQUEST, "잘못된 요청 파라미터입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
    private final Integer retryAfterSeconds; // null 이면 Retry-After 헤더 미부착

    ErrorCode(HttpStatus status, String message) {
        this(status, message, null);
    }

    ErrorCode(HttpStatus status, String message, Integer retryAfterSeconds) {
        this.status = status;
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
}

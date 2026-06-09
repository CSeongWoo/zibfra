package com.example.zipfra.web;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드 (AGENTS.md §8.3).
 * MAP-01에서 최초 도입하는 공유 자산 — 이후 도메인은 여기에 코드를 추가한다.
 *
 * 팀 규칙: ErrorCode 추가 시 "추가한 사람 / 목적"을 주석으로 남긴다.
 */
public enum ErrorCode {

    // --- MAP-01 (kangwon): 지도 마커 조회 §7·§8.3 ---
    /** kangwon — bbox가 쉼표 구분 숫자 4개가 아님. */
    BBOX_PARSE_ERROR(HttpStatus.BAD_REQUEST),
    /** kangwon — 위도(lat) 절댓값 > 90. lng/lat 순서 뒤바뀜 감지. */
    BBOX_COORD_SWAPPED(HttpStatus.BAD_REQUEST),
    /** kangwon — min 좌표가 max 이상(minLng>=maxLng 또는 minLat>=maxLat). */
    BBOX_INVALID_RANGE(HttpStatus.BAD_REQUEST),
    /** kangwon — bbox 대각 > 150km 인데 줌인(DETAIL) 요청. */
    BBOX_TOO_LARGE_FOR_DETAIL(HttpStatus.BAD_REQUEST),
    /** kangwon — zoom 이 1~21 범위를 벗어남. */
    ZOOM_OUT_OF_RANGE(HttpStatus.BAD_REQUEST),
    /** kangwon — 페이지 size 가 상한(200)을 초과. */
    PAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST),

    // --- 공통 (kangwon) ---
    /** kangwon — 필수 파라미터 누락·타입 불일치 등 일반 파라미터 오류. */
    INVALID_PARAM(HttpStatus.BAD_REQUEST),
    /** kangwon — 처리되지 않은 서버 내부 오류. */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}

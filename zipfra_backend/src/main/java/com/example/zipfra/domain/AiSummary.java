package com.example.zipfra.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * mysql_ai_summaries 테이블 매핑 도메인 객체 (AGENTS.md §6.3).
 *
 * <pre>
 * summary_type   VARCHAR(20)  — PROPERTY | REVIEW
 * target_id      BIGINT       — propertyId (PROPERTY) 또는 targetId (REVIEW)
 * target_type    VARCHAR(20)  — PROPERTY 시 null, REVIEW 시 BUILDING|AREA
 * summary_available TINYINT(1)
 * summary_json   JSON         — 전체 응답 페이로드 직렬화. LLM 불가 시 null
 * created_at     DATETIME
 * expires_at     DATETIME     — created_at + 24h
 * </pre>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummary {

    private Long id;

    /** 요약 유형: PROPERTY | REVIEW */
    private String summaryType;

    /** 조회 대상 ID (propertyId 또는 targetId). */
    private Long targetId;

    /** 조회 대상 유형 (PROPERTY 시 null, REVIEW 시 BUILDING|AREA). */
    private String targetType;

    /** LLM 요약 가용 여부. Fallback 상태이면 false. */
    private boolean summaryAvailable;

    /**
     * 전체 응답 페이로드 JSON 직렬화 문자열.
     * summaryAvailable=false 이면 null (AGENTS.md §6.3: 실패 결과 캐시 저장 금지).
     */
    private String summaryJson;

    /** 캐시 생성 시각. */
    private LocalDateTime createdAt;

    /**
     * 캐시 만료 시각 (created_at + 24h).
     * expires_at > NOW() 이어야 유효한 캐시로 간주.
     */
    private LocalDateTime expiresAt;

    /** 캐시가 현재 시각 기준으로 유효한지 확인. */
    public boolean isValid() {
        return summaryAvailable
                && summaryJson != null
                && expiresAt != null
                && expiresAt.isAfter(LocalDateTime.now());
    }
}

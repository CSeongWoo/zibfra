package com.example.zipfra.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * ai_summaries 테이블 매핑 도메인 객체 (AGENTS.md §6.3).
 *
 * <pre>
 * summary_type     VARCHAR(50)  — PROPERTY_INFO | REVIEW
 * target_type      VARCHAR(50)  — PROPERTY | BUILDING | AREA
 * target_id        VARCHAR(255) — 논리참조 (매물ID 또는 법정동코드)
 * summary          TEXT         — 요약 본문
 * positives        JSON         — 긍정 테마 배열 직렬화 텍스트
 * negatives        JSON         — 부정 테마 배열 직렬화 텍스트
 * review_count     INT          — 분석된 리뷰 수
 * model_name       VARCHAR(100) — 예: gpt-4o-mini
 * summary_available TINYINT(1)
 * generated_at     DATETIME
 * expires_at       DATETIME     — 캐시 만료 시간 (TTL 24h)
 * created_at       DATETIME
 * updated_at       DATETIME
 * </pre>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummary {

    private Long id;

    /** 요약 유형: PROPERTY_INFO | REVIEW */
    private String summaryType;

    /** 조회 대상 유형: PROPERTY | BUILDING | AREA */
    private String targetType;

    /** 조회 대상 ID: 매물ID 또는 법정동코드 등 */
    private String targetId;

    /** 요약 본문 (Fallback 시 null 가능) */
    private String summary;

    /** 긍정 테마 배열 JSON 문자열 (Nullable) */
    private String positives;

    /** 부정 테마 배열 JSON 문자열 (Nullable) */
    private String negatives;

    /** 분석된 리뷰 수 */
    private Integer reviewCount;

    /** LLM 모델명 (예: gpt-4o-mini) */
    private String modelName;

    /** LLM 요약 가용 여부. Fallback 상태이면 false. */
    private boolean summaryAvailable;

    /** 요약 생성 시각 */
    private LocalDateTime generatedAt;

    /** 캐시 만료 시각. expires_at > NOW() 이어야 유효한 캐시. */
    private LocalDateTime expiresAt;

    /** 데이터 생성 시각 */
    private LocalDateTime createdAt;

    /** 데이터 수정 시각 */
    private LocalDateTime updatedAt;

    /** 캐시가 현재 시각 기준으로 유효한지 확인. */
    public boolean isValid() {
        return summaryAvailable
                && expiresAt != null
                && expiresAt.isAfter(LocalDateTime.now());
    }
}

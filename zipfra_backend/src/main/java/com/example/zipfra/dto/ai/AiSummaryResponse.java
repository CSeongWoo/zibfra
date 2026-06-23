package com.example.zipfra.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 요약 통합 응답 DTO (AGENTS.md §6.6·§6.7).
 *
 * <p>summaryAvailable=false 시 summary·positives·negatives·generatedAt은 null.
 * @JsonInclude(NON_NULL)로 null 필드는 응답 JSON에 포함되지 않는다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiSummaryResponse {

    // ─── 공통 ─────────────────────────────────────────────────────────────────

    /**
     * LLM 요약 가용 여부.
     * LLM 타임아웃/오류 시 false, 캐시 HIT 또는 정상 생성 시 true.
     */
    private final boolean summaryAvailable;

    // ─── AI-01 전용 ────────────────────────────────────────────────────────────

    /** 매물 ID. */
    private final Long propertyId;

    /**
     * 부동산·인프라 자연어 요약문 (3~5문장).
     * summaryAvailable=false 이면 null.
     */
    private final String summary;

    // ─── AI-02 전용 ────────────────────────────────────────────────────────────

    /** 리뷰 대상 유형 (BUILDING | AREA). */
    private final String targetType;

    /** 리뷰 대상 ID. */
    private final String targetId;

    /** 요약에 사용된 리뷰 건수. */
    private final Integer reviewCount;

    /**
     * 긍정 테마 목록 (최대 3가지).
     * summaryAvailable=false 이면 null.
     */
    private final List<String> positives;

    /**
     * 부정 테마 목록 (최대 3가지).
     * summaryAvailable=false 이면 null.
     */
    private final List<String> negatives;

    // ─── 공통 (하단) ──────────────────────────────────────────────────────────

    /**
     * 요약 생성 시각 (ISO 8601).
     * summaryAvailable=false 이면 null.
     * 캐시 HIT 시 원본 생성 시각, 신규 생성 시 현재 시각.
     */
    private final LocalDateTime generatedAt;

    // ─── 팩토리 메서드 ────────────────────────────────────────────────────────

    /** AI-01 Fallback 응답 (LLM 오류·타임아웃). */
    public static AiSummaryResponse propertyFallback(Long propertyId) {
        return AiSummaryResponse.builder()
                .summaryAvailable(false)
                .propertyId(propertyId)
                .build();
    }

    /** AI-02 Fallback 응답 (리뷰 0건 포함). */
    public static AiSummaryResponse reviewFallback(String targetType, String targetId, int reviewCount) {
        return AiSummaryResponse.builder()
                .summaryAvailable(false)
                .targetType(targetType)
                .targetId(targetId)
                .reviewCount(reviewCount)
                .build();
    }
}

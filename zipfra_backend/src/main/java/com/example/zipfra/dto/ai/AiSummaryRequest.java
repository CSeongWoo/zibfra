package com.example.zipfra.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 요약 통합 요청 DTO (AGENTS.md §6.6·§6.7).
 *
 * <p>AI-01(PROPERTY)과 AI-02(REVIEW)를 단일 엔드포인트로 수용.
 * summaryType=PROPERTY 시 propertyId 필수.
 * summaryType=REVIEW 시 targetType·targetId 필수.
 */
@Getter
@NoArgsConstructor
public class AiSummaryRequest {

    /**
     * 요약 유형.
     * <ul>
     *   <li>PROPERTY — 부동산·인프라 요약 (AI-01, §6.6)</li>
     *   <li>REVIEW    — 리뷰 요약 (AI-02, §6.7)</li>
     * </ul>
     */
    @NotNull(message = "summaryType은 필수입니다.")
    private SummaryType summaryType;

    // ─── AI-01 전용 ────────────────────────────────────────────────────────────

    /** 매물 ID (summaryType=PROPERTY 시 필수). */
    private Long propertyId;

    /**
     * 입지 점수 breakdown 포함 여부 (default true).
     * false 로 설정하면 getLocationBreakdown Tool 호출을 생략한다.
     */
    private boolean includeScore = true;

    // ─── AI-02 전용 ────────────────────────────────────────────────────────────

    /**
     * 대상 유형 (summaryType=REVIEW 시 필수).
     * BUILDING | AREA
     */
    private String targetType;

    /** 대상 ID (summaryType=REVIEW 시 필수). */
    private String targetId;

    /**
     * 최대 리뷰 수 (1~50, default 30).
     * 51 이상이면 400 INVALID_PARAM.
     */
    @Min(value = 1, message = "maxReviews는 1 이상이어야 합니다.")
    @Max(value = 50, message = "maxReviews는 50 이하여야 합니다.")
    private int maxReviews = 30;

    // ─── 요약 유형 Enum ────────────────────────────────────────────────────────

    public enum SummaryType {
        /** 부동산·인프라 요약 (AI-01). */
        PROPERTY,
        /** 리뷰 요약 (AI-02). */
        REVIEW
    }
}

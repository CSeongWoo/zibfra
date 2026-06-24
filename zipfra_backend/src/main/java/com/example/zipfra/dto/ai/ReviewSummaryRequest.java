package com.example.zipfra.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI-02 리뷰 요약 요청 DTO (AGENTS.md §6.7).
 *
 * <p>POST /api/v1/ai/review-summary 전용.
 * targetType은 BUILDING | AREA, maxReviews는 1~50(default 30).
 */
@Getter
@NoArgsConstructor
public class ReviewSummaryRequest {

    /**
     * 대상 유형.
     * BUILDING — 건물 단위 / AREA — 법정동 단위
     */
    @NotBlank(message = "targetType은 필수입니다.")
    private String targetType;

    /** 대상 ID (건물 ID 또는 법정동 코드). */
    @NotBlank(message = "targetId는 필수입니다.")
    private String targetId;

    /**
     * 최대 리뷰 수 (1~50, default 30).
     * 51 이상이면 400 INVALID_PARAM.
     */
    @Min(value = 1, message = "maxReviews는 1 이상이어야 합니다.")
    @Max(value = 50, message = "maxReviews는 50 이하여야 합니다.")
    private int maxReviews = 30;
}

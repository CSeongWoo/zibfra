package com.example.zipfra.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI-02 리뷰 요약 응답 DTO (AGENTS.md §6.7).
 *
 * <p>summaryAvailable=false 시 summary·positives·negatives·generatedAt은 null.
 * {@code @JsonInclude(NON_NULL)}로 null 필드는 응답 JSON에서 제외된다.
 *
 * <p>Fallback 조건 (DB 저장 금지):
 * <ul>
 *   <li>LLM 타임아웃 / 5xx (§6.9)</li>
 *   <li>리뷰 0건 (reviewCount=0)</li>
 *   <li>LLM JSON 파싱 실패</li>
 * </ul>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewSummaryResponse {

    /** LLM 요약 가용 여부. Fallback 시 false. */
    private final boolean summaryAvailable;

    /** 리뷰 대상 유형 (BUILDING | AREA). */
    private final String targetType;

    /** 리뷰 대상 ID. */
    private final String targetId;

    /** 요약에 사용된 리뷰 건수. summaryAvailable=false 시 0 또는 null. */
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

    /**
     * 전체 요약문 (2문장).
     * summaryAvailable=false 이면 null.
     */
    private final String summary;

    /**
     * 요약 생성 시각 (ISO 8601).
     * 캐시 HIT 시 원본 생성 시각, 신규 생성 시 현재 시각.
     * summaryAvailable=false 이면 null.
     */
    private final LocalDateTime generatedAt;

    // ─── 팩토리 메서드 ────────────────────────────────────────────────────────

    /** Fallback 응답 (LLM 오류·타임아웃·리뷰 0건). DB 저장 금지. */
    public static ReviewSummaryResponse fallback(String targetType, String targetId, int reviewCount) {
        return ReviewSummaryResponse.builder()
                .summaryAvailable(false)
                .targetType(targetType)
                .targetId(targetId)
                .reviewCount(reviewCount)
                .build();
    }
}

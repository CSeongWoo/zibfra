package com.example.zipfra.dto.map;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * MAP-01 응답 (response 전용). 줌 전략에 따라 두 형태로 직렬화된다(§7.1).
 * <ul>
 *   <li>DETAIL: {@code markers} + 페이지네이션 필드</li>
 *   <li>SUMMARY: {@code regions} (페이지네이션 없음)</li>
 * </ul>
 * null 필드는 직렬화에서 제외하여 형태별로 깔끔한 body 를 만든다.
 *
 * 생성은 정적 팩토리({@link #detail}, {@link #summary})로만 — 빌더는 내부 조립용(private).
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarkerResponse {

    private final String strategy;                  // "DETAIL" | "SUMMARY"
    private final List<MarkerDTO> markers;          // DETAIL 전용
    private final List<RegionSummaryDTO> regions;   // SUMMARY 전용
    private final Integer page;                     // DETAIL 전용
    private final Integer size;                     // DETAIL 전용
    private final Long totalCount;                  // DETAIL 전용
    private final Boolean hasNext;                  // DETAIL 전용
    private final boolean bboxOversized;            // 항상 포함

    /** 줌인 상세 응답. */
    public static MarkerResponse detail(List<MarkerDTO> markers, int page, int size,
                                        long totalCount, boolean hasNext) {
        return MarkerResponse.builder()
                .strategy("DETAIL")
                .markers(markers)
                .page(page)
                .size(size)
                .totalCount(totalCount)
                .hasNext(hasNext)
                .bboxOversized(false)
                .build();
    }

    /** 줌아웃 요약 응답. {@code bboxOversized}=true 면 거대 bbox 강제 다운그레이드(§7.2). */
    public static MarkerResponse summary(List<RegionSummaryDTO> regions, boolean bboxOversized) {
        return MarkerResponse.builder()
                .strategy("SUMMARY")
                .regions(regions)
                .bboxOversized(bboxOversized)
                .build();
    }
}

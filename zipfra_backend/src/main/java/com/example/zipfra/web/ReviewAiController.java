package com.example.zipfra.web;

import com.example.zipfra.dto.ai.ReviewSummaryRequest;
import com.example.zipfra.dto.ai.ReviewSummaryResponse;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.security.ZipfraPrincipal;
import com.example.zipfra.service.ai.AiReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-02 리뷰 요약 컨트롤러 (AGENTS.md §6.7).
 *
 * <h3>엔드포인트</h3>
 * <pre>
 * POST /api/v1/ai/review-summary  — Bearer JWT 필수 (Protected)
 * </pre>
 *
 * <h3>인증</h3>
 * Protected(Bearer JWT). 미인증 → 401 TOKEN_MISSING (SecurityConfig에서 처리).
 *
 * <h3>에러 처리</h3>
 * LLM 오류는 5xx 노출 없이 Fallback({ summaryAvailable:false })으로 처리.
 * 도메인 예외(INVALID_PARAM·AI_QUOTA_EXCEEDED)는 GlobalExceptionHandler 위임.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI 요약", description = "리뷰 긍부정 요약 (AI-02) — AGENTS.md §6.7")
public class ReviewAiController {

    private final AiReviewService aiReviewService;

    /**
     * AI-02 리뷰 요약 생성.
     *
     * <p>캐시 HIT 시 LLM 미호출(24h TTL, §6.3).
     * 리뷰 0건 또는 LLM 타임아웃/오류 시 {@code summaryAvailable:false} Fallback 반환(HTTP 200, §6.9).
     *
     * @param principal 인증된 사용자 (JWT → ZipfraPrincipal)
     * @param request   리뷰 요약 요청 DTO
     * @return 200 + ReviewSummaryResponse (summaryAvailable=false 포함)
     * @throws ApiException TOKEN_MISSING     — Authorization 헤더 없음 (401)
     * @throws ApiException INVALID_PARAM     — 파라미터 검증 실패 (400)
     * @throws ApiException AI_QUOTA_EXCEEDED — 일일 쿼터 초과 (429, Retry-After: 86400)
     */
    @PostMapping("/review-summary")
    @Operation(
        summary = "AI-02 리뷰 요약 생성",
        description = "건물(BUILDING) 또는 지역(AREA)의 최신 리뷰를 LLM으로 분석하여 긍/부정 요소와 전체 요약을 반환합니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = {
                @ExampleObject(name = "건물 리뷰 요약", value = """
                    { "targetType": "BUILDING", "targetId": "10023", "maxReviews": 30 }
                    """),
                @ExampleObject(name = "지역 리뷰 요약", value = """
                    { "targetType": "AREA", "targetId": "1168010600", "maxReviews": 20 }
                    """)
            })
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "성공 (summaryAvailable=false 포함)"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터 (INVALID_PARAM)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (TOKEN_MISSING / TOKEN_EXPIRED / TOKEN_BLACKLISTED)"),
            @ApiResponse(responseCode = "403", description = "인가 실패 (TOKEN_INVALID)"),
            @ApiResponse(responseCode = "429", description = "쿼터 초과 (AI_QUOTA_EXCEEDED, Retry-After: 86400)")
        }
    )
    public ResponseEntity<ReviewSummaryResponse> summarizeReview(
            @AuthenticationPrincipal ZipfraPrincipal principal,
            @Valid @RequestBody ReviewSummaryRequest request) {

        if (principal == null) {
            throw new ApiException(ErrorCode.TOKEN_MISSING);
        }

        log.info("[AI-02] 리뷰 요약 요청 userId={}, targetType={}, targetId={}",
                principal.getId(), request.getTargetType(), request.getTargetId());

        ReviewSummaryResponse response = aiReviewService.summarize(principal.getId(), request);

        log.info("[AI-02] 완료 userId={}, targetId={}, available={}",
                principal.getId(), request.getTargetId(), response.isSummaryAvailable());

        return ResponseEntity.ok(response);
    }
}

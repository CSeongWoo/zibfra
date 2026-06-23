package com.example.zipfra.web;

import com.example.zipfra.dto.ai.AiSummaryRequest;
import com.example.zipfra.dto.ai.AiSummaryResponse;
import com.example.zipfra.exception.ApiException;
import com.example.zipfra.exception.ErrorCode;
import com.example.zipfra.security.ZipfraPrincipal;
import com.example.zipfra.service.ai.AiSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * AI 요약 통합 컨트롤러 (AGENTS.md §6.6·§6.7·§8.2).
 *
 * <h3>엔드포인트</h3>
 * <pre>
 * POST /api/v1/ai/summary
 *   - summaryType=PROPERTY : AI-01 부동산·인프라 요약
 *   - summaryType=REVIEW   : AI-02 리뷰 요약
 * </pre>
 *
 * <h3>인증</h3>
 * 전체 Protected(Bearer JWT). 미인증 → 401 TOKEN_MISSING (§6.9).
 *
 * <h3>에러 처리</h3>
 * LLM 오류는 5xx 노출 없이 Fallback({ summaryAvailable:false })으로 처리.
 * 도메인 예외(PROPERTY_NOT_FOUND·INVALID_PARAM·AI_QUOTA_EXCEEDED)는 GlobalExceptionHandler 위임.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI 요약", description = "부동산·인프라 요약(AI-01) 및 리뷰 요약(AI-02) — AGENTS.md §6")
public class AiSummaryController {

    private final AiSummaryService aiSummaryService;

    /**
     * AI 요약 생성 엔드포인트 (AI-01 + AI-02 통합).
     *
     * <p>요청의 {@code summaryType} 필드로 AI-01/AI-02를 분기한다:
     * <ul>
     *   <li>PROPERTY: {@code propertyId} 필수. {@code includeScore} 생략 시 true.</li>
     *   <li>REVIEW: {@code targetType}(BUILDING|AREA), {@code targetId} 필수.
     *       {@code maxReviews} 생략 시 30, 최대 50.</li>
     * </ul>
     *
     * <p>캐시 HIT 시 LLM 미호출(24h TTL, §6.3).
     * LLM 타임아웃/오류 시 {@code summaryAvailable:false} Fallback 반환(HTTP 200, §6.9).
     *
     * @param principal 인증된 사용자 (JWT → ZipfraPrincipal)
     * @param request   AI 요약 요청 DTO
     * @return 200 + AiSummaryResponse (summaryAvailable=false 포함)
     * @throws ApiException TOKEN_MISSING — Authorization 헤더 없음 (401)
     * @throws ApiException INVALID_PARAM — 파라미터 검증 실패 (400)
     * @throws ApiException PROPERTY_NOT_FOUND — 존재하지 않는 propertyId (404)
     * @throws ApiException AI_QUOTA_EXCEEDED — 일일 쿼터 초과 (429)
     */
    @PostMapping("/summary")
    @Operation(
        summary = "AI 요약 생성 (AI-01 / AI-02)",
        description = "summaryType=PROPERTY: 부동산·인프라 자연어 요약, summaryType=REVIEW: 리뷰 긍부정 분석",
        security = @SecurityRequirement(name = "bearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = {
                @ExampleObject(name = "AI-01 매물 요약", value = """
                    { "summaryType": "PROPERTY", "propertyId": 10023, "includeScore": true }
                    """),
                @ExampleObject(name = "AI-02 리뷰 요약", value = """
                    { "summaryType": "REVIEW", "targetType": "BUILDING", "targetId": "10023", "maxReviews": 30 }
                    """)
            })
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "성공 (summaryAvailable=false 포함)"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터 (INVALID_PARAM)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (TOKEN_MISSING / TOKEN_EXPIRED / TOKEN_BLACKLISTED)"),
            @ApiResponse(responseCode = "403", description = "인가 실패 (TOKEN_INVALID)"),
            @ApiResponse(responseCode = "404", description = "매물 없음 (PROPERTY_NOT_FOUND)"),
            @ApiResponse(responseCode = "429", description = "쿼터 초과 (AI_QUOTA_EXCEEDED, Retry-After: 86400)")
        }
    )
    public ResponseEntity<AiSummaryResponse> summarize(
            @AuthenticationPrincipal ZipfraPrincipal principal,
            @Valid @RequestBody AiSummaryRequest request) {

        // 인증 체크 (SecurityConfig에서 Protected로 설정하지만 명시적 방어)
        if (principal == null) {
            throw new ApiException(ErrorCode.TOKEN_MISSING);
        }

        log.info("[AI 요약] userId={}, summaryType={}", principal.getId(), request.getSummaryType());

        AiSummaryResponse response = aiSummaryService.summarize(principal.getId(), request);

        log.info("[AI 요약] 완료 userId={}, summaryType={}, available={}",
                principal.getId(), request.getSummaryType(), response.isSummaryAvailable());

        return ResponseEntity.ok(response);
    }
}

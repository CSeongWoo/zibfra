package com.example.zipfra.service.ai;

import com.example.zipfra.dto.review.ReviewResponse;
import com.example.zipfra.mapper.mysql.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Spring AI @Tool 컴포넌트 — AI-02 리뷰 요약 전용 (AGENTS.md §6.4.1).
 *
 * <p>단일 책임: LLM이 Function Calling으로 호출하는 {@link #getReviews} 하나만 노출.
 *
 * <p><b>가드레일 (§6.8)</b>:
 * <ol>
 *   <li>PII 마스킹 — 전화번호·이메일·주민번호 → {@code [REDACTED]}</li>
 *   <li>프롬프트 인젝션 방지 — {@code ---USER REVIEW START---} / {@code ---USER REVIEW END---} 구분자</li>
 *   <li>토큰 절단 — 한글 1자 ≈ 2.5 tokens, 1,500 토큰 상한 초과 시 최신 리뷰부터 절단</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewTools {

    private static final int    MAX_TOKEN_ESTIMATE       = 1500;
    private static final double AVG_TOKENS_PER_KO_CHAR  = 2.5;

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\d{2,3})[\\-\\s]?(\\d{3,4})[\\-\\s]?(\\d{4})");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SSN_PATTERN =
            Pattern.compile("(\\d{6})-[1-4]\\d{6}");

    private static final String REVIEW_START = "---USER REVIEW START---";
    private static final String REVIEW_END   = "---USER REVIEW END---";

    private final ReviewMapper reviewMapper;

    /**
     * [AI-02 Tool] PII 마스킹된 리뷰 목록 조회 (AGENTS.md §6.4.1 getReviews).
     *
     * <p>MySQL reviews 테이블에서 최신순으로 {@code maxReviews}건을 조회하고,
     * PII 마스킹 → 토큰 절단 → 인젝션 방지 구분자 삽입 후 반환한다.
     *
     * @param targetType BUILDING | AREA
     * @param targetId   건물 또는 법정동 ID
     * @param maxReviews 최대 조회 건수 (1~50, null이면 30)
     * @return 마스킹·구분자 처리된 리뷰 텍스트 (LLM Resource)
     */
    @Tool(name = "getReviews",
          description = "지정된 건물(BUILDING) 또는 지역(AREA)의 최신 리뷰 목록을 PII 마스킹 처리 후 반환합니다.")
    @Transactional(transactionManager = "primaryTransactionManager",
                   readOnly = true,
                   propagation = Propagation.SUPPORTS)
    public String getReviews(String targetType, String targetId, Integer maxReviews) {
        int limit = (maxReviews != null) ? Math.min(maxReviews, 50) : 30;
        log.debug("[ReviewTool][getReviews] targetType={}, targetId={}, limit={}", targetType, targetId, limit);

        List<ReviewResponse> reviews = reviewMapper.findByTarget(targetType, targetId, 0, limit);
        long totalCount = reviewMapper.countByTarget(targetType, targetId);

        log.debug("[ReviewTool][getReviews] 조회된 리뷰 {}건 (전체 {}건)", reviews.size(), totalCount);

        if (reviews.isEmpty()) {
            return "리뷰 건수: 0건\n리뷰 데이터가 없습니다.";
        }

        List<String> masked = reviews.stream()
                .map(r -> maskPii(r.getContent()))
                .collect(Collectors.toList());

        List<String> truncated = truncateToTokenLimit(masked);

        StringBuilder sb = new StringBuilder();
        sb.append("리뷰 건수: ").append(reviews.size()).append("건 (전체 ").append(totalCount).append("건)\n\n");
        for (int i = 0; i < truncated.size(); i++) {
            sb.append(REVIEW_START).append("\n");
            sb.append("[리뷰 ").append(i + 1).append("] ").append(truncated.get(i)).append("\n");
            sb.append(REVIEW_END).append("\n\n");
        }

        String result = sb.toString();
        log.debug("[ReviewTool][getReviews] 최종 리뷰 텍스트 {}자", result.length());
        return result;
    }

    /** 전화번호, 이메일, 주민번호를 [REDACTED]로 치환 (AGENTS.md §6.8). */
    private String maskPii(String content) {
        if (content == null) return "";
        return SSN_PATTERN.matcher(
                EMAIL_PATTERN.matcher(
                        PHONE_PATTERN.matcher(content)
                                .replaceAll("[REDACTED]")
                ).replaceAll("[REDACTED]")
        ).replaceAll("[REDACTED]");
    }

    /** 총 토큰 추정치가 MAX_TOKEN_ESTIMATE 초과 시 뒤쪽 리뷰부터 제거. */
    private List<String> truncateToTokenLimit(List<String> reviews) {
        double totalTokens = 0;
        int cutAt = 0;
        for (String review : reviews) {
            double tokens = review.length() * AVG_TOKENS_PER_KO_CHAR;
            if (totalTokens + tokens > MAX_TOKEN_ESTIMATE) {
                log.debug("[ReviewTool][getReviews] 토큰 초과로 {}번째 이후 리뷰 절단", cutAt);
                break;
            }
            totalTokens += tokens;
            cutAt++;
        }
        return cutAt == reviews.size() ? reviews : reviews.subList(0, cutAt);
    }
}

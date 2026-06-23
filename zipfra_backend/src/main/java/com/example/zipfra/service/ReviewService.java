package com.example.zipfra.service;

import com.example.zipfra.domain.Review;
import com.example.zipfra.dto.review.PageResponse;
import com.example.zipfra.dto.review.ReviewRequest;
import com.example.zipfra.dto.review.ReviewResponse;
import com.example.zipfra.mapper.mysql.ReviewMapper;
import com.example.zipfra.service.ai.AiSummaryService;
import com.example.zipfra.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 리뷰 서비스 (REV-01~REV-04, AGENTS.md §8.2).
 *
 * <p>리뷰 작성(createReview) 시 AI 요약 캐시(mysql_ai_summaries)를
 * 즉시 만료시킨다 (§6.8 리뷰 이벤트 무효화 규칙).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final CryptoUtils cryptoUtils;
    private final AiSummaryService aiSummaryService;

    @Transactional("primaryTransactionManager")
    public void createReview(Long userId, ReviewRequest request) {
        String originalContent = request.getContent();

        // 1. PII Masking
        String maskedContent = cryptoUtils.maskPii(originalContent);

        // 2. Encryption
        byte[] encryptedContent = cryptoUtils.encrypt(originalContent);

        // 3. Save
        Review review = Review.builder()
                .userId(userId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .content(maskedContent)
                .encryptedContent(encryptedContent)
                .rating(request.getRating())
                .build();

        reviewMapper.insert(review);

        // 4. AI 요약 캐시 즉시 만료 (§6.8 리뷰 이벤트 무효화)
        invalidateAiCacheSafely(request.getTargetId());
    }

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public PageResponse<ReviewResponse> getReviews(String targetType, String targetId, int page, int size) {
        int offset = page * size;
        List<ReviewResponse> content = reviewMapper.findByTarget(targetType, targetId, offset, size);
        long totalElements = reviewMapper.countByTarget(targetType, targetId);
        boolean hasNext = (offset + size) < totalElements;

        return PageResponse.<ReviewResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .hasNext(hasNext)
                .build();
    }

    /**
     * targetId 문자열로 AI 요약 캐시를 안전하게 무효화한다.
     * NumberFormatException은 경고 로그 후 스킵 (비치명적).
     */
    private void invalidateAiCacheSafely(String targetId) {
        try {
            Long targetIdLong = Long.parseLong(targetId);
            aiSummaryService.invalidateReviewCache(targetIdLong);
            log.debug("[ReviewService] AI 요약 캐시 무효화 완료 targetId={}", targetId);
        } catch (NumberFormatException e) {
            log.warn("[ReviewService] targetId 파싱 실패 — 캐시 무효화 스킵 targetId={}", targetId);
        }
    }
}

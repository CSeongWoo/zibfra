package com.example.zipfra.service;

import com.example.zipfra.domain.Review;
import com.example.zipfra.dto.review.PageResponse;
import com.example.zipfra.dto.review.ReviewRequest;
import com.example.zipfra.dto.review.ReviewResponse;
import com.example.zipfra.mapper.mysql.ReviewMapper;
import com.example.zipfra.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final CryptoUtils cryptoUtils;

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
}

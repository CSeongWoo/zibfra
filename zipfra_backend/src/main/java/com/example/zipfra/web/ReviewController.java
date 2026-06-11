package com.example.zipfra.web;

import com.example.zipfra.dto.review.PageResponse;
import com.example.zipfra.dto.review.ReviewRequest;
import com.example.zipfra.dto.review.ReviewResponse;
import com.example.zipfra.security.ZipfraPrincipal;
import com.example.zipfra.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Void> createReview(@AuthenticationPrincipal ZipfraPrincipal principal,
                                             @Valid @RequestBody ReviewRequest request) {
        if (principal == null) {
            throw new com.example.zipfra.exception.ApiException(com.example.zipfra.exception.ErrorCode.TOKEN_MISSING);
        }

        reviewService.createReview(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @RequestParam String targetType,
            @RequestParam String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 50) {
            throw new com.example.zipfra.exception.ApiException(
                    com.example.zipfra.exception.ErrorCode.INVALID_PARAM,
                    "page must be >= 0 and size must be between 1 and 50"
            );
        }

        PageResponse<ReviewResponse> response = reviewService.getReviews(targetType, targetId, page, size);
        return ResponseEntity.ok(response);
    }
}

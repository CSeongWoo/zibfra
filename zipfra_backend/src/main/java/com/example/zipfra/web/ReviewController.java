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

import java.util.Map;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> createReview(@AuthenticationPrincipal ZipfraPrincipal principal,
                                          @Valid @RequestBody ReviewRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "TOKEN_MISSING"));
        }

        try {
            reviewService.createReview(principal.getId(), request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "INTERNAL_SERVER_ERROR"));
        }
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @RequestParam String targetType,
            @RequestParam String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        PageResponse<ReviewResponse> response = reviewService.getReviews(targetType, targetId, page, size);
        return ResponseEntity.ok(response);
    }
}

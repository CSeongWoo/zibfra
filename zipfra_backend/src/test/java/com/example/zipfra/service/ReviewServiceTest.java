package com.example.zipfra.service;

import com.example.zipfra.domain.Review;
import com.example.zipfra.dto.review.PageResponse;
import com.example.zipfra.dto.review.ReviewRequest;
import com.example.zipfra.dto.review.ReviewResponse;
import com.example.zipfra.mapper.mysql.ReviewMapper;
import com.example.zipfra.util.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private CryptoUtils cryptoUtils;

    @InjectMocks
    private ReviewService reviewService;

    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        reviewRequest = new ReviewRequest();
        reviewRequest.setTargetType("BUILDING");
        reviewRequest.setTargetId("100");
        reviewRequest.setContent("Good building. Call me 010-1234-5678");
        reviewRequest.setRating(5);
    }

    @Test
    void testCreateReview_MasksAndEncryptsContent() {
        // Given
        String maskedText = "Good building. Call me 010-****-5678";
        byte[] cipherText = "encrypted_bytes".getBytes();
        given(cryptoUtils.maskPii(reviewRequest.getContent())).willReturn(maskedText);
        given(cryptoUtils.encrypt(reviewRequest.getContent())).willReturn(cipherText);

        // When
        reviewService.createReview(1L, reviewRequest);

        // Then
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(reviewCaptor.capture());

        Review capturedReview = reviewCaptor.getValue();
        assertThat(capturedReview.getUserId()).isEqualTo(1L);
        assertThat(capturedReview.getTargetType()).isEqualTo("BUILDING");
        assertThat(capturedReview.getTargetId()).isEqualTo("100");
        assertThat(capturedReview.getContent()).isEqualTo(maskedText);
        assertThat(capturedReview.getEncryptedContent()).isEqualTo(cipherText);
        assertThat(capturedReview.getRating()).isEqualTo(5);
    }

    @Test
    void testGetReviews_ReturnsPageResponse() {
        // Given
        ReviewResponse responseDto = ReviewResponse.builder()
                .id(1L)
                .userId(2L)
                .targetType("BUILDING")
                .targetId("100")
                .content("Masked content")
                .rating(4)
                .build();

        given(reviewMapper.findByTarget(anyString(), anyString(), anyInt(), anyInt()))
                .willReturn(List.of(responseDto));
        given(reviewMapper.countByTarget(anyString(), anyString()))
                .willReturn(15L);

        // When
        PageResponse<ReviewResponse> result = reviewService.getReviews("BUILDING", "100", 0, 10);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Masked content");
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(15L);
        assertThat(result.isHasNext()).isTrue();
    }

    @Test
    void testGetReviews_HasNextFalse_WhenOnLastPage() {
        // Given
        given(reviewMapper.findByTarget(anyString(), anyString(), anyInt(), anyInt()))
                .willReturn(Collections.emptyList());
        given(reviewMapper.countByTarget(anyString(), anyString()))
                .willReturn(8L);

        // When
        PageResponse<ReviewResponse> result = reviewService.getReviews("BUILDING", "100", 0, 10);

        // Then
        assertThat(result.isHasNext()).isFalse();
    }
}

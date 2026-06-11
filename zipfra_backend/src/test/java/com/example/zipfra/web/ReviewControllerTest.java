package com.example.zipfra.web;

import com.example.zipfra.dto.review.PageResponse;
import com.example.zipfra.dto.review.ReviewRequest;
import com.example.zipfra.dto.review.ReviewResponse;
import com.example.zipfra.security.JwtUtil;
import com.example.zipfra.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ReviewControllerTest {

    static {
        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        } catch (Exception e) {
            // 무시
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtil jwtUtil;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.example.zipfra.service.MapService mapService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private ReviewService reviewService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void testCreateReview_Unauthenticated_Returns401() throws Exception {
        ReviewRequest request = new ReviewRequest();
        request.setTargetType("BUILDING");
        request.setTargetId("100");
        request.setContent("Valid content");
        request.setRating(5);

        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateReview_InvalidRequest_Returns400() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com");
        given(redisTemplate.hasKey("bl:" + token)).willReturn(false);

        ReviewRequest invalidRequest = new ReviewRequest();
        invalidRequest.setTargetType("BUILDING");
        invalidRequest.setContent("");
        invalidRequest.setRating(10); // rating max is 5

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateReview_Success_Returns200() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com");
        given(redisTemplate.hasKey("bl:" + token)).willReturn(false);

        ReviewRequest request = new ReviewRequest();
        request.setTargetType("BUILDING");
        request.setTargetId("100");
        request.setContent("Great place to live!");
        request.setRating(4);

        doNothing().when(reviewService).createReview(eq(1L), any(ReviewRequest.class));

        mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testGetReviews_ReturnsPageResponse() throws Exception {
        ReviewResponse reviewResponse = ReviewResponse.builder()
                .id(1L)
                .userId(2L)
                .targetType("BUILDING")
                .targetId("100")
                .content("Nice masked content")
                .rating(5)
                .build();

        PageResponse<ReviewResponse> pageResponse = PageResponse.<ReviewResponse>builder()
                .content(Collections.singletonList(reviewResponse))
                .page(0)
                .size(10)
                .totalElements(1)
                .hasNext(false)
                .build();

        given(reviewService.getReviews(anyString(), anyString(), anyInt(), anyInt()))
                .willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/reviews")
                        .param("targetType", "BUILDING")
                        .param("targetId", "100")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Nice masked content"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}

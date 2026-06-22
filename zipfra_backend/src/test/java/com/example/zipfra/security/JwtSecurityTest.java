package com.example.zipfra.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@Import(JwtSecurityTest.TestController.class)
public class JwtSecurityTest {

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
    private com.example.zipfra.service.ReviewService reviewService;

    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    private String secretKey;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }



    @RestController
    public static class TestController {
        @PostMapping("/api/v1/location/score")
        public String mockLocationScore() {
            return "ok";
        }
    }


    @Test
    public void T_6_1_Protected_Without_Token_Should_Return_401_TOKEN_MISSING() throws Exception {
        mockMvc.perform(post("/api/v1/location/score")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Api-Version", "1"))
                .andExpect(jsonPath("$.error").value("TOKEN_MISSING"))
                .andExpect(jsonPath("$.message").value("Authorization header is missing or invalid"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void T_6_2_Protected_With_Expired_Token_Should_Return_401_TOKEN_EXPIRED() throws Exception {
        // JwtUtil의 만료 필드를 테스트하기 위해, 만료시간이 음수인 토큰을 직접 생성하는 대신
        // 현재 JwtUtil 구조 상 secretKey를 이용하여 만료 토큰을 수동 생성하여 테스트합니다.
        // 이를 위해 임시 만료 토큰을 수동으로 빌드합니다.
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("1")
                .claim("email", "test@test.com")
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 60000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 30000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(post("/api/v1/location/score")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Api-Version", "1"))
                .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("Access Token has expired"));
    }


    @Test
    public void T_6_3_Protected_With_Invalid_Token_Should_Return_403_TOKEN_INVALID() throws Exception {
        String invalidToken = "invalid.token.signature";

        mockMvc.perform(post("/api/v1/location/score")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Api-Version", "1"))
                .andExpect(jsonPath("$.error").value("TOKEN_INVALID"))
                .andExpect(jsonPath("$.message").value("Signature is invalid or token is malformed"));
    }

    @Test
    public void T_6_4_Public_Markers_Without_Token_Should_Return_200_OK() throws Exception {
        org.mockito.BDDMockito.given(mapService.getMarkers(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(com.example.zipfra.dto.map.MarkerResponse.summary(java.util.Collections.emptyList(), false));

        mockMvc.perform(get("/api/v1/map/markers")
                        .param("bbox", "126.9,37.4,127.1,37.6")
                        .param("zoom", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void T_6_5_Public_Reviews_Without_Token_Should_Return_200_OK() throws Exception {
        org.mockito.BDDMockito.given(reviewService.getReviews(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(com.example.zipfra.dto.review.PageResponse.<com.example.zipfra.dto.review.ReviewResponse>builder()
                        .content(java.util.Collections.emptyList())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .hasNext(false)
                        .build());

        mockMvc.perform(get("/api/v1/reviews")
                        .param("targetType", "BUILDING")
                        .param("targetId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void T_6_6_Post_Reviews_Without_Token_Should_Return_401_TOKEN_MISSING() throws Exception {
        // GET /reviews는 Public이지만 POST /reviews는 Protected 대상인지 확인
        mockMvc.perform(post("/api/v1/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Api-Version", "1"))
                .andExpect(jsonPath("$.error").value("TOKEN_MISSING"));
    }

    @Test
    public void Valid_Token_Should_Allow_Protected_Access() throws Exception {
        String validToken = jwtUtil.generateAccessToken(1L, "test@test.com");
        org.mockito.BDDMockito.given(redisTemplate.hasKey("bl:" + validToken)).willReturn(false);
        org.mockito.BDDMockito.given(redisTemplate.hasKey("rt:1")).willReturn(true);

        mockMvc.perform(post("/api/v1/location/score")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void Protected_With_Blacklisted_Token_Should_Return_401_TOKEN_BLACKLISTED() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com");
        org.mockito.BDDMockito.given(redisTemplate.hasKey("bl:" + token)).willReturn(true);
        org.mockito.BDDMockito.given(redisTemplate.hasKey("rt:1")).willReturn(true);

        mockMvc.perform(post("/api/v1/location/score")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Api-Version", "1"))
                .andExpect(jsonPath("$.error").value("TOKEN_BLACKLISTED"))
                .andExpect(jsonPath("$.message").value("Token has been blacklisted (logged out)"));
    }
}

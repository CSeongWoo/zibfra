package com.example.zipfra.web;

import com.example.zipfra.dto.favorite.FavoriteDto;
import com.example.zipfra.security.JwtUtil;
import com.example.zipfra.service.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class FavoriteControllerTest {

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

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private FavoriteService favoriteService;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void testToggleFavorite_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/favorites/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testToggleFavorite_Success_ReturnsIsFavoriteTrue() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com");
        given(redisTemplate.hasKey("bl:" + token)).willReturn(false);
        given(favoriteService.toggleFavorite(1L, 100L)).willReturn(true);

        mockMvc.perform(post("/api/v1/favorites/100")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(true));
    }

    @Test
    void testToggleFavorite_Success_ReturnsIsFavoriteFalse() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com");
        given(redisTemplate.hasKey("bl:" + token)).willReturn(false);
        given(favoriteService.toggleFavorite(1L, 100L)).willReturn(false);

        mockMvc.perform(post("/api/v1/favorites/100")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFavorite").value(false));
    }

    @Test
    void testToggleFavorite_Conflict_Returns409() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com");
        given(redisTemplate.hasKey("bl:" + token)).willReturn(false);
        given(favoriteService.toggleFavorite(1L, 100L)).willThrow(new DataIntegrityViolationException("Conflict"));

        mockMvc.perform(post("/api/v1/favorites/100")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void testGetFavorites_Success_ReturnsList() throws Exception {
        String token = jwtUtil.generateAccessToken(1L, "test@test.com");
        given(redisTemplate.hasKey("bl:" + token)).willReturn(false);

        FavoriteDto dto = FavoriteDto.builder()
                .id(10L)
                .propertyId(200L)
                .build();
        given(favoriteService.getFavoritesByUserId(1L)).willReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].propertyId").value(200L));
    }
}

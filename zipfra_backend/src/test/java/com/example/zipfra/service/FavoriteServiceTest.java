package com.example.zipfra.service;

import com.example.zipfra.dto.favorite.FavoriteDto;
import com.example.zipfra.mapper.mysql.FavoriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteMapper favoriteMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void testToggleFavorite_WhenNotExists_InsertsAndReturnsTrue() {
        // Given
        Long userId = 1L;
        Long propertyId = 100L;
        given(favoriteMapper.existsByUserIdAndPropertyId(userId, propertyId)).willReturn(false);

        // When
        boolean result = favoriteService.toggleFavorite(userId, propertyId);

        // Then
        assertThat(result).isTrue();
        verify(favoriteMapper).insert(userId, propertyId);
    }

    @Test
    void testToggleFavorite_WhenExists_DeletesAndReturnsFalse() {
        // Given
        Long userId = 1L;
        Long propertyId = 100L;
        given(favoriteMapper.existsByUserIdAndPropertyId(userId, propertyId)).willReturn(true);

        // When
        boolean result = favoriteService.toggleFavorite(userId, propertyId);

        // Then
        assertThat(result).isFalse();
        verify(favoriteMapper).delete(userId, propertyId);
    }

    @Test
    void testGetFavoritesByUserId() {
        // Given
        Long userId = 1L;
        FavoriteDto dto = FavoriteDto.builder()
                .id(10L)
                .propertyId(200L)
                .build();
        given(favoriteMapper.findByUserId(userId)).willReturn(List.of(dto));

        // When
        List<FavoriteDto> result = favoriteService.getFavoritesByUserId(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPropertyId()).isEqualTo(200L);
    }
}

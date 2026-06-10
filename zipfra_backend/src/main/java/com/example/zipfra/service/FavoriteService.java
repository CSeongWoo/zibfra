package com.example.zipfra.service;

import com.example.zipfra.dto.favorite.FavoriteDto;
import com.example.zipfra.mapper.mysql.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    @Transactional("primaryTransactionManager")
    public boolean toggleFavorite(Long userId, Long propertyId) {
        boolean exists = favoriteMapper.existsByUserIdAndPropertyId(userId, propertyId);
        if (exists) {
            favoriteMapper.delete(userId, propertyId);
            return false;
        } else {
            favoriteMapper.insert(userId, propertyId);
            return true;
        }
    }

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public List<FavoriteDto> getFavoritesByUserId(Long userId) {
        return favoriteMapper.findByUserId(userId);
    }
}

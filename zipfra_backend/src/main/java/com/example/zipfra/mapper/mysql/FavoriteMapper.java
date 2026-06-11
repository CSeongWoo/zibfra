package com.example.zipfra.mapper.mysql;

import com.example.zipfra.dto.favorite.FavoriteDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {
    boolean existsByUserIdAndPropertyId(@Param("userId") Long userId, @Param("propertyId") Long propertyId);
    void insert(@Param("userId") Long userId, @Param("propertyId") Long propertyId);
    void delete(@Param("userId") Long userId, @Param("propertyId") Long propertyId);
    List<FavoriteDto> findByUserId(@Param("userId") Long userId);
}

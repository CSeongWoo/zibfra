package com.example.zipfra.mapper.mysql;

import com.example.zipfra.domain.Review;
import com.example.zipfra.dto.review.ReviewResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    void insert(Review review);
    List<ReviewResponse> findByTarget(@Param("targetType") String targetType,
                                      @Param("targetId") String targetId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);
    long countByTarget(@Param("targetType") String targetType,
                       @Param("targetId") String targetId);
}

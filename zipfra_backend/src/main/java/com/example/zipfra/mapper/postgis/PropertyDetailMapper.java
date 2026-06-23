package com.example.zipfra.mapper.postgis;

import com.example.zipfra.dto.ai.PropertyDetailDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI-01 getPropertyDetail Tool 전용 매퍼 (AGENTS.md §6.4.1, PostGIS spatialDataSource).
 *
 * <p>real_estate_sales에서 propertyId 기반으로 거래 데이터를 조회한다.
 * 좌표는 ST_X/ST_Y로 double 분리(§4 좌표 매핑 표준).
 */
@Mapper
public interface PropertyDetailMapper {

    /**
     * 매물 상세 정보 조회 (AI Tool용).
     *
     * @param propertyId real_estate_sales.id
     * @return 매물 상세, 없으면 null → PROPERTY_NOT_FOUND 예외
     */
    PropertyDetailDTO findById(@Param("propertyId") Long propertyId);
}

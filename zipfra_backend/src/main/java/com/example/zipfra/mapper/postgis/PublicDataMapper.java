package com.example.zipfra.mapper.postgis;

import com.example.zipfra.dto.map.MarkerDTO;
import com.example.zipfra.dto.map.PoiMarkerDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PUB-01 공공데이터 배치 적재본 조회 (§8.1, PostGIS 읽기).
 * REAL_ESTATE=real_estate_sales(lawd_cd 필터), COMMERCE=poi 상업 카테고리.
 */
@Mapper
public interface PublicDataMapper {

    long countRealEstateByLawd(@Param("lawdCd") String lawdCd);

    List<MarkerDTO> findRealEstateByLawd(@Param("lawdCd") String lawdCd,
                                         @Param("size") int size,
                                         @Param("offset") long offset);

    long countCommerce();

    List<PoiMarkerDTO> findCommerce(@Param("size") int size,
                                    @Param("offset") long offset);
}

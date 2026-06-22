package com.example.zipfra.mapper.postgis;

import com.example.zipfra.dto.location.PoiDistanceDTO;
import com.example.zipfra.dto.map.Bbox;
import com.example.zipfra.dto.map.PoiMarkerDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * LOC-01 입지 점수용 PostGIS 매퍼 (AGENTS.md §8.3 postgis.PoiMapper).
 * 미터 반경은 ::geography 캐스팅(§4). 매핑은 spatialSqlSessionFactory.
 */
@Mapper
public interface PoiMapper {

    /** 반경(m) 내 POI 를 카테고리·거리(m)와 함께 조회. */
    List<PoiDistanceDTO> findWithin(@Param("lon") double lon,
                                    @Param("lat") double lat,
                                    @Param("radiusMeters") int radiusMeters);

    /** MAP-02: bbox 내 + 카테고리(대문자 enum 이름) 매칭 POI 조회(오버레이용). */
    List<PoiMarkerDTO> findInBbox(@Param("bbox") Bbox bbox,
                                  @Param("categories") List<String> categories);
}

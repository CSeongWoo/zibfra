package com.example.zipfra.mapper.postgis;

import java.util.List;

import com.example.zipfra.dto.map.Bbox;
import com.example.zipfra.dto.map.MarkerDTO;
import com.example.zipfra.dto.map.MarkerFilter;
import com.example.zipfra.dto.map.PropertySearchDTO;
import com.example.zipfra.dto.map.RegionSummaryDTO;
import org.apache.ibatis.annotations.Param;

/**
 * MAP-01 공간 조회 매퍼 (PostGIS, spatialSqlSessionFactory).
 * bbox 필터는 geometry GiST 인덱스를 타도록 {@code geom && ST_MakeEnvelope(...,4326)} 사용(§4).
 */
public interface MarkerMapper {

    /** DETAIL: bbox + 검색 필터 매칭 매물 총 개수(페이지네이션 totalCount 용). */
    long countMarkers(@Param("bbox") Bbox bbox, @Param("filter") MarkerFilter filter);

    /** DETAIL: bbox + 검색 필터 매칭 매물 마커 페이지. */
    List<MarkerDTO> findMarkers(@Param("bbox") Bbox bbox,
                                @Param("filter") MarkerFilter filter,
                                @Param("size") int size,
                                @Param("offset") long offset);

    /** SUMMARY: bbox 내 시·군·구 사전 집계. */
    List<RegionSummaryDTO> findRegionSummaries(@Param("bbox") Bbox bbox);

    /** MAP-03: 단지명 부분일치 검색(단지 단위 집계, dealCount 내림차순). */
    List<PropertySearchDTO> searchProperties(@Param("q") String q, @Param("limit") int limit);
}

package com.example.zipfra.mapper.postgis;

import java.util.List;

import com.example.zipfra.dto.ingest.GridCell;
import com.example.zipfra.dto.ingest.RealEstateRow;
import com.example.zipfra.dto.ingest.RegionAgg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 실거래가 적재 매퍼 (§9, PostGIS 쓰기). 공공데이터 적재본은 PostGIS 전용(MySQL 경유 안 함).
 * 재적재 멱등성: 동일 (lawd_cd, deal_ym, SALE/APT) 삭제 후 insert.
 */
@Mapper
public interface IngestionMapper {

    /** 동일 지역·월·종류·거래유형 적재분 삭제(재적재 멱등). */
    int deleteRealEstate(@Param("lawdCd") String lawdCd, @Param("dealYm") String dealYm,
                         @Param("dealType") String dealType, @Param("propertyType") String propertyType);

    /** 1행 insert (geom 은 ST_MakePoint 로 생성). */
    int insertRealEstate(RealEstateRow row);

    /** POI 전체 삭제(더미/기존 제거 후 실데이터 재적재). */
    int clearPoi();

    /** POI 1건 insert (category=대문자 enum 이름). */
    int insertPoi(@Param("category") String category,
                  @Param("name") String name,
                  @Param("lng") double lng,
                  @Param("lat") double lat);

    /** 특정 카테고리 POI 만 삭제(버스정류장 등 부분 재적재 멱등 — clearPoi 와 달리 다른 카테고리는 보존). */
    int deletePoiByCategory(@Param("category") String category);

    /** region_summary 전체 삭제(실집계 재생성). */
    int clearRegionSummary();

    /** real_estate_sales 를 시군구(lawd_cd)별 집계(SALE 기준). */
    List<RegionAgg> aggregateRealEstateByLawd();

    /** 매물이 존재하는 0.02도 격자의 좌하단 좌표 목록(전국 POI 격자 적재 대상, §9). */
    List<GridCell> findPropertyGridCells();

    /** region_summary 1행 insert (geom 은 중심 좌표). */
    int insertRegionSummary(@Param("regionCd") String regionCd,
                            @Param("regionName") String regionName,
                            @Param("centerLon") double centerLon,
                            @Param("centerLat") double centerLat,
                            @Param("dealCount") int dealCount,
                            @Param("avgAmount") Long avgAmount,
                            @Param("maxAmount") Long maxAmount);

    /** 추이: 동일 지역·월 적재분 삭제(재적재 멱등). */
    int deletePriceHistory(@Param("lawdCd") String lawdCd, @Param("dealYm") String dealYm);

    /** 추이: 단지·거래유형·월별 평균가 1행 upsert. */
    int upsertPriceHistory(@Param("buildingName") String buildingName,
                           @Param("lawdCd") String lawdCd,
                           @Param("dealType") String dealType,
                           @Param("dealYm") String dealYm,
                           @Param("avgAmount") long avgAmount,
                           @Param("dealCount") int dealCount);
}

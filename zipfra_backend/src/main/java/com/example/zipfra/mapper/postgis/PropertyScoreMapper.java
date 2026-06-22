package com.example.zipfra.mapper.postgis;

import java.util.List;

import com.example.zipfra.dto.score.PropertyGeom;
import org.apache.ibatis.annotations.Param;

/**
 * 매물별 점수 사전계산(§5.1) 매퍼 (PostGIS, spatialSqlSessionFactory).
 * 배치(ApplicationRunner)가 채우고, MAP-01 은 MarkerMapper 의 LEFT JOIN 으로 읽는다.
 */
public interface PropertyScoreMapper {

    /** 배치 skip 판단용 — 적재된 점수 행 수. */
    long count();

    /** 배치 입력 — 전체 매물 id + 좌표(lon/lat). */
    List<PropertyGeom> findAllPropertiesForScoring();

    /** 매물별 4그룹 base upsert(property_id 충돌 시 갱신). */
    void upsert(@Param("propertyId") long propertyId,
                @Param("transitBase") double transitBase,
                @Param("educationBase") double educationBase,
                @Param("commerceBase") double commerceBase,
                @Param("convenienceBase") double convenienceBase);
}

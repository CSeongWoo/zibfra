/**
 * PostGIS Mapper 인터페이스 패키지.
 * spatialSqlSessionFactory(@MapperScan)가 이 패키지를 스캔한다.
 * 실제 Mapper 인터페이스는 기능 구현(MAP-01, LOC-01 등) 시 추가한다.
 *
 * AGENTS.md §4 주의:
 * - ST_DWithin 사용 시 ::geography 캐스팅 필수 (미터 단위)
 * - bbox 필터는 geometry && ST_MakeEnvelope(..., 4326)
 * - PGobject 직접 매핑 금지 → ST_X(geom) AS lon, ST_Y(geom) AS lat 분리
 */
package com.example.zipfra.mapper.postgis;

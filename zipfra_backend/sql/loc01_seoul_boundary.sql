-- LOC-01 ENV_PENALTY 서울 판정용 행정경계 테이블 (AGENTS.md §5)
-- 서울특별시 경계 MultiPolygon 1행을 두고 ST_Contains 로 좌표의 서울(법정동 11*) 여부를 판정한다.
-- spatialDataSource(PostGIS)에서 실행.

CREATE TABLE IF NOT EXISTS seoul_boundary (
    geom geometry(MultiPolygon, 4326) NOT NULL
);

CREATE INDEX IF NOT EXISTS gist_seoul_geom ON seoul_boundary USING GIST (geom);

-- 시드
--   운영: 서울시 행정경계 GeoJSON(법정동 11* 합집합)을 MultiPolygon 1행으로 적재한다.
--   개발/통합테스트: 아래 단순화 사각형으로 대체(서울 대략 경계 lng 126.76~127.18, lat 37.41~37.70).
--   ST_MakeEnvelope 는 POLYGON 을 반환하므로 ST_Multi 로 컬럼 typmod(MultiPolygon)에 맞춘다.
INSERT INTO seoul_boundary (geom)
SELECT ST_Multi(ST_SetSRID(ST_MakeEnvelope(126.76, 37.41, 127.18, 37.70, 4326), 4326))
WHERE NOT EXISTS (SELECT 1 FROM seoul_boundary);

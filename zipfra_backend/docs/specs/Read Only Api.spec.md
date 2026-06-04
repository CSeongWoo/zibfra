# Zipfra Read-Only API — 설계안 (spec draft)

> **상태: DRAFT — 사람 승인 전 구현·마이그레이션 착수 금지 (AGENT.md §1.2)**
> 
> **강제 장치 미설치 고지**: `docs/specs/` 디렉터리가 비어 있고 pre-commit hook / CI spec-lint가 아직 구성되지 않았습니다. 승인 시 hook 셋업을 먼저 진행할지 확인 바랍니다.

---

## 범위

- **포함**: 지도 마커 조회(읽기), 입지 점수 분석(계산), 공공데이터 프록시(중계), 인가 흐름(JWT 검증).
- **제외**: 회원 가입/수정, 리뷰 쓰기, 즐겨찾기 추가 등 쓰기 API → 별도 설계안으로 분리.
- **DB 라우팅 원칙**: 모든 엔드포인트는 **읽기 전용**. 쓰기 라우팅 없음.
  - 공간 조회 → **PostGIS (읽기)**
  - 사용자 조회(JWT 검증 시 회원 확인) → **MySQL (읽기)**

---

## §2.1 기술 스택 및 아키텍처 제약

### 확정 스택 (AGENT.md §3 준수, 신규 의존성 추가 금지)

| 레이어 | 기술 | 비고 |
|--------|------|------|
| 언어·런타임 | Java 21 / Spring Boot 4.0.6 | `build.gradle` 확정값 |
| ORM (일반 CRUD) | MyBatis 3.0.3 (mybatis-spring-boot-starter) | 기본 필수 |
| 공간 쿼리 | MyBatis **Native SQL** | JPA/Querydsl 공간 연산 금지 |
| 보안 | Spring Security + JJWT 0.11.5 | 확정값 |
| API 문서 | springdoc-openapi 3.0.2 (Swagger UI) | 확정값 |
| MySQL | MySQL 8.x | 사용자 읽기 |
| PostGIS | PostgreSQL 15.x / PostGIS 3.x | 공간 읽기 |

### 멀티 데이터소스 구성

```
[요청] → Spring Security (JWT 필터)
           ↓
        Controller
           ↓
        Service
       ┌───┴───┐
  MySQL DS    PostGIS DS
(사용자 읽기) (공간·집계 읽기)
```

**두 개의 `SqlSessionFactory` / `DataSource` Bean으로 분리:**

```
- primaryDataSource     → MySQL 8.x (사용자 조회용, 읽기 전용 커넥션 풀)
  primarySqlSessionFactory
  primaryTransactionManager  (read-only, propagation=SUPPORTS)

- spatialDataSource     → PostGIS (공간·집계 조회, 읽기 전용)
  spatialSqlSessionFactory
  spatialTransactionManager  (read-only, propagation=SUPPORTS)
```

- **이중 쓰기(double-write) 금지** — 본 설계안에는 쓰기 트랜잭션 없음.
- 두 `TransactionManager`가 동시에 열리는 패턴은 없음(각 서비스 메서드가 하나만 사용).
- 동기화(MySQL → PostGIS)는 §6 심야 배치 전담, 본 API와 무관.

### MyBatis Mapper 패키지 분리

```
com.example.zipfra.mapper.mysql.*    → @Mapper + primarySqlSessionFactory
com.example.zipfra.mapper.postgis.*  → @Mapper + spatialSqlSessionFactory
```

---

## §2.2 DB 스키마 및 데이터 흐름

### 엔드포인트별 DB 라우팅

| 엔드포인트 | DB | Mapper 패키지 | 쿼리 성격 |
|------------|-----|--------------|----------|
| `GET /api/v1/map/markers` | PostGIS | `postgis.MarkerMapper` | bbox 공간 필터 + 줌 분기 |
| `POST /api/v1/location/score` | PostGIS | `postgis.PoiMapper` | ST_DWithin 반경 조회 |
| `GET /api/v1/proxy/public-data` | PostGIS (배치 적재본) | `postgis.PublicDataMapper` | 사전 적재 데이터 조회 |
| `GET /api/v1/proxy/public-data/realtime` | 외부 API 중계 | — (HTTP 프록시) | 배치 미반영분 단건 |
| JWT 검증 내부 (사용자 조회) | MySQL | `mysql.UserMapper` | id/email 단건 조회 |

### 공간 쿼리 단위 규칙 (AGENT.md §3.2 준수)

**원칙: `ST_DWithin` + GiST 인덱스 + `::geography` 캐스팅 (권장)**

```sql
-- GiST 인덱스 (마이그레이션 시 적용)
CREATE INDEX idx_poi_geom ON poi USING GIST (geom);
CREATE INDEX idx_real_estate_geom ON real_estate_sales USING GIST (location);

-- 반경 N미터 검색 (미터 단위 보장)
SELECT id, name, category, ST_X(geom) AS lon, ST_Y(geom) AS lat
FROM poi
WHERE ST_DWithin(
    geom::geography,
    ST_MakePoint(:lon, :lat)::geography,
    :radiusMeters        -- 미터 단위, degree 아님
);

-- ST_Distance 단독 사용 금지 (인덱스 미사용 + degree 혼동 위험)
```

**SRID**: 저장 시 EPSG:4326(`geometry`). 거리 연산 시 `::geography` 캐스팅으로 미터 단위 전환.

### MyBatis resultMap 예시 (좌표 컬럼 처리)

```xml
<resultMap id="poiResultMap" type="com.example.zipfra.dto.postgis.PoiDto">
  <id     property="id"       column="id"/>
  <result property="name"     column="name"/>
  <result property="category" column="category"/>
  <result property="lon"      column="lon"/>   <!-- ST_X(geom) alias -->
  <result property="lat"      column="lat"/>   <!-- ST_Y(geom) alias -->
  <result property="distanceM" column="distance_m"/>
</resultMap>

<select id="findPoisWithin" resultMap="poiResultMap">
  SELECT id, name, category,
         ST_X(geom) AS lon, ST_Y(geom) AS lat,
         ST_Distance(geom::geography,
                     ST_MakePoint(#{lon}, #{lat})::geography) AS distance_m
  FROM poi
  WHERE ST_DWithin(geom::geography,
                   ST_MakePoint(#{lon}, #{lat})::geography,
                   #{radiusMeters})
  ORDER BY distance_m
</select>
```

> `geometry` 컬럼을 JDBC로 직접 읽으면 `PGobject`가 반환됨. `ST_X`/`ST_Y`로 double 분리해 DTO에 매핑하는 방식을 표준으로 채택.

### 배치 적재 경계 명시 (§6 연계)

| 데이터 유형 | 프록시 용도 | 조회 원천 |
|------------|------------|----------|
| 실거래가 (대용량) | 심야 배치 적재본 우선 | PostGIS `real_estate_sales` |
| 상권 POI (대용량) | 심야 배치 적재본 우선 | PostGIS `poi` |
| 배치 미반영분 (최근 1일 이내 등) | 실시간 단건 프록시 | 외부 API 직접 중계 |

---

## §2.3 핵심 비즈니스 로직 및 알고리즘

### A. 지도 마커 API — 줌 분기 로직

#### 줌 임계값 (서버 권위, 확정값)

| 줌 레벨 | 판정 | 전략 |
|---------|------|------|
| ≤ 13 | **줌아웃** | 시/군/구 사전 집계 요약 테이블 조회 |
| 14 | **전환점** (경계) | 줌아웃 전략 유지 (요약) |
| ≥ 15 | **줌인** | 개별 마커 조회 (bbox + 페이지네이션) |

> **카카오 맵 기준**: 레벨 1(최대 줌인) ~ 14(전국). 본 spec은 카카오 맵 레벨 기준으로 `level ≤ 3` = 줌인(동 단위), `level ≥ 4` = 줌아웃(시/군/구)으로 재정의 가능. **최종 수치는 프론트 팀과 협의 후 확정 필요.**

**서버 권위 원칙**: 클라이언트가 보낸 줌값은 `int`로 수신하되, 서버가 임계값 테이블로 재판정. 클라 값을 그대로 신뢰하지 않음.

```java
// ZoomStrategy.java
public enum ZoomStrategy {
    DETAIL,   // zoom >= ZOOM_THRESHOLD_IN
    SUMMARY;  // zoom <= ZOOM_THRESHOLD_OUT

    static final int ZOOM_THRESHOLD_IN  = 15;  // 이 값 이상: 상세 마커
    static final int ZOOM_THRESHOLD_OUT = 14;  // 이 값 이하: 요약

    public static ZoomStrategy from(int clientZoom) {
        return clientZoom >= ZOOM_THRESHOLD_IN ? DETAIL : SUMMARY;
    }
}
```

#### Bounding Box 포맷 확정

- **파라미터 순서**: `minLng,minLat,maxLng,maxLat` (쉼표 구분 단일 문자열, 또는 개별 파라미터)
- **SRID**: 4326 (WGS84) 고정
- **검증 규칙**:
  - `minLng ∈ [-180, 180]`, `maxLng ∈ [-180, 180]`, `minLat ∈ [-90, 90]`, `maxLat ∈ [-90, 90]`
  - `minLng < maxLng`, `minLat < maxLat`
  - **lat/lng 뒤바뀜 거부**: `minLat > 90` 또는 `minLng > 90`(한국 위도 범위 33~39) → 400 + `"error": "BBOX_COORD_SWAPPED"`

#### 거대 bbox 가드

- 대각 거리 상한: **150km** (ST_Distance로 서버 계산)
- 초과 시: **줌아웃 전략 강제** (거부 대신 요약 응답으로 다운그레이드) + `Warning: bbox-oversized` 응답 헤더
- 줌인(DETAIL) 요청이면 400 거부: `"error": "BBOX_TOO_LARGE_FOR_DETAIL"`

#### 줌인 폭발 방지 (페이지네이션)

- 줌인 시 마커 개수 **cap: 200개/페이지**
- 페이지네이션: `page` (0-based) + `size` (max 200) 쿼리 파라미터
- 응답에 `totalCount`, `hasNext` 포함

---

### B. 입지 점수 알고리즘 (AGENT.md §4 정합)

#### 거리 감쇠 함수

```
W(t) = 1             (0 ≤ t ≤ 5분)
W(t) = 1 / (t/5)²   (t > 5분)
```

도보 속도 가정: **분당 80m** (4.8 km/h). 따라서 거리 d(m) → t = d / 80 분.

```java
double travelTimeMin = distanceM / 80.0;
double weight = (travelTimeMin <= 5.0)
    ? 1.0
    : 1.0 / Math.pow(travelTimeMin / 5.0, 2);
```

#### 카테고리 분기 산출

| 카테고리 | 산출 방식 | 설명 |
|----------|----------|------|
| **필수 공급** (약국·마트·은행) | `W(min_travel_time)` | 가장 가까운 단 1개만, 중복 가산 금지 |
| **미식·여가** (식당·카페·영화관) | `Σ W(travel_time_poi)` | 반경 내 전체 합산 |

```
final_score = Σ_category [ base_score[category] × user_weight[category] ]

where:
  base_score(필수)  = W( min(travel_time_i) for i in category )
  base_score(여가)  = Σ_i W(travel_time_i) for i in category
  user_weight[c] ∈ [0.0, 1.0]
```

#### 반경 설정

- 기본 반경: **1,500m** (도보 약 19분)
- 클라이언트가 `radiusMeters` 파라미터로 오버라이드 가능 (최대 3,000m)

#### 연산 위치

1. PostGIS: `ST_DWithin(::geography)` → 반경 내 POI 추출 + 거리(m) 반환
2. 백엔드 메모리: 감쇠 계산 + 카테고리 분기 + 가중치 적용 + 합산

---

## §2.4 API 인터페이스 및 프론트엔드 시각화

### 인증 체계

| 구분 | 기준 | 적용 엔드포인트 |
|------|------|----------------|
| **Public** | JWT 불필요 | `GET /api/v1/map/markers` |
| **Protected** | `Authorization: Bearer <token>` 필수 | `POST /api/v1/location/score`, `GET /api/v1/proxy/**` |

**Protected 실패 응답:**
- 토큰 누락: `401 Unauthorized` + `{"error": "TOKEN_MISSING"}`
- 토큰 만료: `401 Unauthorized` + `{"error": "TOKEN_EXPIRED"}`
- 토큰 위변조: `403 Forbidden` + `{"error": "TOKEN_INVALID"}`

**JWT 발급·갱신 흐름** (쓰기 API 설계안에서 상세 정의, 여기선 검증 계약만):
- Access Token: 만료 15분
- Refresh Token: 만료 7일 (MySQL `refresh_tokens` 테이블 조회, MySQL 읽기 라우팅)
- 재발급: `POST /api/v1/auth/refresh` (별도 설계안)

---

### 엔드포인트 1: 지도 마커 조회

```
GET /api/v1/map/markers
인가: Public
DB: PostGIS (읽기)
캐싱: 줌아웃(SUMMARY)에만 ETag + Cache-Control
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `bbox` | String | ✅ | `minLng,minLat,maxLng,maxLat` (EPSG:4326) |
| `zoom` | int | ✅ | 클라이언트 줌 레벨 (서버가 재판정) |
| `page` | int | — | 줌인 전용, 0-based (default: 0) |
| `size` | int | — | 줌인 전용, max 200 (default: 100) |

#### 응답 — 줌인(DETAIL) `200 OK`

```json
{
  "strategy": "DETAIL",
  "zoom": 15,
  "bbox": { "minLng": 126.9, "minLat": 37.4, "maxLng": 127.1, "maxLat": 37.6 },
  "page": 0,
  "size": 100,
  "totalCount": 342,
  "hasNext": true,
  "markers": [
    {
      "id": 10023,
      "lon": 127.023,
      "lat": 37.512,
      "dealAmount": 85000,
      "buildingName": "래미안퍼스티지",
      "dongName": "서초동"
    }
  ]
}
```

**응답 헤더 (DETAIL)**:
```
Cache-Control: no-store
```

#### 응답 — 줌아웃(SUMMARY) `200 OK`

```json
{
  "strategy": "SUMMARY",
  "zoom": 12,
  "bbox": { "minLng": 126.7, "minLat": 37.3, "maxLng": 127.3, "maxLat": 37.7 },
  "summaries": [
    {
      "regionCode": "11650",
      "regionName": "서초구",
      "centerLon": 127.032,
      "centerLat": 37.484,
      "count": 1842,
      "avgDealAmount": 124000,
      "maxDealAmount": 380000
    }
  ]
}
```

**응답 헤더 (SUMMARY)**:
```
ETag: "v2-bbox-126.7-37.3-127.3-37.7-zoom12"
Cache-Control: public, max-age=300, stale-while-revalidate=60
Vary: Accept-Encoding
```

- **TTL**: 5분 (`max-age=300`). 요약 데이터는 심야 배치 갱신이므로 단기 캐싱 허용.
- ETag 구성: `"v{배치버전}-bbox-{minLng}-{minLat}-{maxLng}-{maxLat}-zoom{level}"`. 동일 bbox + zoom이면 304 반환.

#### 에러 응답

| 상황 | HTTP | body.error |
|------|------|-----------|
| bbox 파싱 실패 | 400 | `BBOX_PARSE_ERROR` |
| lat/lng 뒤바뀜 의심 | 400 | `BBOX_COORD_SWAPPED` |
| minLng ≥ maxLng 또는 minLat ≥ maxLat | 400 | `BBOX_INVALID_RANGE` |
| 줌인 + bbox 대각 > 150km | 400 | `BBOX_TOO_LARGE_FOR_DETAIL` |
| zoom 범위 외 (< 1 or > 21) | 400 | `ZOOM_OUT_OF_RANGE` |
| size > 200 | 400 | `PAGE_SIZE_EXCEEDED` |

---

### 엔드포인트 2: 동적 입지 점수 분석

```
POST /api/v1/location/score
인가: Protected (Bearer)
DB: PostGIS (읽기) → 백엔드 메모리 계산
캐싱: 없음 (POST + 동적 가중치, 캐싱 불가 트레이드오프 수용)
```

> **GET+Body 금지 이유**: 다수 프록시·클라이언트(CDN, 일부 HTTP 라이브러리)가 GET 요청의 body를 무시하거나 탈락시킴. 계산 시맨틱이므로 POST 확정.
>
> **캐싱 트레이드오프**: POST이므로 HTTP 표준 캐시 불가. 동일 좌표+가중치 반복 요청이 문제가 되면 서버 사이드 캐시(Redis 등)를 추후 별도 spec으로 추가. 현재는 신규 의존성 추가 금지 원칙에 따라 캐싱 없이 설계.

#### 요청 Body (application/json)

```json
{
  "lon": 127.023,
  "lat": 37.512,
  "radiusMeters": 1500,
  "weights": {
    "pharmacy":    0.8,
    "mart":        0.6,
    "bank":        0.4,
    "restaurant":  0.9,
    "cafe":        0.7,
    "cinema":      0.3
  }
}
```

**유효성 제약**:
- `lon ∈ [-180, 180]`, `lat ∈ [-90, 90]`
- `radiusMeters ∈ [100, 3000]` (default 1500, 생략 가능)
- `weights` 각 값 `∈ [0.0, 1.0]` (생략된 카테고리는 0.0으로 처리)

#### 응답 `200 OK`

```json
{
  "lon": 127.023,
  "lat": 37.512,
  "radiusMeters": 1500,
  "finalScore": 3.42,
  "breakdown": {
    "essential": {
      "pharmacy": {
        "model": "ONE_IS_ENOUGH",
        "nearestDistanceM": 320,
        "travelTimeMin": 4.0,
        "baseScore": 1.0,
        "userWeight": 0.8,
        "contribution": 0.8
      },
      "mart": {
        "model": "ONE_IS_ENOUGH",
        "nearestDistanceM": 850,
        "travelTimeMin": 10.6,
        "baseScore": 0.222,
        "userWeight": 0.6,
        "contribution": 0.133
      },
      "bank": {
        "model": "ONE_IS_ENOUGH",
        "nearestDistanceM": 120,
        "travelTimeMin": 1.5,
        "baseScore": 1.0,
        "userWeight": 0.4,
        "contribution": 0.4
      }
    },
    "leisure": {
      "restaurant": {
        "model": "MORE_IS_BETTER",
        "poiCount": 48,
        "aggregatedBaseScore": 18.4,
        "userWeight": 0.9,
        "contribution": 16.56
      },
      "cafe": {
        "model": "MORE_IS_BETTER",
        "poiCount": 22,
        "aggregatedBaseScore": 8.1,
        "userWeight": 0.7,
        "contribution": 5.67
      },
      "cinema": {
        "model": "MORE_IS_BETTER",
        "poiCount": 1,
        "aggregatedBaseScore": 0.444,
        "userWeight": 0.3,
        "contribution": 0.133
      }
    }
  }
}
```

**산식 정합 확인**:
- `contribution = baseScore × userWeight`
- 필수(ONE_IS_ENOUGH): `baseScore = W(nearestDistanceM / 80)`
- 여가(MORE_IS_BETTER): `baseScore = Σ W(distanceM / 80)` for all poi in radius
- `finalScore = Σ contribution` (모든 카테고리)

#### 에러 응답

| 상황 | HTTP | body.error |
|------|------|-----------|
| lon/lat 범위 오류 | 400 | `COORD_OUT_OF_RANGE` |
| radiusMeters 범위 오류 | 400 | `RADIUS_OUT_OF_RANGE` |
| weights 값 범위 오류 | 400 | `WEIGHT_OUT_OF_RANGE` |
| JWT 누락 | 401 | `TOKEN_MISSING` |
| JWT 만료 | 401 | `TOKEN_EXPIRED` |
| JWT 위변조 | 403 | `TOKEN_INVALID` |

---

### 엔드포인트 3a: 공공데이터 프록시 (배치 적재본 조회)

```
GET /api/v1/proxy/public-data
인가: Protected (Bearer)
DB: PostGIS (배치 적재본, 읽기)
용도: 심야 배치로 이미 적재된 대용량 데이터 조회
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `type` | String | ✅ | `REAL_ESTATE` \| `COMMERCE` |
| `regionCode` | String | — | 법정동코드 (10자리) |
| `page` | int | — | 0-based (default 0) |
| `size` | int | — | max 100 (default 20) |

#### 응답 `200 OK`

```json
{
  "type": "REAL_ESTATE",
  "regionCode": "1165010100",
  "page": 0,
  "size": 20,
  "totalCount": 2341,
  "hasNext": true,
  "data": [ { "...": "..." } ]
}
```

---

### 엔드포인트 3b: 공공데이터 프록시 (실시간 단건 중계)

```
GET /api/v1/proxy/public-data/realtime
인가: Protected (Bearer)
DB: 없음 (외부 API 직접 중계)
용도: 배치 미반영분(최근 1일 이내 등) 실시간 단건 조회만 허용
```

> **사용 조건 명시**: 이 엔드포인트는 심야 배치가 아직 반영하지 못한 **당일 발생 데이터**에 대한 단건 보완 조회 전용. 대량 수집에 사용 금지(Rate Limit + 응답 지연 위험).

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `source` | String | ✅ | `MOLIT_APT` \| `MOLIT_ROW` \| `COMMERCE` |
| `dealYear` | int | ✅ | 거래 연도 |
| `dealMonth` | int | ✅ | 거래 월 |
| `lawd_cd` | String | ✅ | 법정동코드 5자리 |

#### 응답 `200 OK`

외부 API 원본 JSON 그대로 반환 (인증키 제거 후).

#### 에러 응답

| 상황 | HTTP | body | 추가 헤더 |
|------|------|------|----------|
| 외부 API 타임아웃 (> 5초) | 504 | `{"error":"UPSTREAM_TIMEOUT"}` | — |
| 외부 API Rate Limit | 503 | `{"error":"UPSTREAM_RATE_LIMIT"}` | `Retry-After: 60` |
| 외부 API 4xx | 502 | `{"error":"UPSTREAM_CLIENT_ERROR","detail":"..."}` | — |

**retry 정책**: 외부 API 호출 시 최대 2회 재시도 (1초 간격). 3회 모두 실패 시 504/503 반환. (§6 배치 retry 정책과 동일 원칙 적용)

---

## §2.5 테스트 기준

> AGENT.md §1 3단계: spec의 수식·제약을 검증하는 테스트 작성 기준.

### T-1: Bounding Box 좌표 검증

```
[PASS] bbox = "126.9,37.4,127.1,37.6" → 200
[FAIL] bbox = "37.4,126.9,37.6,127.1" (lat/lng 뒤바뀜) → 400 BBOX_COORD_SWAPPED
[FAIL] bbox = "127.1,37.4,126.9,37.6" (minLng > maxLng) → 400 BBOX_INVALID_RANGE
[FAIL] bbox = "abc,37.4,127.1,37.6"   (파싱 오류) → 400 BBOX_PARSE_ERROR
[PASS] bbox 대각 150km 이하, zoom=15 → DETAIL 응답
[FAIL] bbox 대각 200km, zoom=15 → 400 BBOX_TOO_LARGE_FOR_DETAIL
[PASS] bbox 대각 200km, zoom=10 → SUMMARY 응답 + Warning 헤더
```

### T-2: 줌 임계값 경계 케이스

```
zoom=14 → strategy: "SUMMARY"   (전환점, 요약 유지)
zoom=15 → strategy: "DETAIL"    (줌인 시작)
zoom=13 → strategy: "SUMMARY"
zoom=16 → strategy: "DETAIL"
```

### T-3: ST_DWithin 미터 단위 동작

```
기준점: (127.0, 37.5) / 반경: 1000m
알려진 1km 케이스: (127.0090, 37.5) ≈ 790m 이내 → 조회됨
알려진 1km 케이스: (127.0200, 37.5) ≈ 1720m → 조회 안됨
검증: degree 단위로 계산했다면 1 degree ≈ 111km이므로
      500m = 0.0045° → degree 모드에서 500 전달 시 55500m 검색 발생 → 버그
      geography 캐스팅 시 500 = 500m 정확 → 정상
```

### T-4: 거대 bbox 가드, 줌인 cap/페이지네이션

```
[GUARD] zoom=15, 대각 > 150km → 400 BBOX_TOO_LARGE_FOR_DETAIL
[CAP]   size=201 → 400 PAGE_SIZE_EXCEEDED
[PAGE]  totalCount=342, size=100 → page=0: hasNext=true, page=3: hasNext=false
[WRAP]  zoom=10, 대각 > 150km → SUMMARY 응답 + "Warning: bbox-oversized" 헤더
```

### T-5: 프록시 타임아웃/Rate Limit

```
외부 API stub 타임아웃 (6초) → 504 + body.error = "UPSTREAM_TIMEOUT"
외부 API stub Rate Limit (HTTP 429) → 503 + body.error = "UPSTREAM_RATE_LIMIT"
                                          + Retry-After: 60
외부 API stub 400 → 502 + body.error = "UPSTREAM_CLIENT_ERROR"
```

### T-6: Protected 엔드포인트 인가 검증

```
POST /api/v1/location/score, 헤더 없음 → 401 TOKEN_MISSING
POST /api/v1/location/score, 만료 토큰 → 401 TOKEN_EXPIRED
POST /api/v1/location/score, 위변조 토큰 → 403 TOKEN_INVALID
GET  /api/v1/proxy/public-data, 헤더 없음 → 401 TOKEN_MISSING
GET  /api/v1/map/markers (Public), 헤더 없음 → 200 (정상)
```

### T-7: 입지 점수 breakdown § 산식 정합

```
입력: pharmacy 1개, 거리=320m → travelTime=4.0min → W=1.0, weight=0.8
      contribution = 1.0 × 0.8 = 0.8  ✓

입력: mart 1개, 거리=850m → travelTime=10.625min → W=1/(10.625/5)²=0.2213, weight=0.6
      contribution = 0.2213 × 0.6 = 0.1328  ✓ (오차 ±0.001 허용)

입력: restaurant 2개, 거리=[200m, 600m] → t=[2.5, 7.5]min → W=[1.0, 0.444]
      aggregatedBaseScore = 1.444, weight=0.9
      contribution = 1.3 (오차 ±0.001 허용)  ✓
```

---

## 미결 사항 (Open Questions)

> 사람 승인 전 아래 항목 확인 요망.

1. **줌 임계값 수치**: 카카오 맵 레벨 기준 `≥15=DETAIL, ≤14=SUMMARY`로 제안. 프론트 팀 기준과 맞는지 확인 필요.
2. **거대 bbox 가드**: 150km 초과 시 줌인은 거부(400), 줌아웃은 강제 요약으로 설계. 거부로 통일할지 확인 필요.
3. **입지 점수 POST 캐싱 없음**: 동일 요청 반복이 문제가 되면 서버 사이드 캐시(Redis) spec 추가 필요. 현재는 신규 의존성 추가 금지 원칙 준수.
4. **Protected 범위**: 프록시 엔드포인트를 Protected로 설정. 지도 조회는 Public. 이 기준이 비즈니스 요구와 맞는지 확인 필요.
5. **강제 장치 미설치**: pre-commit hook / CI spec-lint 미구성. 승인 시 hook 먼저 셋업할지 결정 필요.

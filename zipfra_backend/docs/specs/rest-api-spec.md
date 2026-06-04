# Zipfra REST API 명세서 — Read-Only & AI Summary API v1

> **기준 문서**: `agent.md §6~§8` (AI 요약 기능 반영 2026-05-29)  
> **범위**: 조회·분석·AI 요약 전용. 회원·리뷰 쓰기 등은 별도 명세서.  
> **Base URL**: `https://{host}/api/v1`

---

## 목차

1. [공통 규약](#1-공통-규약)
2. [인증 체계](#2-인증-체계)
3. [에러 응답 형식](#3-에러-응답-형식)
4. [API 목록 요약](#4-api-목록-요약)
5. [MAP-01 · 지도 마커 조회](#5-map-01--지도-마커-조회)
6. [LOC-01 · 동적 입지 점수 분석](#6-loc-01--동적-입지-점수-분석)
7. [PUB-01 · 공공데이터 배치 적재본 조회](#7-pub-01--공공데이터-배치-적재본-조회)
8. [PUB-02 · 공공데이터 실시간 단건 중계](#8-pub-02--공공데이터-실시간-단건-중계)
9. [AI-01 · 부동산·인프라 요약](#9-ai-01--부동산인프라-요약)
10. [AI-02 · 리뷰 요약](#10-ai-02--리뷰-요약)
11. [DB 라우팅 요약](#11-db-라우팅-요약)
12. [테스트 기준](#12-테스트-기준)

---

## 1. 공통 규약

| 항목 | 값 |
|------|-----|
| 프로토콜 | HTTPS |
| 데이터 포맷 | `application/json` (UTF-8) |
| 좌표계 | **EPSG:4326 (WGS84)** 고정. `lon` = 경도, `lat` = 위도. |
| 좌표 순서 | 항상 `(lon, lat)` — 절대 뒤바꾸지 않는다. |
| 시각 포맷 | ISO 8601 (`2026-05-29T16:00:00Z`) |
| 페이지네이션 | `page` (0-based) + `size`. 응답에 `hasNext`, `totalCount` 포함. |
| 서버 버전 | 응답 헤더 `X-Api-Version: 1` |

### 공통 응답 헤더

```
Content-Type: application/json;charset=UTF-8
X-Api-Version: 1
```

---

## 2. 인증 체계

### 인증 수준 분류

| 수준 | 기준 | 해당 엔드포인트 |
|------|------|---------------|
| **Public** | 인증 불필요 | `MAP-01` |
| **Protected** | `Authorization: Bearer <AccessToken>` 필수 | `LOC-01`, `PUB-01`, `PUB-02`, `AI-01`, `AI-02` |

> **AI 엔드포인트 Protected 이유**: LLM API 호출은 토큰 비용이 발생하므로, 무분별한 호출을 막기 위해 인증을 필수로 요구한다 (agent.md §6.2).

### JWT 계약 (검증 전용 — 발급은 별도 쓰기 API 명세)

| 항목 | 값 |
|------|-----|
| 알고리즘 | HMAC-SHA256 |
| Access Token TTL | **15분** |
| Refresh Token TTL | **7일** (MySQL `refresh_tokens` 읽기 라우팅) |
| 재발급 엔드포인트 | `POST /api/v1/auth/refresh` (별도 명세) |

### Protected 실패 응답 매핑

| 상황 | HTTP | `error` 코드 |
|------|------|-------------|
| `Authorization` 헤더 자체 없음 | `401` | `TOKEN_MISSING` |
| 토큰 만료 (`exp` 초과) | `401` | `TOKEN_EXPIRED` |
| 서명 위변조 / 형식 불량 | `403` | `TOKEN_INVALID` |

---

## 3. 에러 응답 형식

### 공통 에러 Body

```json
{
  "error": "ERROR_CODE",
  "message": "사람이 읽을 수 있는 설명",
  "timestamp": "2026-05-29T16:00:00Z"
}
```

### 전체 에러 코드 목록

| 코드 | HTTP | 발생 엔드포인트 | 설명 |
|------|------|----------------|------|
| `TOKEN_MISSING` | 401 | Protected 전체 | Authorization 헤더 없음 |
| `TOKEN_EXPIRED` | 401 | Protected 전체 | 액세스 토큰 만료 |
| `TOKEN_INVALID` | 403 | Protected 전체 | 서명 불일치·형식 오류 |
| `BBOX_PARSE_ERROR` | 400 | MAP-01 | bbox 파라미터 파싱 실패 |
| `BBOX_COORD_SWAPPED` | 400 | MAP-01 | lat/lng 뒤바뀜 의심 (`\|lat\| > 90`) |
| `BBOX_INVALID_RANGE` | 400 | MAP-01 | minLng ≥ maxLng 또는 minLat ≥ maxLat |
| `BBOX_TOO_LARGE_FOR_DETAIL` | 400 | MAP-01 | 줌인 + 대각 > 150km |
| `ZOOM_OUT_OF_RANGE` | 400 | MAP-01 | zoom < 1 또는 zoom > 21 |
| `PAGE_SIZE_EXCEEDED` | 400 | MAP-01, PUB-01 | size > max |
| `COORD_OUT_OF_RANGE` | 400 | LOC-01 | lon/lat 범위 초과 |
| `RADIUS_OUT_OF_RANGE` | 400 | LOC-01 | radiusMeters < 100 또는 > 3000 |
| `WEIGHT_OUT_OF_RANGE` | 400 | LOC-01 | weights 값 0.0~1.0 범위 외 |
| `UPSTREAM_TIMEOUT` | 504 | PUB-02 | 외부 API 응답 > 5초 |
| `UPSTREAM_RATE_LIMIT` | 503 | PUB-02 | 외부 API 429 (+ `Retry-After: 60`) |
| `UPSTREAM_CLIENT_ERROR` | 502 | PUB-02 | 외부 API 4xx |
| `AI_QUOTA_EXCEEDED` | 429 | AI-01, AI-02 | 일일 AI 호출 쿼터 초과 |
| `INVALID_PARAM` | 400 | PUB-01, AI-01, AI-02 | 유효하지 않은 파라미터 값 |
| `INTERNAL_SERVER_ERROR` | 500 | 전체 | 서버 내부 오류 |

> **AI 에러 특이사항**: LLM API 장애(타임아웃·5xx)는 에러로 노출하지 않고 `summaryAvailable: false` Fallback으로 처리한다. AI는 보조 기능이므로 장애 시에도 200을 반환한다 (agent.md §6.6).

---

## 4. API 목록 요약

| ID | Method | Path | 인증 | DB | 캐싱 |
|----|--------|------|------|----|------|
| MAP-01 | `GET` | `/api/v1/map/markers` | Public | PostGIS | SUMMARY: ETag+5분 / DETAIL: no-store |
| LOC-01 | `POST` | `/api/v1/location/score` | Protected | PostGIS | 없음 |
| PUB-01 | `GET` | `/api/v1/proxy/public-data` | Protected | PostGIS | 없음 |
| PUB-02 | `GET` | `/api/v1/proxy/public-data/realtime` | Protected | 외부 API | 없음 |
| AI-01 | `POST` | `/api/v1/ai/property-summary` | Protected | MySQL + PostGIS | DB 캐시 24h |
| AI-02 | `POST` | `/api/v1/ai/review-summary` | Protected | MySQL | DB 캐시 24h |

---

## 5. MAP-01 · 지도 마커 조회

### 개요

```
GET /api/v1/map/markers
인증: Public
DB: PostGIS (읽기) — postgis.MarkerMapper
```

지도 Bounding Box 내 부동산 실거래가 데이터를 줌 레벨에 따라 **개별 마커(DETAIL)** 또는 **시·군·구 요약(SUMMARY)**으로 반환한다. 줌 판정은 **서버가 권위**를 가지며, 클라이언트 줌값을 그대로 신뢰하지 않는다.

### 줌 레벨 → 전략 분기표 (서버 권위)

| 클라이언트 `zoom` | 서버 판정 전략 | 응답 형태 |
|-------------------|---------------|----------|
| ≥ 15 | `DETAIL` | 개별 마커 목록 + 페이지네이션 |
| ≤ 14 | `SUMMARY` | 시·군·구 집계 요약 목록 |

> 카카오 맵 레벨 기준 적용 시 `level ≤ 3` = DETAIL, `level ≥ 4` = SUMMARY로 프론트와 협의 후 재조정 가능.

### 요청

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|:----:|--------|------|
| `bbox` | `string` | ✅ | — | `minLng,minLat,maxLng,maxLat` (쉼표 구분, EPSG:4326) |
| `zoom` | `integer` | ✅ | — | 클라이언트 줌 레벨 (1–21). 서버가 임계값으로 재판정. |
| `page` | `integer` | — | `0` | DETAIL 전용, 0-based |
| `size` | `integer` | — | `100` | DETAIL 전용, 최대 `200` |

#### bbox 검증 규칙 (서버 수행)

| 검사 항목 | 거부 조건 | 에러 코드 |
|----------|----------|----------|
| 파싱 | 쉼표 구분 숫자 4개 아님 | `BBOX_PARSE_ERROR` |
| 위·경도 뒤바뀜 | `\|minLat\| > 90` 또는 `\|maxLat\| > 90` | `BBOX_COORD_SWAPPED` |
| 범위 역전 | `minLng ≥ maxLng` 또는 `minLat ≥ maxLat` | `BBOX_INVALID_RANGE` |
| 거대 bbox + DETAIL | 대각거리 > 150km 이고 zoom ≥ 15 | `BBOX_TOO_LARGE_FOR_DETAIL` |
| 거대 bbox + SUMMARY | 대각거리 > 150km 이고 zoom ≤ 14 | 400 대신 **SUMMARY 강제 + `Warning: bbox-oversized` 헤더** |

### 응답

#### 200 OK — DETAIL (zoom ≥ 15)

```http
HTTP/1.1 200 OK
Cache-Control: no-store
X-Api-Version: 1
```

```json
{
  "strategy": "DETAIL",
  "zoom": 15,
  "bbox": {
    "minLng": 126.9,
    "minLat": 37.4,
    "maxLng": 127.1,
    "maxLat": 37.6
  },
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

**DETAIL 응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `strategy` | `string` | 항상 `"DETAIL"` |
| `zoom` | `integer` | 서버가 수신한 줌값 |
| `bbox` | `object` | 적용된 bounding box |
| `page` | `integer` | 현재 페이지 (0-based) |
| `size` | `integer` | 페이지당 항목 수 |
| `totalCount` | `integer` | bbox 내 전체 마커 수 |
| `hasNext` | `boolean` | 다음 페이지 존재 여부 |
| `markers[].id` | `long` | 실거래가 고유 ID |
| `markers[].lon` | `double` | 경도 (EPSG:4326) |
| `markers[].lat` | `double` | 위도 (EPSG:4326) |
| `markers[].dealAmount` | `integer` | 거래금액 (만원) |
| `markers[].buildingName` | `string` | 건물명 |
| `markers[].dongName` | `string` | 법정동명 |

#### 200 OK — SUMMARY (zoom ≤ 14)

```http
HTTP/1.1 200 OK
ETag: "v2-bbox-126.7-37.3-127.3-37.7-zoom12"
Cache-Control: public, max-age=300, stale-while-revalidate=60
Vary: Accept-Encoding
X-Api-Version: 1
```

```json
{
  "strategy": "SUMMARY",
  "zoom": 12,
  "bbox": {
    "minLng": 126.7,
    "minLat": 37.3,
    "maxLng": 127.3,
    "maxLat": 37.7
  },
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

**SUMMARY 응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `strategy` | `string` | 항상 `"SUMMARY"` |
| `summaries[].regionCode` | `string` | 시·군·구 행정코드 |
| `summaries[].regionName` | `string` | 지역명 |
| `summaries[].centerLon` | `double` | 지역 중심 경도 |
| `summaries[].centerLat` | `double` | 지역 중심 위도 |
| `summaries[].count` | `integer` | 거래 건수 |
| `summaries[].avgDealAmount` | `integer` | 평균 거래금액 (만원) |
| `summaries[].maxDealAmount` | `integer` | 최고 거래금액 (만원) |

**캐싱 정책**

| 항목 | 값 |
|------|-----|
| TTL | 5분 (`max-age=300`) — 심야 배치 갱신 주기 반영 |
| ETag | `"v{배치버전}-bbox-{minLng}-{minLat}-{maxLng}-{maxLat}-zoom{level}"` |
| 조건부 요청 | 동일 ETag이면 `304 Not Modified` 반환 |

#### 거대 bbox SUMMARY 강제 응답 (경고 포함)

```http
HTTP/1.1 200 OK
Warning: bbox-oversized
Cache-Control: public, max-age=300
```

```json
{
  "strategy": "SUMMARY",
  "zoom": 10,
  "bboxOversized": true,
  "summaries": [ "..." ]
}
```

#### 에러 응답 예시

```http
HTTP/1.1 400 Bad Request
```
```json
{
  "error": "BBOX_COORD_SWAPPED",
  "message": "위도·경도가 뒤바뀐 것으로 의심됩니다. bbox 순서: minLng,minLat,maxLng,maxLat",
  "timestamp": "2026-05-29T16:00:00Z"
}
```

---

## 6. LOC-01 · 동적 입지 점수 분석

### 개요

```
POST /api/v1/location/score
인증: Protected (Bearer)
DB: PostGIS (읽기) → 백엔드 메모리 계산
캐싱: 없음
```

좌표 + 카테고리별 가중치를 받아 주변 POI를 PostGIS로 추출하고, 백엔드 메모리에서 **거리 감쇠 × 카테고리 분기 × 사용자 가중치** 공식으로 입지 점수를 산출한다.

> **GET+Body 대신 POST 사용 이유**: CDN·일부 프록시가 GET Body를 탈락시켜 신뢰할 수 없음. POST로 확정.  
> **캐싱 없음**: POST는 HTTP 표준 캐시 불가. 반복 호출 성능이 문제가 되면 서버 사이드 캐시(별도 spec)로 해결.

### 점수 산출 알고리즘

#### 거리 감쇠 함수 W(t)

```
도보 속도: 80m/분 (4.8km/h)
t = distanceM / 80.0  (분)

W(t) = 1              (0 ≤ t ≤ 5분)
W(t) = 1 / (t/5)²    (t > 5분)
```

| 거리 | 도보시간 | W(t) |
|------|---------|------|
| 400m | 5분 | 1.000 |
| 800m | 10분 | 0.250 |
| 1200m | 15분 | 0.111 |
| 1600m | 20분 | 0.063 |

#### 카테고리별 분기

| 카테고리 | 모델 | base_score 산출 |
|---------|------|----------------|
| **필수 공급** (약국·마트·은행) | `ONE_IS_ENOUGH` | 가장 가까운 POI 1개의 W(t)만 사용, 중복 가산 금지 |
| **미식·여가** (식당·카페·영화관) | `MORE_IS_BETTER` | 반경 내 모든 POI의 W(t) 합산 |

#### 최종 점수 공식

```
final_score = Σ_category [ base_score[c] × user_weight[c] ]

base_score(필수) = W( min(travel_time_i) for i in category )
base_score(여가) = Σ_i W(travel_time_i)   for i in category
```

### 요청

```http
POST /api/v1/location/score
Authorization: Bearer <AccessToken>
Content-Type: application/json
```

```json
{
  "lon": 127.023,
  "lat": 37.512,
  "radiusMeters": 1500,
  "weights": {
    "pharmacy":   0.8,
    "mart":       0.6,
    "bank":       0.4,
    "restaurant": 0.9,
    "cafe":       0.7,
    "cinema":     0.3
  }
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 기본값 | 제약 |
|------|------|:----:|--------|------|
| `lon` | `double` | ✅ | — | `[-180, 180]` |
| `lat` | `double` | ✅ | — | `[-90, 90]` |
| `radiusMeters` | `integer` | — | `1500` | `[100, 3000]` |
| `weights` | `object` | ✅ | — | 각 값 `[0.0, 1.0]`. 생략된 카테고리는 `0.0` 처리. |

**지원 카테고리 키**

| 키 | 유형 | 설명 |
|----|------|------|
| `pharmacy` | ESSENTIAL | 약국 |
| `mart` | ESSENTIAL | 마트·슈퍼마켓 |
| `bank` | ESSENTIAL | 은행 |
| `restaurant` | LEISURE | 식당 |
| `cafe` | LEISURE | 카페 |
| `cinema` | LEISURE | 영화관 |

### 응답

#### 200 OK

```http
HTTP/1.1 200 OK
Cache-Control: no-store
```

```json
{
  "lon": 127.023,
  "lat": 37.512,
  "radiusMeters": 1500,
  "finalScore": 23.69,
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
        "travelTimeMin": 10.625,
        "baseScore": 0.2213,
        "userWeight": 0.6,
        "contribution": 0.1328
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
        "aggregatedBaseScore": 0.4444,
        "userWeight": 0.3,
        "contribution": 0.1333
      }
    }
  }
}
```

**응답 필드 — ESSENTIAL 항목**

| 필드 | 타입 | 설명 |
|------|------|------|
| `model` | `string` | `"ONE_IS_ENOUGH"` |
| `nearestDistanceM` | `double` | 가장 가까운 POI까지의 거리(m) |
| `travelTimeMin` | `double` | 도보 시간(분) = `nearestDistanceM / 80` |
| `baseScore` | `double` | `W(travelTimeMin)` |
| `userWeight` | `double` | 요청의 weights 값 |
| `contribution` | `double` | `baseScore × userWeight` |

**응답 필드 — LEISURE 항목**

| 필드 | 타입 | 설명 |
|------|------|------|
| `model` | `string` | `"MORE_IS_BETTER"` |
| `poiCount` | `integer` | 반경 내 POI 개수 |
| `aggregatedBaseScore` | `double` | `Σ W(travel_time_i)` |
| `userWeight` | `double` | 요청의 weights 값 |
| `contribution` | `double` | `aggregatedBaseScore × userWeight` |

**finalScore 검증**: `finalScore = Σ contribution` (모든 카테고리). 오차 ±0.001 허용.

#### 에러 응답

| 상황 | HTTP | `error` |
|------|------|---------|
| lon 또는 lat 범위 초과 | 400 | `COORD_OUT_OF_RANGE` |
| radiusMeters < 100 또는 > 3000 | 400 | `RADIUS_OUT_OF_RANGE` |
| weights 값 0.0 미만 또는 1.0 초과 | 400 | `WEIGHT_OUT_OF_RANGE` |
| Authorization 없음 | 401 | `TOKEN_MISSING` |
| 토큰 만료 | 401 | `TOKEN_EXPIRED` |
| 토큰 위변조 | 403 | `TOKEN_INVALID` |

---

## 7. PUB-01 · 공공데이터 배치 적재본 조회

### 개요

```
GET /api/v1/proxy/public-data
인증: Protected (Bearer)
DB: PostGIS (배치 적재본, 읽기) — postgis.PublicDataMapper
캐싱: 없음
```

**심야 배치로 PostGIS에 적재된 대용량 공공데이터**를 조회한다. 인증키 은닉·CORS 차단 목적의 백엔드 프록시이며, 외부 API를 직접 호출하지 않는다.

> **배치 경계**: 전일 24:00 이전 데이터는 이 엔드포인트로 조회. 당일 발생 데이터(배치 미반영)는 **PUB-02** 사용.

### 요청

```http
GET /api/v1/proxy/public-data?type=REAL_ESTATE&regionCode=1165010100&page=0&size=20
Authorization: Bearer <AccessToken>
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|:----:|--------|------|
| `type` | `string` | ✅ | — | `REAL_ESTATE` \| `COMMERCE` |
| `regionCode` | `string` | — | — | 법정동코드 10자리 |
| `page` | `integer` | — | `0` | 0-based |
| `size` | `integer` | — | `20` | 최대 `100` |

**`type` 값 설명**

| 값 | 데이터 원천 | 배치 적재 테이블 |
|----|------------|----------------|
| `REAL_ESTATE` | 국토교통부 실거래가 OpenAPI | PostGIS `real_estate_sales` |
| `COMMERCE` | 공공데이터포털 상권정보 | PostGIS `poi` |

### 응답

#### 200 OK

```json
{
  "type": "REAL_ESTATE",
  "regionCode": "1165010100",
  "page": 0,
  "size": 20,
  "totalCount": 2341,
  "hasNext": true,
  "data": [
    {
      "id": 10023,
      "buildingName": "래미안퍼스티지",
      "dealAmount": 85000,
      "dealYear": 2026,
      "dealMonth": 5,
      "floor": 12,
      "exclusiveArea": 84.93,
      "dongName": "서초동",
      "lawdCd": "11650"
    }
  ]
}
```

**페이지네이션 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalCount` | `integer` | 전체 결과 수 |
| `hasNext` | `boolean` | 다음 페이지 존재 여부 |

#### 에러 응답

| 상황 | HTTP | `error` |
|------|------|---------|
| size > 100 | 400 | `PAGE_SIZE_EXCEEDED` |
| 유효하지 않은 type 값 | 400 | `INVALID_PARAM` |
| Authorization 없음 | 401 | `TOKEN_MISSING` |
| 토큰 만료 | 401 | `TOKEN_EXPIRED` |
| 토큰 위변조 | 403 | `TOKEN_INVALID` |

---

## 8. PUB-02 · 공공데이터 실시간 단건 중계

### 개요

```
GET /api/v1/proxy/public-data/realtime
인증: Protected (Bearer)
DB: 없음 (외부 공공 API 직접 중계)
캐싱: 없음
```

**심야 배치 미반영분(당일 발생 데이터) 단건 보완 조회 전용**. 대량 수집에 사용을 금지한다. 인증키는 서버 환경변수에서만 관리하며 클라이언트에 노출하지 않는다.

> ⚠️ **사용 조건**: 배치가 아직 반영하지 못한 당일 거래 등 단건 보완 목적으로만 사용. 배치 적재본이 있는 데이터는 **PUB-01** 우선 사용.

**retry 정책**: 외부 API 호출 최대 **2회 재시도** (1초 간격). 3회 모두 실패 시 504/503 반환.

### 요청

```http
GET /api/v1/proxy/public-data/realtime?source=MOLIT_APT&dealYear=2026&dealMonth=5&lawd_cd=11650
Authorization: Bearer <AccessToken>
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|:----:|------|
| `source` | `string` | ✅ | `MOLIT_APT` \| `MOLIT_ROW` \| `COMMERCE` |
| `dealYear` | `integer` | ✅ | 거래 연도 (예: 2026) |
| `dealMonth` | `integer` | ✅ | 거래 월 1–12 |
| `lawd_cd` | `string` | ✅ | 법정동코드 5자리 (예: `11650`) |

**`source` 값 → 외부 API 매핑**

| 값 | 외부 API |
|----|----------|
| `MOLIT_APT` | 국토교통부 아파트 실거래가 OpenAPI |
| `MOLIT_ROW` | 국토교통부 연립다세대 실거래가 OpenAPI |
| `COMMERCE` | 공공데이터포털 상권정보 OpenAPI |

### 응답

#### 200 OK

외부 API 원본 JSON을 **인증키 필드 제거** 후 그대로 반환한다.

```http
HTTP/1.1 200 OK
Cache-Control: no-store
```

```json
{
  "response": {
    "header": { "resultCode": "00", "resultMsg": "NORMAL SERVICE." },
    "body": {
      "items": { "item": [ { "...": "외부 API 원본 데이터" } ] },
      "numOfRows": 10,
      "pageNo": 1,
      "totalCount": 23
    }
  }
}
```

#### 에러 응답 (업스트림 장애 계약)

| 상황 | HTTP | `error` | 추가 헤더 |
|------|------|---------|----------|
| 외부 API 응답 없음 (> 5초) | `504` | `UPSTREAM_TIMEOUT` | — |
| 외부 API Rate Limit (HTTP 429) | `503` | `UPSTREAM_RATE_LIMIT` | `Retry-After: 60` |
| 외부 API 클라이언트 오류 (4xx) | `502` | `UPSTREAM_CLIENT_ERROR` | — |
| Authorization 없음 | `401` | `TOKEN_MISSING` | — |
| 토큰 만료 | `401` | `TOKEN_EXPIRED` | — |
| 토큰 위변조 | `403` | `TOKEN_INVALID` | — |

**Rate Limit 에러 예시**

```http
HTTP/1.1 503 Service Unavailable
Retry-After: 60
```
```json
{
  "error": "UPSTREAM_RATE_LIMIT",
  "message": "공공 API Rate Limit 초과. 60초 후 재시도하세요.",
  "timestamp": "2026-05-29T16:00:00Z"
}
```

---

## 9. AI-01 · 부동산·인프라 요약

### 개요

```
POST /api/v1/ai/property-summary
인증: Protected (Bearer)
DB: MySQL (매물 기본정보 읽기) + PostGIS (입지 점수)
캐싱: MySQL ai_summaries 테이블, TTL 24시간
```

매물 ID와 입지 점수 포함 여부를 받아 LLM을 통해 **자연어 요약문(3~5문장)**을 생성한다.  
LLM 장애 시 `summaryAvailable: false` Fallback 응답을 반환하며, 이 경우에도 HTTP 200을 유지한다.

> **캐싱 전략**: 동일 `propertyId`에 대한 요약이 `ai_summaries` 테이블에 24시간 이내 존재하면 LLM 호출 없이 캐시를 반환한다.

### 처리 흐름

```
1. ai_summaries 테이블에서 캐시 조회 (TTL 24h 이내)
   └── 캐시 HIT → 즉시 반환
   └── 캐시 MISS →
       2. MySQL: 매물 정보 조회 (건물명·거래금액·면적·층·법정동)
       3. [includeScore=true] PostGIS: 입지 점수 breakdown 계산 (§5 알고리즘)
       4. 프롬프트 조립 → LLM API 호출 (타임아웃 10초)
          └── 성공: 요약문 반환 + ai_summaries 저장
          └── 실패: summaryAvailable: false Fallback 반환
```

### 프롬프트 설계 (agent.md §6.3)

| 항목 | 내용 |
|------|------|
| System 역할 | "당신은 한국 부동산 정보를 간결하고 객관적으로 요약하는 AI 어시스턴트입니다." |
| User 턴 | 구조화된 매물 JSON 제공 후 "3~5문장으로 요약" 요청 |
| 가드레일 | 가격 예측·투자 추천·법률 조언 생성 금지 명시 |
| 입력 토큰 한도 | max 1,500 tokens |
| 출력 토큰 한도 | max 300 tokens |

### 요청

```http
POST /api/v1/ai/property-summary
Authorization: Bearer <AccessToken>
Content-Type: application/json
```

```json
{
  "propertyId": 10023,
  "includeScore": true
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|:----:|--------|------|
| `propertyId` | `long` | ✅ | — | 조회할 매물 ID |
| `includeScore` | `boolean` | — | `true` | 입지 점수 breakdown을 요약에 포함할지 여부 |

### 응답

#### 200 OK — 요약 생성 성공

```http
HTTP/1.1 200 OK
Cache-Control: no-store
```

```json
{
  "propertyId": 10023,
  "summaryAvailable": true,
  "summary": "서초동 래미안퍼스티지 12층 (전용 84㎡)은 2026년 5월 8억 5,000만 원에 거래되었습니다. 도보 5분 이내 약국과 은행이 위치해 생활 편의성이 높으며, 반경 1.5km 내 식당 48개·카페 22개로 미식 환경이 풍부합니다.",
  "generatedAt": "2026-05-29T17:00:00Z"
}
```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `propertyId` | `long` | 요청한 매물 ID |
| `summaryAvailable` | `boolean` | `true`: 요약문 있음 / `false`: LLM 장애 또는 데이터 없음 |
| `summary` | `string \| null` | 자연어 요약문. `summaryAvailable=false`이면 `null` |
| `generatedAt` | `string \| null` | ISO 8601 생성 시각. `summaryAvailable=false`이면 `null` |

#### 200 OK — Fallback (LLM 장애)

```json
{
  "propertyId": 10023,
  "summaryAvailable": false,
  "summary": null,
  "generatedAt": null
}
```

> **프론트 처리**: `summaryAvailable: false`이면 요약 UI 컴포넌트를 숨긴다 (오류 표시 금지).

#### 에러 응답

| 상황 | HTTP | `error` |
|------|------|---------|
| `propertyId` 없음 (존재하지 않는 매물) | 404 | `PROPERTY_NOT_FOUND` |
| 일일 쿼터 초과 | 429 | `AI_QUOTA_EXCEEDED` |
| Authorization 없음 | 401 | `TOKEN_MISSING` |
| 토큰 만료 | 401 | `TOKEN_EXPIRED` |
| 토큰 위변조 | 403 | `TOKEN_INVALID` |

---

## 10. AI-02 · 리뷰 요약

### 개요

```
POST /api/v1/ai/review-summary
인증: Protected (Bearer)
DB: MySQL (리뷰 목록 읽기)
캐싱: MySQL ai_summaries 테이블, TTL 24시간 (새 리뷰 추가 시 무효화)
```

건물·지역 단위의 리뷰 목록을 LLM에 전달해 **긍정·부정 핵심 테마와 요약문**을 생성한다.  
리뷰 0건이거나 LLM 장애 시 `summaryAvailable: false` Fallback 반환.

> **PII 마스킹**: 리뷰 텍스트를 LLM에 전달하기 전 전화번호·이메일 패턴을 `[REDACTED]`로 치환한다 (agent.md §6.5).

### 처리 흐름

```
1. ai_summaries 테이블에서 캐시 조회 (TTL 24h, 새 리뷰 없음 조건)
   └── 캐시 HIT → 즉시 반환
   └── 캐시 MISS →
       2. MySQL: 최신순 maxReviews건 리뷰 조회
       3. PII 마스킹 (전화번호·이메일 → [REDACTED])
       4. 토큰 추정 → 1,500 tokens 초과 시 최신 순으로 자르기
       5. 프롬프트 조립 → LLM API 호출 (타임아웃 10초)
          └── 성공: positives·negatives·summary 반환 + ai_summaries 저장
          └── 실패: summaryAvailable: false Fallback 반환
```

### 프롬프트 설계 (agent.md §6.4)

| 항목 | 내용 |
|------|------|
| User 턴 | 번호 목록 리뷰 제공 후 "긍정 3가지·부정 3가지·전체 요약 2문장을 JSON으로 반환" 요청 |
| 출력 포맷 | JSON 강제 (structured output 또는 few-shot 예시 제공) |
| 인젝션 방지 | 리뷰 텍스트 앞뒤에 `---USER REVIEW START---` 구분자 삽입 |
| 입력 토큰 한도 | max 1,500 tokens (초과 시 최신 리뷰부터 자르기) |
| 출력 토큰 한도 | max 400 tokens |

### 요청

```http
POST /api/v1/ai/review-summary
Authorization: Bearer <AccessToken>
Content-Type: application/json
```

```json
{
  "targetType": "BUILDING",
  "targetId": 10023,
  "maxReviews": 30
}
```

**요청 필드**

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 |
|------|------|:----:|--------|------|------|
| `targetType` | `string` | ✅ | — | `BUILDING` \| `AREA` | 요약 대상 유형 |
| `targetId` | `long` | ✅ | — | — | 건물 ID 또는 법정동 코드 |
| `maxReviews` | `integer` | — | `30` | `[1, 50]` | LLM 토큰 초과 방지용 상한 |

**`targetType` 값**

| 값 | 설명 |
|----|------|
| `BUILDING` | 특정 건물의 리뷰 전체 |
| `AREA` | 법정동 단위 지역의 리뷰 전체 |

### 응답

#### 200 OK — 요약 생성 성공

```http
HTTP/1.1 200 OK
Cache-Control: no-store
```

```json
{
  "targetType": "BUILDING",
  "targetId": 10023,
  "reviewCount": 28,
  "summaryAvailable": true,
  "positives": [
    "역세권 접근성 우수",
    "관리 상태 양호",
    "조망 좋음"
  ],
  "negatives": [
    "주차 공간 부족",
    "층간소음 언급",
    "관리비 높음"
  ],
  "summary": "전반적으로 입지와 조망에 대한 긍정 평가가 많으나, 주차와 관리비 부담에 대한 불만이 반복됩니다.",
  "generatedAt": "2026-05-29T17:00:00Z"
}
```

**응답 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `targetType` | `string` | 요청의 targetType 그대로 반환 |
| `targetId` | `long` | 요청의 targetId 그대로 반환 |
| `reviewCount` | `integer` | 실제 분석에 사용된 리뷰 수 (0이면 summaryAvailable=false) |
| `summaryAvailable` | `boolean` | `true`: 요약 생성 성공 / `false`: 리뷰 없음 또는 LLM 장애 |
| `positives` | `string[] \| null` | 긍정적 핵심 테마 (최대 3개). Fallback 시 `null` |
| `negatives` | `string[] \| null` | 부정적 핵심 테마 (최대 3개). Fallback 시 `null` |
| `summary` | `string \| null` | 전체 요약문 (2문장). Fallback 시 `null` |
| `generatedAt` | `string \| null` | ISO 8601 생성 시각. Fallback 시 `null` |

#### 200 OK — Fallback (리뷰 없음 또는 LLM 장애)

```json
{
  "targetType": "BUILDING",
  "targetId": 10023,
  "reviewCount": 0,
  "summaryAvailable": false,
  "positives": null,
  "negatives": null,
  "summary": null,
  "generatedAt": null
}
```

#### 에러 응답

| 상황 | HTTP | `error` |
|------|------|---------|
| 유효하지 않은 targetType | 400 | `INVALID_PARAM` |
| maxReviews 범위 초과 (> 50) | 400 | `INVALID_PARAM` |
| 일일 쿼터 초과 | 429 | `AI_QUOTA_EXCEEDED` |
| Authorization 없음 | 401 | `TOKEN_MISSING` |
| 토큰 만료 | 401 | `TOKEN_EXPIRED` |
| 토큰 위변조 | 403 | `TOKEN_INVALID` |

**쿼터 초과 에러 예시**

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 86400
```
```json
{
  "error": "AI_QUOTA_EXCEEDED",
  "message": "일일 AI 요약 호출 한도를 초과했습니다. 내일 다시 시도해 주세요.",
  "timestamp": "2026-05-29T17:00:00Z"
}
```

---

## 11. DB 라우팅 요약

| 엔드포인트 | DB | Mapper 클래스 | 공간 쿼리 전략 |
|------------|-----|-------------|---------------|
| MAP-01 (DETAIL) | PostGIS | `MarkerMapper` | `location && ST_MakeEnvelope(bbox)` + GiST |
| MAP-01 (SUMMARY) | PostGIS | `MarkerMapper` | `region_summary` 사전 집계 테이블 |
| LOC-01 | PostGIS | `PoiMapper` | `ST_DWithin(geom::geography, ..., radiusM)` |
| PUB-01 | PostGIS | `PublicDataMapper` | 배치 적재본 직접 조회 |
| PUB-02 | 외부 API | — (HTTP 프록시) | — |
| AI-01 | MySQL + PostGIS | `PropertyMapper` + `PoiMapper` | 매물 조회(MySQL) + 입지 점수(PostGIS) |
| AI-02 | MySQL | `ReviewMapper` | 리뷰 목록 최신순 조회 |
| AI 캐시 R/W | MySQL | `AiSummaryMapper` | `ai_summaries` 테이블 단건 조회·저장 |
| JWT 검증(내부) | MySQL | `UserMapper` | 단건 PK 조회 |

**공간 쿼리 공통 원칙 (agent.md §4)**

- `ST_DWithin(geom::geography, ..., N)` → N = **미터** (degree 아님)
- `::geography` 캐스팅 필수. `ST_Distance` 단독 사용 금지.
- 모든 `geom` 컬럼: 저장 SRID EPSG:4326 (`geometry`), 거리 연산 시 `::geography` 캐스팅.

---

## 12. 테스트 기준

### T-1 · Bounding Box 좌표 검증

| 입력 | 기대 결과 |
|------|----------|
| `bbox=126.9,37.4,127.1,37.6` | `200 OK` |
| `bbox=37.4,126.9,37.6,127.1` (lat/lng 뒤바뀜) | `400 BBOX_COORD_SWAPPED` |
| `bbox=127.1,37.4,126.9,37.6` (minLng > maxLng) | `400 BBOX_INVALID_RANGE` |
| `bbox=abc,37.4,127.1,37.6` | `400 BBOX_PARSE_ERROR` |
| bbox 대각 ≤ 150km + `zoom=15` | `200 DETAIL` |
| bbox 대각 > 150km + `zoom=15` | `400 BBOX_TOO_LARGE_FOR_DETAIL` |
| bbox 대각 > 150km + `zoom=10` | `200 SUMMARY` + `Warning: bbox-oversized` |

### T-2 · 줌 임계값 경계 케이스

| `zoom` | 기대 `strategy` |
|--------|----------------|
| 13 | `SUMMARY` |
| 14 | `SUMMARY` (전환점, 요약 유지) |
| 15 | `DETAIL` (줌인 시작) |
| 16 | `DETAIL` |

### T-3 · ST_DWithin 미터 단위 동작

기준점 `(127.0, 37.5)`, 반경 **1,000m**:
- `(127.0090, 37.5)` ≈ 790m → **조회됨** ✅
- `(127.0200, 37.5)` ≈ 1,720m → **조회 안됨** ✅
- degree 모드 버그 검증: `geography` 캐스팅 없을 시 `1000` = 1000° ≈ 111,000km → 전국 조회됨 (버그)

### T-4 · 거대 bbox 가드, cap, 페이지네이션

| 케이스 | 기대 결과 |
|--------|----------|
| `zoom=15` + 대각 > 150km | `400 BBOX_TOO_LARGE_FOR_DETAIL` |
| `size=201` | `400 PAGE_SIZE_EXCEEDED` |
| `totalCount=342`, `size=100`, `page=0` | `hasNext=true` |
| `totalCount=342`, `size=100`, `page=3` | `hasNext=false` |
| `zoom=10` + 대각 > 150km | `200 SUMMARY` + `Warning: bbox-oversized` |

### T-5 · 프록시 업스트림 장애

| stub 설정 | 기대 응답 |
|----------|----------|
| 외부 API 응답 > 5초 | `504` + `UPSTREAM_TIMEOUT` |
| 외부 API HTTP 429 | `503` + `UPSTREAM_RATE_LIMIT` + `Retry-After: 60` |
| 외부 API HTTP 400 | `502` + `UPSTREAM_CLIENT_ERROR` |

### T-6 · Protected 엔드포인트 인가

| 요청 | 기대 응답 |
|------|----------|
| `POST /location/score` — 헤더 없음 | `401 TOKEN_MISSING` |
| `POST /location/score` — 만료 토큰 | `401 TOKEN_EXPIRED` |
| `POST /location/score` — 위변조 토큰 | `403 TOKEN_INVALID` |
| `GET /proxy/public-data` — 헤더 없음 | `401 TOKEN_MISSING` |
| `POST /ai/property-summary` — 헤더 없음 | `401 TOKEN_MISSING` |
| `POST /ai/review-summary` — 헤더 없음 | `401 TOKEN_MISSING` |
| `GET /map/markers` — 헤더 없음 (Public) | `200 OK` |

### T-7 · 입지 점수 산식 정합 (오차 ±0.001)

| 입력 | 계산 과정 | 기대값 |
|------|----------|--------|
| pharmacy 1개, 거리 320m | t=4.0분 → W=1.0, weight=0.8 | contribution=**0.800** |
| mart 1개, 거리 850m | t=10.625분 → W=1/(10.625/5)²=0.2213, weight=0.6 | contribution=**0.133** |
| restaurant 2개, 거리 [200m, 600m] | t=[2.5, 7.5]분 → W=[1.0, 0.4444] → Σ=1.4444, weight=0.9 | contribution=**1.300** |

### T-8 · AI Fallback 및 캐시 동작

| 케이스 | 기대 응답 |
|--------|----------|
| LLM API 타임아웃 (> 10초) | `200` + `summaryAvailable: false`, `summary: null` |
| LLM API 500 오류 | `200` + `summaryAvailable: false`, `summary: null` |
| 리뷰 0건 (`reviewCount: 0`) | `200` + `summaryAvailable: false`, `positives: null` |
| 일일 쿼터 초과 | `429` + `AI_QUOTA_EXCEEDED` + `Retry-After: 86400` |
| 동일 `propertyId` 24h 이내 재요청 | `200` + `summaryAvailable: true` (LLM 미호출, DB 캐시 반환) |
| 동일 `propertyId` 24h 이후 재요청 | `200` + `summaryAvailable: true` (LLM 재호출, 캐시 갱신) |
| `maxReviews=51` (범위 초과) | `400` + `INVALID_PARAM` |
| `targetType=INVALID` | `400` + `INVALID_PARAM` |

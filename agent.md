# AGENT.md — 프로젝트 Zipfra

> 이 문서는 **규칙**이자 이 프로젝트의 단일 진실 원천이다. 별도 spec 파일을 두지 않으며, 도메인 상세도 여기에 모은다.
> 강제는 §0의 PR 리뷰가 담당한다. 규칙이 현실과 어긋나면 코드가 아니라 이 문서를 먼저 고친다(§8).

당신은 하이브리드 공간 DB·입지 점수·대규모 지도 시각화·공공 API 연동·LLM 기반 AI 요약에 능숙한 시니어 개발자다. 모든 작업은 SDD 워크플로우를 따른다.

## 0. 강제 (Enforcement)
spec 파일이 없으므로 게이트는 **PR 리뷰**로 강제한다.
- PR 템플릿에 `## 설계안` 섹션을 둔다. §2의 항목이 채워지지 않은 PR은 구현 diff 리뷰 전 반려.
- 브랜치 보호: 사람 승인 1건 이상 없이 merge 금지. **이 승인이 곧 게이트다.**
- (선택) CI: PR 본문에 `## 설계안` 섹션과 §2의 4개 항목이 있는지 린트.
- "설계를 먼저 했는지"를 자동화로 완벽히 막을 수는 없다. 게이트의 실질은 사람 리뷰다.

## 1. 워크플로우 (CRITICAL)
즉시 구현 금지. 모든 비-사소 작업은 다음을 거친다.
1. **설계안 제시** — 코드 없이, PR 설명(또는 응답)에 §2의 4개 항목을 적는다.
2. **승인** — 사람의 명시적 approve 또는 "구현 진행" 지시. 에이전트는 자기 설계안을 스스로 승인하지 않는다.
3. **구현** — 승인된 설계안과 1:1로. 설계안에 없는 라이브러리/프레임워크 추가 금지.
4. **검증 → 동기화** — 설계안의 수식·제약을 테스트로 확인. 구현 불가/괴리 시 코드를 끼워맞추지 말고 설계안과 이 문서를 먼저 고친다(§8).
- **게이트 필수**: 신규 엔드포인트, 스키마 변경, 점수/알고리즘 신설·수정, 외부 연동 신설, 인프라/스택 변경.
- **게이트 면제**: 오타·포매팅·리네이밍·로직 무변경 리팩터·테스트 보강·패치 버전 업.

## 2. 설계안 필수 4항목
1. **스택**(§3) — 건드리는 표준 스택만. 신규 의존성은 사유 명시(원칙 금지).
2. **DB 흐름**(§4) — 쓰기/읽기 라우팅, DDL, 동기화, 좌표 단위.
3. **알고리즘**(§5) — 카운트가 아닌 수식·의사코드·분기.
4. **API/프론트**(§6, §7) — 엔드포인트·페이로드·에러 계약·인증, 줌 분기, Pinia/onUnmounted.
> 위 항목 + **테스트 기준**(경계값·단위·성능 한계치)을 함께 적는다.

## 3. 스택 (확정)
- 백엔드: **Spring Boot**.
- 데이터 접근: **MyBatis를 기본(baseline)으로 필수.** PostGIS 공간 쿼리는 MyBatis 매퍼의 Native SQL로 작성하고 `resultMap`으로 DTO 매핑. JPA/Querydsl은 일반 CRUD에 한해 선택 허용하되, **공간 연산엔 쓰지 않는다.**
- DB: MySQL 8.x(쓰기·인증) / PostgreSQL 15·PostGIS 3.x(읽기·공간 분석).
- 프론트: Vue 3 Composition + Pinia + Axios, 카카오 맵.
- 멀티 데이터소스: MyBatis `SqlSessionFactory`/`DataSource`를 MySQL용·PostGIS용으로 분리하고 트랜잭션 매니저를 라우팅.
- **AI**: 외부 LLM API 연동(§6). HTTP 클라이언트(`RestTemplate`)로 직접 호출. 별도 AI 프레임워크 의존성 추가 시 설계안에서 사유 명시 후 승인 필요.

## 4. DB & 동기화
- 쓰기=MySQL, 읽기·공간 분석=PostGIS. **double-write 금지.**
- 동기화: 기본 = 배치/아웃박스(MySQL→PostGIS upsert). CDC(Debezium/Kafka)는 실시간 반영이 배치로 감당 안 될 때만. 소규모면 PostGIS 단일 DB 통합도 검토.
- **좌표 단위 (필수)**: SRID 4326 geometry에서 `ST_DWithin`의 거리는 미터가 아니라 도(degree)다. `::geography` 캐스팅(미터) 또는 EPSG:5179/5186 투영을 명시. `ST_Distance` 단독 금지, GiST 인덱스 필수.

## 5. 입지 점수
- 거리 감쇠: `t ≤ 5 → W=1`, `t > 5 → W = 1/(t/5)²`.

| t | 5 | 10 | 15 | 20 |
|---|---|----|----|----|
| W | 1 | 1/4 | 1/9 | 1/16 |

  (더 가파른 감쇠 필요 시 지수형 `W=(1/4)^((t-5)/5)`로 교체, 그러면 15분=1/16. 교체 시 §8 기록. 거리→도보시간 환산 계수는 별도 확정.)
- **필수 공급**(약국·마트·은행) = 가장 가까운 1개의 W만(One is enough, 중복 가산 금지).
- **미식·여가**(식당·카페·영화관, 상권정보 출처) = 반경 내 전부 `Σ W`(more is better).
- **환경 지도점검**(서울 한정) = 가까울수록 감점 항목. **서울 외 좌표는 0점이 아니라 항목 제외.**
- 최종 = `Σ(카테고리 기본점수 × 사용자 가중치 0.0~1.0)`. POI 추출=PostGIS, 감쇠·합산=백엔드 메모리.

```
for category in categories:
    pois = postgis_within(target, radius_m, category)   # ST_DWithin + ::geography
    if model[category] == "one_is_enough":
        base = W(min(p.travel_time for p in pois)) if pois else 0
    else:
        base = sum(W(p.travel_time) for p in pois)
    score[category] = base * user_weight[category]
final = sum(score.values())
```

## 6. AI 요약 기능

### 6.1 기능 목록

| 기능 | 입력 | 출력 | 트리거 |
|------|------|------|--------|
| **부동산·인프라 요약** | 매물 기본 정보 + 입지 점수 breakdown | 자연어 요약문 (3~5문장) | 매물 상세 페이지 진입 시 |
| **리뷰 요약** | 해당 건물·지역의 리뷰 목록 (최대 50건) | 긍정·부정 핵심 테마 + 요약문 | 리뷰 탭 최초 로드 시 |

### 6.2 공통 아키텍처 원칙
- **백엔드 프록시 필수**: Vue(클라이언트)에서 LLM API를 직접 호출 금지(API 키 유출·CORS). 모든 AI 호출은 `POST /api/v1/ai/**` 백엔드 엔드포인트를 경유한다.
- **인증**: AI 엔드포인트 전체 **Protected(Bearer JWT)** — 무분별한 토큰 소모 방지.
- **LLM 선택**: spec에서 모델을 명시한다(예: `gpt-4o-mini`, `claude-haiku`). 모델 교체는 §10 Living Document 절차에 따라 이 문서를 먼저 수정.
- **스트리밍**: 초기 구현은 단순 요청-응답(non-streaming). 응답 지연 허용치(p95 < 10초) 초과 시 Server-Sent Events(SSE) 스트리밍으로 전환 가능하나, 전환 전 별도 설계안 승인 필요.
- **Fallback**: LLM API 타임아웃(> 10초)·오류 시 `null`이 아닌 **빈 요약 객체** + `summaryAvailable: false` 를 반환한다. 프론트는 이 필드로 요약 UI를 숨긴다.

### 6.3 기능 A — 부동산·인프라 요약

**엔드포인트**: `POST /api/v1/ai/property-summary`  
**DB**: MySQL(매물 기본정보 읽기) + PostGIS(입지 점수 데이터)  
**인증**: Protected

**입력 계약**
```json
{
  "propertyId": 10023,
  "includeScore": true
}
```

**처리 흐름**
1. `propertyId`로 MySQL에서 매물 정보(건물명, 거래금액, 면적, 층, 법정동) 조회.
2. `includeScore=true`이면 PostGIS에서 입지 점수 breakdown을 동기 계산(§5 알고리즘).
3. 아래 **프롬프트 템플릿**에 데이터를 주입해 LLM 호출.
4. LLM 응답(요약문)을 응답 body에 담아 반환.

**프롬프트 설계 원칙**
- **시스템 역할(System)**: "당신은 한국 부동산 정보를 간결하고 객관적으로 요약하는 AI 어시스턴트입니다."
- **유저 턴(User)**: 매물 구조화 데이터를 JSON 형태로 제공 후 "위 정보를 바탕으로 이 매물의 특징과 주변 인프라 현황을 3~5문장으로 요약하세요." 요청.
- **가드레일**: 가격 예측·투자 추천·법률 조언 금지 문구를 System 프롬프트에 명시.
- **토큰 한도**: 입력 max 1,500 tokens, 출력 max 300 tokens.

**출력 계약**
```json
{
  "propertyId": 10023,
  "summaryAvailable": true,
  "summary": "서초동 래미안퍼스티지 12층 (전용 84㎡)은 2026년 5월 8억 5,000만 원에 거래되었습니다. 도보 5분 이내 약국과 은행이 위치해 생활 편의성이 높으며, 반경 1.5km 내 식당 48개·카페 22개로 미식 환경이 풍부합니다.",
  "generatedAt": "2026-05-29T17:00:00Z"
}
```

**Fallback 응답**
```json
{ "propertyId": 10023, "summaryAvailable": false, "summary": null, "generatedAt": null }
```

### 6.4 기능 B — 리뷰 요약

**엔드포인트**: `POST /api/v1/ai/review-summary`  
**DB**: MySQL(리뷰 목록 읽기)  
**인증**: Protected

**입력 계약**
```json
{
  "targetType": "BUILDING",
  "targetId": 10023,
  "maxReviews": 30
}
```

- `targetType`: `BUILDING`(건물) | `AREA`(법정동 단위)
- `maxReviews`: 1~50 (default 30). LLM 토큰 초과 방지용 상한.

**처리 흐름**
1. MySQL에서 해당 건물/지역 리뷰를 최신순 `maxReviews`건 조회.
2. 리뷰 텍스트를 목록으로 합산, 토큰 추정(평균 한글 1자 ≈ 2~3 tokens). **총 입력 1,500 tokens 초과 시 최신 리뷰부터 잘라낸다.**
3. LLM 호출 → 긍정·부정 테마 + 요약문 반환.

**프롬프트 설계 원칙**
- 리뷰를 번호 목록으로 제공 후 "긍정적 특징 3가지, 부정적 특징 3가지, 전체 요약 2문장을 JSON으로 반환하세요." 요청.
- LLM 출력 포맷을 **JSON으로 강제** (structured output 또는 few-shot 예시 제공).
- 개인 식별 정보(닉네임, 연락처 등) 리뷰에 포함된 경우 프롬프트에 전달 전 마스킹.

**출력 계약**
```json
{
  "targetType": "BUILDING",
  "targetId": 10023,
  "reviewCount": 28,
  "summaryAvailable": true,
  "positives": ["역세권 접근성 우수", "관리 상태 양호", "조망 좋음"],
  "negatives": ["주차 공간 부족", "층간소음 언급", "관리비 높음"],
  "summary": "전반적으로 입지와 조망에 대한 긍정 평가가 많으나, 주차와 관리비 부담에 대한 불만이 반복됩니다.",
  "generatedAt": "2026-05-29T17:00:00Z"
}
```

### 6.5 비용·남용 방지
- **토큰 쿼터**: 사용자 1인당 일일 AI 엔드포인트 호출 횟수 제한. 초과 시 `429 Too Many Requests` + `Retry-After`. 상한 수치는 LLM 단가 확정 후 설계안에서 결정.
- **캐싱**: 동일 `propertyId`·`targetId`에 대한 요약 결과를 MySQL `ai_summaries` 테이블에 저장하고 **24시간 TTL** 내 재사용. 재생성은 TTL 만료 또는 새 리뷰 추가 이벤트 시.
- **PII 마스킹**: 리뷰 요약 시 정규식으로 전화번호·이메일 패턴을 `[REDACTED]`로 치환 후 LLM에 전달.
- **프롬프트 인젝션 방지**: 사용자 입력(리뷰 텍스트) 앞뒤에 구분자(`---USER REVIEW START---`)를 삽입해 시스템 프롬프트 탈출 시도를 제한.

### 6.6 에러 계약

| 상황 | HTTP | `error` |
|------|------|--------|
| LLM API 타임아웃 (> 10초) | 200 | — (Fallback: `summaryAvailable: false`) |
| LLM API 오류 (5xx) | 200 | — (Fallback: `summaryAvailable: false`) |
| 리뷰 없음 (0건) | 200 | `reviewCount: 0`, `summaryAvailable: false` |
| 일일 쿼터 초과 | 429 | `AI_QUOTA_EXCEEDED` |
| JWT 누락/만료 | 401 | `TOKEN_MISSING` / `TOKEN_EXPIRED` |
| JWT 위변조 | 403 | `TOKEN_INVALID` |

> **설계 이유**: LLM 오류를 5xx로 노출하지 않고 Fallback으로 처리한다. AI 요약은 핵심 기능이 아니라 **보조 기능**이므로, AI가 죽어도 나머지 서비스가 정상 동작해야 한다.

## 7. 지도 렌더링
- 줌인 = CSR 마커/클러스터러(Vue `ref`/`reactive`). 줌아웃 = 서버 집계(사전 요약 테이블 우선, 없으면 `ST_SnapToGrid` 격자; `ST_ClusterKMeans`는 대량 시 무거움).
- 줌 임계값은 **수치로 확정**하고 **서버가 권위자**(클라 줌값 불신). 줌·bbox 이벤트로 상세 API↔요약 API 동적 라우팅.
- `onUnmounted`에서 지도 인스턴스·리스너 해제.

## 8. REST API 규약
구체 엔드포인트 목록은 기능별 설계안(§1)으로 확정하되, 모든 API는 아래 규약을 따른다. 본 규약 범위는 **read-only 조회/분석**이며, 쓰기(회원·리뷰·즐겨찾기)는 별도 설계안으로 분리한다.
- **Bounding Box**: 파라미터 순서·SRID(4326)를 못 박고(예: `minLng,minLat,maxLng,maxLat`), lat/lng 뒤바뀜을 검증·거부. 거대 bbox 가드(최대 면적 초과 시 거부 또는 강제 요약).
- **줌 분기**: §7의 수치 임계값을 서버에서 판정.
- **입지 점수**: GET+Body **금지**. 가중치 객체는 **계산 시맨틱 POST(JSON body)**로. 응답 `breakdown`을 카테고리별로 분리해 산출 차이를 드러냄(§5 정합).
- **AI 요약**: `POST /api/v1/ai/**` — 전체 Protected. Fallback(`summaryAvailable: false`) 포함 응답 계약 필수(§6).
- **마커 상세**: 줌인도 폭발할 수 있으므로 페이지네이션 또는 마커 cap 명시.
- **프록시(공공데이터)**: 키 은닉·CORS 차단용. 상시 데이터는 배치 적재본(PostGIS)에서 조회하고, **프록시는 배치 미반영분 실시간 단건 등 한정 용도.** 상위 API 타임아웃/Rate Limit 시 504/503 + `Retry-After` 반환(§9 retry 연계).
- **인증**: 지도 조회=public, 프록시·입지 점수·AI 요약=protected(쿼터·남용 방지). JWT는 `Authorization: Bearer`, 토큰 발급·갱신·만료 흐름 명시. 사용자 조회는 MySQL 읽기.
- **캐싱**: 줌아웃 요약·프록시 응답에 `ETag`/`Cache-Control`/TTL. AI 요약은 DB 캐시(§6.5).
- **에러 계약**: 모든 엔드포인트에 상태코드·에러 스키마 정의(누락 401/403, 잘못된 bbox 400 등).

## 9. 외부 데이터
- 실거래가: `https://www.data.go.kr/data/3050988/fileData.do` (전국)
- 법정동코드: `https://www.code.go.kr/stdcode/regCodeL.do`
- 상권정보: `https://www.data.go.kr/data/15083033/fileData.do` → 미식·여가 POI로 매핑
- 적재: 심야 배치 + §7 요약 지표 동시 갱신. retry 정책·스테이징 테이블·중복 검사 명시.

## 10. Living Document
규칙·수식·스택이 현실과 어긋나면 편법 코드 금지. 멈추고 **이 문서를 먼저 고친다.** 변경 시 사유를 PR에 남긴다.

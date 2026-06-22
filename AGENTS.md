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
4. **검증 → 동기화** — 설계안의 수식·제약을 테스트로 확인. 구현 불가/괴리 시 코드를 끼워맞추지 말고 설계안과 이 문서를 먼저 고친다(§11).
- **게이트 필수**: 신규 엔드포인트, 스키마 변경, 점수/알고리즘 신설·수정, 외부 연동 신설, 인프라/스택 변경.
- **게이트 면제**: 오타·포매팅·리네이밍·로직 무변경 리팩터·테스트 보강·패치 버전 업.

## 2. 설계안 필수 4항목
1. **스택**(§3) — 건드리는 표준 스택만. 신규 의존성은 사유 명시(원칙 금지).
2. **DB 흐름**(§4) — 쓰기/읽기 라우팅, DDL, 동기화, 좌표 단위.
3. **알고리즘**(§5) — 카운트가 아닌 수식·의사코드·분기.
4. **API/프론트/인증**(§6, §7, §10) — 엔드포인트·페이로드·에러 계약·인증(JWT/Redis 포함), 줌 분기, Pinia/onUnmounted.
> 위 항목 + **테스트 기준**(경계값·단위·성능 한계치)을 함께 적는다.

### 2.1 필수 검증 및 테스트 기준 (T-1 ~ T-9)

> 입력 → 기대 결과 형태 단일 표.

| ID | 입력 / 케이스 | 기대 결과 |
|----|--------------|----------|
| **T-1** | `bbox=126.9,37.4,127.1,37.6` | `200 OK` |
| T-1 | `bbox=37.4,126.9,37.6,127.1` (lat/lng 뒤바뀜) | `400 BBOX_COORD_SWAPPED` |
| T-1 | `bbox=127.1,37.4,126.9,37.6` (minLng>maxLng) | `400 BBOX_INVALID_RANGE` |
| T-1 | `bbox=abc,37.4,127.1,37.6` | `400 BBOX_PARSE_ERROR` |
| T-1 | 대각 ≤150km + `zoom=15` | `200 DETAIL` |
| T-1 | 대각 >150km + `zoom=15` | `400 BBOX_TOO_LARGE_FOR_DETAIL` |
| T-1 | 대각 >150km + `zoom=10` | `200 SUMMARY` + `Warning: bbox-oversized` |
| **T-2** | `zoom=13` | `SUMMARY` |
| T-2 | `zoom=14` (전환점) | `SUMMARY` |
| T-2 | `zoom=15` (줌인 시작) | `DETAIL` |
| T-2 | `zoom=16` | `DETAIL` |
| **T-3** | 기준 `(127.0,37.5)` r=1000m, 점 `(127.0090,37.5)`≈790m | 조회됨 ✅ |
| T-3 | 기준 `(127.0,37.5)` r=1000m, 점 `(127.0200,37.5)`≈1720m | 조회 안됨 ✅ |
| T-3 | `::geography` 누락(degree 모드) | `1000`=1000°≈111,000km → 전국 조회(버그) |
| **T-4** | `zoom=15` + 대각 >150km | `400 BBOX_TOO_LARGE_FOR_DETAIL` |
| T-4 | `size=201` | `400 PAGE_SIZE_EXCEEDED` |
| T-4 | `totalCount=342, size=100, page=0` | `hasNext=true` |
| T-4 | `totalCount=342, size=100, page=3` | `hasNext=false` |
| T-4 | `zoom=10` + 대각 >150km | `200 SUMMARY` + `Warning: bbox-oversized` |
| **T-5** | 외부 API 응답 >5초 | `504 UPSTREAM_TIMEOUT` |
| T-5 | 외부 API HTTP 429 | `503 UPSTREAM_RATE_LIMIT` + `Retry-After: 60` |
| T-5 | 외부 API HTTP 4xx(400) | `502 UPSTREAM_CLIENT_ERROR` |
| **T-6** | `POST /location/score` 헤더 없음 | `401 TOKEN_MISSING` |
| T-6 | `POST /location/score` 만료 토큰 | `401 TOKEN_EXPIRED` |
| T-6 | `POST /location/score` 위변조 토큰 | `403 TOKEN_INVALID` |
| T-6 | `GET /proxy/public-data` 헤더 없음 | `401 TOKEN_MISSING` |
| T-6 | `POST /ai/property-summary` 헤더 없음 | `401 TOKEN_MISSING` |
| T-6 | `POST /ai/review-summary` 헤더 없음 | `401 TOKEN_MISSING` |
| T-6 | `GET /map/markers` 헤더 없음 (Public) | `200 OK` |
| **T-7** | subway 1개, 320m → t=4.0, W=1.0 (one_is_enough) | base=**1.000** |
| T-7 | mart 1개, 850m → t=10.625, W=1/(10.625/5)²≈0.2215 (more_is_better) | base=**0.2215**, commerce w=0.6 → 기여 0.133 |
| T-7 | academy 2개 [200m,600m] → t=[2.5,7.5], W=[1.0,0.4444], Σ=1.4444 (more_is_better, education w=1.0) | score=**1.444** |
| T-7 | env_penalty/서울 한정 케이스 | **폐기**(§5 재편, 4분류 전부 양수) |
| **T-8** | LLM 타임아웃(>10초) | `200` + `summaryAvailable:false`, `summary:null` |
| T-8 | LLM 500 오류 | `200` + `summaryAvailable:false`, `summary:null` |
| T-8 | 리뷰 0건 (`reviewCount:0`) | `200` + `summaryAvailable:false`, `positives:null` |
| T-8 | 일일 쿼터 초과 | `429 AI_QUOTA_EXCEEDED` + `Retry-After: 86400` |
| T-8 | 동일 `propertyId` 24h 이내 재요청 | `200` + `summaryAvailable:true` (LLM 미호출, DB 캐시) |
| T-8 | 동일 `propertyId` 24h 이후 재요청 | `200` + `summaryAvailable:true` (LLM 재호출, 캐시 갱신) |
| T-8 | `maxReviews=51` | `400 INVALID_PARAM` |
| T-8 | `targetType=INVALID` | `400 INVALID_PARAM` |
| **T-9** | 유효한 Refresh Token으로 재발급 | `200` + 신규 AT·RT, Redis `rt:{userId}` 갱신 |
| T-9 | 이미 사용된(구) Refresh Token 재사용 | `401 REFRESH_TOKEN_INVALID` + Redis `rt:{userId}` 즉시 삭제 |
| T-9 | 만료된 Refresh Token (Redis TTL 소멸) | `401 REFRESH_TOKEN_EXPIRED` |
| T-9 | 로그아웃 후 기존 Access Token으로 Protected 요청 | `401 TOKEN_BLACKLISTED` |
| T-9 | 로그아웃 후 기존 Refresh Token으로 재발급 시도 | `401 REFRESH_TOKEN_INVALID` (Redis `rt` 삭제됨) |

## 3. 스택 (확정)
- 백엔드: **Spring Boot**.
- 데이터 접근: **MyBatis를 기본(baseline)으로 필수.** PostGIS 공간 쿼리는 MyBatis 매퍼의 Native SQL로 작성하고 `resultMap`으로 DTO 매핑. JPA/Querydsl은 일반 CRUD에 한해 선택 허용하되, **공간 연산엔 쓰지 않는다.**
- DB: MySQL 8.x(쓰기·인증) / PostgreSQL 15·PostGIS 3.x(읽기·공간 분석) / **Redis**(토큰 상태 관리 — RT 저장 + AT Blacklist, §10).
- 프론트: Vue 3 Composition + Pinia + Axios, 카카오 맵.
- 멀티 데이터소스: MyBatis `SqlSessionFactory`/`DataSource`를 MySQL용·PostGIS용으로 분리하고 트랜잭션 매니저를 라우팅.
- **AI**: **Spring AI** 프레임워크 기반(§6). `ChatClient` + `@Tool`(함수 호출) + `Resource` 인터페이스로 LLM ↔ DB 연동. HTTP 클라이언트(`RestTemplate`) 직접 LLM 호출 **금지**. Spring AI 모듈 추가 시 설계안에서 사유 명시 후 승인 필요.

**확정 버전 (build.gradle)**

| 레이어 | 기술(확정) |
|--------|-----------|
| 언어·런타임 | Java 21 / Spring Boot 4.0.6 |
| ORM | MyBatis 3.0.3 (mybatis-spring-boot-starter) |
| 공간 쿼리 | MyBatis Native SQL (JPA/Querydsl 공간 연산 금지) |
| 보안 | Spring Security + JJWT 0.11.5 |
| API 문서 | springdoc-openapi 3.0.2 (Swagger UI) |
| MySQL | 8.x (사용자·인증·AI 요약 캐시·쓰기) |
| PostGIS | PostgreSQL 15.x / PostGIS 3.x (공간·집계 읽기) |
| Redis | 토큰 상태 관리 — RT 저장(`rt:{userId}`) + AT Blacklist(`bl:{accessToken}`), §10 |
| **AI** | **Spring AI** (spring-ai-openai-spring-boot-starter) — `ChatClient`, `@Tool`, `Resource`; 모델 `gpt-4o-mini` |

**멀티 데이터소스 Bean 구성** (둘 다 read-only, `propagation=SUPPORTS`; TransactionManager 동시 오픈 금지 — 서비스 메서드당 하나)

| DataSource | SqlSessionFactory | Mapper 패키지 | 용도 |
|------------|-------------------|--------------|------|
| `primaryDataSource` (MySQL) | `primarySqlSessionFactory` / `primaryTransactionManager` | `com.example.zipfra.mapper.mysql.*` | 사용자·인증 읽기 |
| `spatialDataSource` (PostGIS) | `spatialSqlSessionFactory` / `spatialTransactionManager` | `com.example.zipfra.mapper.postgis.*` | 공간·집계 읽기 |

### 3.3 프로젝트 디렉토리 표준 구조

차후 구현 작업은 아래의 백엔드 및 프론트엔드 표준 구조와 컴포넌트 역할을 준수하여 진행한다.

#### 3.3.1 백엔드 구조

백엔드([zipfra_backend](./zipfra_backend)) 프로젝트의 완료된 뼈대 파일 및 디렉토리 구조는 다음과 같다.

*   [zipfra_backend](./zipfra_backend)
    *   [build.gradle](./zipfra_backend/build.gradle): 프로젝트 의존성 관리 및 빌드 설정 파일. MyBatis 3.0.3, Spring Boot 4.0.6, Spring Security, JJWT 등이 정의됨.
    *   [.env](./zipfra_backend/.env): DB/Redis 주소, JWT 비밀키 등 민감한 로컬 개발용 환경 변수를 관리하는 설정 파일.
    *   `src/`
        *   `main/`
            *   `java/com/example/zipfra/`
                *   [ZipfraApplication.java](./zipfra_backend/src/main/java/com/example/zipfra/ZipfraApplication.java): Spring Boot 메인 애플리케이션 시작 클래스.
                *   [config/](./zipfra_backend/src/main/java/com/example/zipfra/config): 데이터소스 및 보안 설정을 포함하는 자바 설정 클래스 패키지.
                    *   [PrimaryDataSourceConfig.java](./zipfra_backend/src/main/java/com/example/zipfra/config/PrimaryDataSourceConfig.java): MySQL(`primaryDataSource`) 및 트랜잭션 매니저, `SqlSessionFactory` 설정. `com.example.zipfra.mapper.mysql` 패키지를 스캔함.
                    *   [SpatialDataSourceConfig.java](./zipfra_backend/src/main/java/com/example/zipfra/config/SpatialDataSourceConfig.java): PostGIS(`spatialDataSource`) 및 트랜잭션 매니저, `SqlSessionFactory` 설정. `com.example.zipfra.mapper.postgis` 패키지를 스캔함.
                    *   [SecurityConfig.java](./zipfra_backend/src/main/java/com/example/zipfra/config/SecurityConfig.java): Spring Security 및 JWT 인증/인가 필터 체인 환경설정 클래스.
                    *   [RedisConfig.java](./zipfra_backend/src/main/java/com/example/zipfra/config/RedisConfig.java): RedisTemplate 빈 설정 클래스.
                *   [domain/](./zipfra_backend/src/main/java/com/example/zipfra/domain): 비즈니스 도메인 엔티티 패키지.
                    *   [User.java](./zipfra_backend/src/main/java/com/example/zipfra/domain/User.java): users 테이블 매핑 VO.
                *   [dto/](./zipfra_backend/src/main/java/com/example/zipfra/dto): 데이터 전송 객체(DTO) 패키지.
                    *   `auth/`: [SignupRequest.java](./zipfra_backend/src/main/java/com/example/zipfra/dto/auth/SignupRequest.java), [LoginRequest.java](./zipfra_backend/src/main/java/com/example/zipfra/dto/auth/LoginRequest.java), [TokenResponse.java](./zipfra_backend/src/main/java/com/example/zipfra/dto/auth/TokenResponse.java), [TokenDto.java](./zipfra_backend/src/main/java/com/example/zipfra/dto/auth/TokenDto.java)
                    *   `user/`: [UserProfileResponse.java](./zipfra_backend/src/main/java/com/example/zipfra/dto/user/UserProfileResponse.java)
                *   [exception/](./zipfra_backend/src/main/java/com/example/zipfra/exception): 예외 처리 패키지.
                    *   [ErrorCode.java](./zipfra_backend/src/main/java/com/example/zipfra/exception/ErrorCode.java): 공통 에러코드 정의 Enum.
                    *   [ZipfraException.java](./zipfra_backend/src/main/java/com/example/zipfra/exception/ZipfraException.java): 도메인 예외 클래스.
                *   [mapper/](./zipfra_backend/src/main/java/com/example/zipfra/mapper): MyBatis Mapper 인터페이스 패키지.
                    *   [mysql/](./zipfra_backend/src/main/java/com/example/zipfra/mapper/mysql): MySQL 전용 Mapper 인터페이스. [package-info.java](./zipfra_backend/src/main/java/com/example/zipfra/mapper/mysql/package-info.java), [UserMapper.java](./zipfra_backend/src/main/java/com/example/zipfra/mapper/mysql/UserMapper.java) 포함.
                    *   [postgis/](./zipfra_backend/src/main/java/com/example/zipfra/mapper/postgis): PostGIS 전용 Mapper 인터페이스. [package-info.java](./zipfra_backend/src/main/java/com/example/zipfra/mapper/postgis/package-info.java) 포함.
                *   [security/](./zipfra_backend/src/main/java/com/example/zipfra/security): Spring Security와 JWT 인증 관련 핵심 컴포넌트 패키지.
                    *   [JwtUtil.java](./zipfra_backend/src/main/java/com/example/zipfra/security/JwtUtil.java): Access/Refresh JWT 생성, 검증, 파싱 유틸리티.
                    *   [JwtAuthenticationFilter.java](./zipfra_backend/src/main/java/com/example/zipfra/security/JwtAuthenticationFilter.java): Request 헤더의 Bearer 토큰 및 Redis Blacklist(`bl:{accessToken}`)를 검증하는 서블릿 필터.
                    *   [ZipfraPrincipal.java](./zipfra_backend/src/main/java/com/example/zipfra/security/ZipfraPrincipal.java): Spring Security의 `UserDetails` 구현체.
                    *   [CustomAuthenticationEntryPoint.java](./zipfra_backend/src/main/java/com/example/zipfra/security/CustomAuthenticationEntryPoint.java): 인증 실패 시 에러 응답(`TOKEN_MISSING`/`TOKEN_EXPIRED` 등)을 커스텀 JSON 포맷으로 일치시키는 엔트리포인트.
                    *   [CustomAccessDeniedHandler.java](./zipfra_backend/src/main/java/com/example/zipfra/security/CustomAccessDeniedHandler.java): 인가 실패 시 에러 응답(`TOKEN_INVALID`)을 커스텀 JSON 포맷으로 일치시키는 핸들러.
                *   [service/](./zipfra_backend/src/main/java/com/example/zipfra/service): 비즈니스 서비스 레이어 패키지.
                    *   [AuthService.java](./zipfra_backend/src/main/java/com/example/zipfra/service/AuthService.java): 회원가입, 로그인, 로그아웃, 토큰 재발급(RTR) 서비스.
                *   [web/](./zipfra_backend/src/main/java/com/example/zipfra/web): API 컨트롤러 및 전역 예외 처리 패키지.
                    *   [AuthController.java](./zipfra_backend/src/main/java/com/example/zipfra/web/AuthController.java): 인증 API 컨트롤러.
                    *   [UserController.java](./zipfra_backend/src/main/java/com/example/zipfra/web/UserController.java): 유저 API 컨트롤러.
                    *   [GlobalExceptionHandler.java](./zipfra_backend/src/main/java/com/example/zipfra/web/GlobalExceptionHandler.java): @RestControllerAdvice 예외 핸들러.
            *   `resources/`
                *   [application.yml](./zipfra_backend/src/main/resources/application.yml): MySQL 및 PostgreSQL/PostGIS 멀티 데이터소스 계정 정보 설정 및 MyBatis 설정, 로깅, 예외 제외 구성 파일.
                *   `mapper/`: MyBatis SQL XML 매퍼 파일 위치.
                    *   [mysql/](./zipfra_backend/src/main/resources/mapper/mysql): MySQL XML 매퍼. [dummy.xml](./zipfra_backend/src/main/resources/mapper/mysql/dummy.xml), [UserMapper.xml](./zipfra_backend/src/main/resources/mapper/mysql/UserMapper.xml) 포함.
                    *   [postgis/dummy.xml](./zipfra_backend/src/main/resources/mapper/postgis/dummy.xml): PostGIS XML 매퍼 기본 skeleton 파일.
        *   `test/`
            *   `java/com/example/zipfra/`
                *   [ZipfraApplicationTests.java](./zipfra_backend/src/test/java/com/example/zipfra/ZipfraApplicationTests.java): 컨텍스트 로딩 단위 테스트.
                *   [security/JwtSecurityTest.java](./zipfra_backend/src/test/java/com/example/zipfra/security/JwtSecurityTest.java): JWT 토큰 발행, 파싱, 보안 에러 코드 핸들링, Blacklist 검증 등 스프링 시큐리티 통합 테스트 코드.

#### 3.3.2 프론트엔드 구조

프론트엔드([zipfra_frontend](./zipfra_frontend)) 프로젝트의 완료된 뼈대 파일 및 디렉토리 구조는 다음과 같다.

*   [zipfra_frontend](./zipfra_frontend)
    *   [package.json](./zipfra_frontend/package.json): 패키지 의존성 및 빌드 스크립트 정의 파일. Vue 3, Pinia, Axios 등 포함.
    *   [vite.config.js](./zipfra_frontend/vite.config.js): Vite 빌드 및 개발 서버 환경 설정 파일.
    *   [index.html](./zipfra_frontend/index.html): 애플리케이션의 메인 HTML 템플릿 (카카오 맵 API 및 메인 스크립트 로드).
    *   [.env.example](./zipfra_frontend/.env.example): 프론트엔드에 필요한 환경 변수 템플릿 파일.
    *   `src/`
        *   [main.js](./zipfra_frontend/src/main.js): 프론트엔점. Vue 인스턴스 초기화 및 스토어 마운트 처리.
        *   [App.vue](./zipfra_frontend/src/App.vue): 메인 레이아웃 및 Map-First UI 화면 구성을 담당하는 루트 컴포넌트.
        *   [index.css](./zipfra_frontend/src/index.css): 전역 CSS 파일 (구글 폰트 불러오기, 기본 스타일 및 Glassmorphism 디자인 토큰 구성).
        *   [api/](./zipfra_frontend/src/api): 백엔드 API와의 통신을 관리하는 디렉토리.
            *   [http.js](./zipfra_frontend/src/api/http.js): Axios 인스턴스 설정. API 요청 시 Bearer JWT 포함 처리 및 전송/응답 인터셉터 구성 (만료 시 RTR 자동 재발급 포함).
            *   [auth.js](./zipfra_frontend/src/api/auth.js): 인증 관련 API 요청 함수.
        *   [components/](./zipfra_frontend/src/components): 화면 구성에 필요한 공통 컴포넌트 디렉토리.
            *   [map/](./zipfra_frontend/src/components/map)
                *   [KakaoMap.vue](./zipfra_frontend/src/components/map/KakaoMap.vue): 카카오 지도 API 연동, 마커 렌더링, 이벤트 리스너(드래그, 줌 등) 설정 컴포넌트.
            *   [auth/](./zipfra_frontend/src/components/auth)
                *   [AuthModal.vue](./zipfra_frontend/src/components/auth/AuthModal.vue): Glassmorphism 로그인/회원가입 모달 컴포넌트.
        *   [stores/](./zipfra_frontend/src/stores): Pinia 상태 관리 디렉토리.
            *   [auth.js](./zipfra_frontend/src/stores/auth.js): 사용자 인증 상태, Access Token/Refresh Token 관리 및 로그인/회원가입/로그아웃 액션 구현.
            *   [map.js](./zipfra_frontend/src/stores/map.js): 지도 줌 레벨, 현재 Bounding Box 범위, 클릭된 마커 ID 등 지도 정보 상태 관리 스토어.
        *   [router/](./zipfra_frontend/src/router): Vue Router 라우팅 패키지.
            *   [index.js](./zipfra_frontend/src/router/index.js): 라우팅 테이블 정의 및 로그인/세션 관리 네비게이션 가드(Navigation Guard) 구현.
        *   [views/](./zipfra_frontend/src/views): 라우팅 페이지 컴포넌트 디렉토리.
            *   [HomeView.vue](./zipfra_frontend/src/views/HomeView.vue): 카카오 지도 및 실시간 뷰포트 정보 오버레이 메인 화면.
            *   [LoginView.vue](./zipfra_frontend/src/views/LoginView.vue): Glassmorphism 로그인/회원가입 독립 페이지 컴포넌트.
            *   [ReviewWriteView.vue](./zipfra_frontend/src/views/ReviewWriteView.vue): 인증 권한이 필수인 보호 구역(requiresAuth) 데모용 리뷰 작성 화면.
        *   [utils/](./zipfra_frontend/src/utils): 공통 헬퍼 함수 디렉토리.
            *   [bbox.js](./zipfra_frontend/src/utils/bbox.js): Bounding Box 파싱, 경계값 유효성 검증 및 대각 거리 계산 유틸리티.

## 4. DB & 동기화
- 쓰기=MySQL, 읽기·공간 분석=PostGIS. **double-write 금지.**
- 동기화: 기본 = 배치/아웃박스(MySQL→PostGIS upsert). CDC(Debezium/Kafka)는 실시간 반영이 배치로 감당 안 될 때만. 소규모면 PostGIS 단일 DB 통합도 검토.
- **좌표 단위 (필수)**: SRID 4326 `geometry` 타입에서 `ST_DWithin`의 거리 파라미터는 degree 단위로 해석된다(1도 ≈ 111km). 미터 단위로 검색하려면 반드시 `::geography` 캐스팅을 사용해야 한다(`ST_DWithin(geom::geography, target::geography, radius_m)`). EPSG:5179/5186 투영도 허용하나 캐스팅이 기본. `ST_Distance` 단독 사용 금지(인덱스 미사용), GiST 인덱스 필수.

**예시·제약 (보강)**

| 항목 | 규칙/예시 |
|------|----------|
| GiST 인덱스(필수) | `CREATE INDEX idx_poi_geom ON poi USING GIST (geom);` · `CREATE INDEX idx_real_estate_geom ON real_estate_sales USING GIST (location);` |
| 미터 반경 검색 | `ST_DWithin(geom::geography, ST_MakePoint(:lon,:lat)::geography, :radiusMeters)` — `:radiusMeters` = 미터 |
| 좌표 매핑 표준 | `geometry`를 JDBC로 직접 읽으면 `PGobject` 반환 → `ST_X(geom) AS lon` / `ST_Y(geom) AS lat`로 double 분리해 `resultMap` 매핑 |
| degree 버그 검증 | `::geography` 누락 시 `1000` = 1000° ≈ 111,000km(전국 조회). 캐스팅 시 `1000` = 1,000m (T-3) |
| SRID | 저장 EPSG:4326(`geometry`), 거리 연산 시 `::geography` 캐스팅 |

## 5. 입지 점수

> **(2026-06-13, §11) 와이어프레임 4분류로 카테고리 전면 재편.** 기존 ESSENTIAL/LEISURE/ENV_PENALTY 폐기 → 교통/교육/상업/편의 4그룹. 거리 감쇠 엔진(W)·모델(one_is_enough/more_is_better)은 유지. 가중치는 **그룹 단위**(와이어프레임 페르소나 슬라이더 4개)로 변경. ENV 감점·서울 한정 로직(`isInSeoul`·`seoul_boundary`) 완전 제거. PR #12 반영.

- 거리 감쇠: `t ≤ 5 → W=1`, `t > 5 → W = 1/(t/5)²`.

| t | 5 | 10 | 15 | 20 |
|---|---|----|----|----|
| W | 1 | 1/4 | 1/9 | 1/16 |

  (더 가파른 감쇠 필요 시 지수형 `W=(1/4)^((t-5)/5)`로 교체, 그러면 15분=1/16. 교체 시 §11 기록.)
- **거리→도보시간 환산(확정)**: 도보 **80m/분(4.8km/h)** → `t = 거리(m) / 80`. 기본 반경 1,500m(≈19분), `radiusMeters` 오버라이드 범위 `[100, 3000]`.
- **카테고리 4분류**(전부 양수, 감점 없음). 응답 `breakdown`은 `transit`/`education`/`commerce`/`convenience`로 분리, `finalScore = Σ(그룹 기여)`(오차 ±0.001):

| 그룹 | breakdown 키 | POI 카테고리 (모델) | 출처 |
|------|------|------|------|
| 🚇 교통 | `transit` | `subway`(지하철역, one_is_enough) · `bus_stop`(버스정류장, more_is_better) | 공공데이터 |
| 🏫 학교·학원 | `education` | `school`(초·중·고, one_is_enough) · `academy`(학원, more_is_better) | 교육부 학교정보 등 |
| 🛍 상업 | `commerce` | `restaurant`·`cafe`·`cinema`·`mart` (전부 more_is_better) | 상권정보 |
| 🏪 편의 | `convenience` | `convenience_store`(편의점)·`hospital`(병원)·`pharmacy`(약국)·`bank`(은행) (전부 more_is_better) | 공공데이터 |

- **모델**: `one_is_enough` = 가장 가까운 1개의 W만(중복 가산 금지) / `more_is_better` = 반경 내 전부 `Σ W`.
- **전국 적용**(서울 제한 없음). POI 미적재 카테고리는 응답 `count:0`(점수 0). **신규 적재 대상**(`school`·`academy`·`hospital`·`convenience_store`·`mart`)은 별도 티켓(§9) — 적재 전까지 해당 카테고리 0점.
- **가중치 = 그룹 단위 4개**: `weights` 키 = `transit`/`education`/`commerce`/`convenience`, 각 `[0.0, 1.0]`, 생략 시 0.0. (카테고리별 가중치 아님)
- 최종 = `Σ(그룹 기본점수 × 그룹 가중치 0.0~1.0)`. 그룹 기본점수 = 그룹 소속 카테고리 base 의 합. POI 추출=PostGIS, 감쇠·합산=백엔드 메모리.

```
for category in all_categories:
    pois = postgis_within(target, radius_m, category)   # ST_DWithin + ::geography
    if model[category] == "one_is_enough":
        base[category] = W(min(p.travel_time for p in pois)) if pois else 0
    else:  # more_is_better
        base[category] = sum(W(p.travel_time) for p in pois)
for group in [transit, education, commerce, convenience]:
    group_base = sum(base[c] for c in categories_of(group))
    contribution[group] = group_base * user_weight[group]
final = sum(contribution.values())
```

### 5.1 매물별 점수 사전계산 (MAP-01 연계, PR A)

매물 마커/목록에 점수를 띄우려면 매물마다 점수가 미리 있어야 한다. **무거운 부분(POI 반경쿼리·감쇠합산)은 배치로 미리 계산·저장**하고, **가벼운 곱셈(그룹 base × 페르소나 가중치)은 실시간**으로 처리한다(LOC-01 좌표 클릭 계산과 별개).

- **저장**: PostGIS `property_score`(매물 1:1) — `transit_base`/`education_base`/`commerce_base`/`convenience_base`(가중치 미적용 그룹 base) + `computed_at`.
- **배치**: 각 매물 좌표로 위 §5 base 산출(`LocationScoreCalculator` 재사용) → `property_score` upsert. **기동 시 자동 실행**(ApplicationRunner; `property_score` 비어 있을 때만). 정식 심야 배치(§9)는 후속.
- **MAP-01 응답**: DETAIL 마커에 4그룹 base 포함(`property_score` LEFT JOIN, 미계산 매물은 `0`).
- **표시 정규화(0~100)**: base 는 객관적 감쇠합산(0~)이고 **그룹별 스케일이 크게 다르므로**(교통 ~1 vs 편의 ~57, 버스 미적재·one_is_enough 영향), **프론트 표시 단계**에서 **그룹마다 다른 계수로 0~100 정규화한 뒤 페르소나 가중평균**한다: `groupScore = clamp(0,100, group_base × GROUP_SCALE[g])`, `total = Σ(groupScore × persona_weight) / Σ(persona_weight)`. (종전 단일 `SCALE` 가중평균은 편의·상업이 지배해 페르소나(교통 강조 등)가 역효과 → 폐기.) `GROUP_SCALE` 은 적재 지역 평균 기준 튜닝값(`utils/score.js`). 백엔드는 base 까지만 제공해 페르소나 가중치(슬라이더, #6) 실시간 반영과 분리한다.

## 6. AI 요약 기능

> **(2026-06-22, §11) Spring AI Tool/Resource/Planner 구조로 전면 재설계.** 기존 `RestTemplate` 직접 LLM 호출 방식 폐기. `mysql_ai_summaries` Read-Through 캐시 도입, LLM 직접 Tool 호출 아키텍처 확정.

### 6.1 기능 목록

| 기능 | ID | 입력 | 출력 | 트리거 |
|------|-----|------|------|--------|
| **부동산·인프라 요약** | AI-01 | `propertyId` | 자연어 요약문 (3~5문장) | 매물 상세 페이지 진입 시 |
| **리뷰 요약** | AI-02 | `targetType` + `targetId` | 긍정·부정 핵심 테마 + 요약문 | 리뷰 탭 최초 로드 시 |

### 6.2 공통 아키텍처 원칙
- **백엔드 프록시 필수**: Vue(클라이언트)에서 LLM API를 직접 호출 금지(API 키 유출·CORS). 모든 AI 호출은 `POST /api/v1/ai/**` 백엔드 엔드포인트를 경유한다.
- **인증**: AI 엔드포인트 전체 **Protected(Bearer JWT)** — 무분별한 토큰 소모 방지.
- **LLM 선택**: AI-01·AI-02 모두 **`gpt-4o-mini`**(확정). 모델 교체는 §11 Living Document 절차에 따라 이 문서를 먼저 수정.
- **스트리밍**: 초기 구현은 단순 요청-응답(non-streaming). 응답 지연 허용치(p95 < 10초) 초과 시 Server-Sent Events(SSE) 스트리밍으로 전환 가능하나, 전환 전 별도 설계안 승인 필요.
- **Fallback**: LLM API 타임아웃(> 10초)·오류 시 `null`이 아닌 **빈 요약 객체** + `summaryAvailable: false` 를 반환한다. 프론트는 이 필드로 요약 UI를 숨긴다.
- **즉시 LLM 호출 금지**: 요청이 들어오면 반드시 §6.3의 캐시 체크를 먼저 수행한다. `mysql_ai_summaries`를 조회하지 않고 LLM을 호출하는 코드는 PR 리뷰에서 **즉시 반려**.

### 6.3 DB 캐시 계층 — `mysql_ai_summaries` (Read-Through)

> **GIGO 방지 원칙**: 캐시 체크를 건너뛰면 동일 매물에 대해 LLM이 매 요청마다 호출되어 쿼터 폭발 및 응답 불일치가 발생한다. 서비스 레이어의 첫 줄은 반드시 캐시 조회여야 한다.

**테이블 스키마 (`mysql_ai_summaries`)**

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | BIGINT PK AUTO_INCREMENT | 내부 식별자 |
| `summary_type` | VARCHAR(20) | `PROPERTY` \| `REVIEW` |
| `target_id` | BIGINT | `propertyId` (PROPERTY) 또는 `targetId` (REVIEW) |
| `target_type` | VARCHAR(20) | PROPERTY 시 `null`, REVIEW 시 `BUILDING`\|`AREA` |
| `summary_available` | TINYINT(1) | 유효 요약 존재 여부 |
| `summary_json` | JSON | 전체 응답 페이로드(직렬화). LLM 불가 시 `null` |
| `created_at` | DATETIME | 최초 생성 시각 |
| `expires_at` | DATETIME | 만료 시각 (`created_at + 24h`) |

**캐시 룩업 로직 (의사코드)**

```
function lookupCache(summaryType, targetId, targetType):
    row = SELECT * FROM mysql_ai_summaries
            WHERE summary_type=summaryType AND target_id=targetId
              AND (target_type=targetType OR target_type IS NULL)
            LIMIT 1

    if row EXISTS AND row.expires_at > NOW():
        return deserialize(row.summary_json)   // 캐시 HIT → LLM 미호출
    else:
        return CACHE_MISS

function upsertCache(summaryType, targetId, targetType, payload):
    INSERT INTO mysql_ai_summaries (...) VALUES (...)
    ON DUPLICATE KEY UPDATE
        summary_json = VALUES(summary_json),
        summary_available = VALUES(summary_available),
        created_at = NOW(),
        expires_at = DATE_ADD(NOW(), INTERVAL 24 HOUR)
```

**캐시 무효화 조건**
- `expires_at` 경과 (24h TTL 만료)
- 해당 `targetId`에 리뷰 신규 작성 또는 삭제 이벤트 발생 시(REV-02·REV-04 후 `AiSummaryMapper.invalidate(targetId)` 호출)

### 6.4 Spring AI 컴포넌트 구조

> LLM이 데이터를 수동으로 주입받는 구조(종전)를 폐기하고, LLM이 **`@Tool`을 직접 실행**하여 DB에서 데이터를 가져오는 **Function Calling 구조**로 명세한다.

#### 6.4.1 Tool 정의

| Tool ID | 빈 이름 | 호출 시점 | DB / Mapper |
|---------|---------|----------|-------------|
| `getPropertyDetail` | `PropertyDetailTool` | AI-01 Planner가 매물 상세 필요 시 | MySQL `PropertyMapper` (건물명·거래금액·면적·층·법정동) |
| `getLocationBreakdown` | `LocationBreakdownTool` | AI-01 Planner가 입지 점수 필요 시 | PostGIS `PoiMapper` (§5 알고리즘, `property_score` LEFT JOIN) |
| `getReviews` | `ReviewsTool` | AI-02 Planner가 리뷰 목록 필요 시 | MySQL `ReviewMapper` (`mysql_reviews`, 마스킹 완료 평문 반환) |

**`getPropertyDetail` 인터페이스 계약**
- 입력 파라미터: `propertyId: Long`
- 출력(Resource): `{ buildingName, dealAmount, area, floor, beopjungdong, dealType, propertyType, dealDate }`
- 오류 시: `PROPERTY_NOT_FOUND` 예외 → Planner가 수신 후 `summaryAvailable: false` Fallback

**`getLocationBreakdown` 인터페이스 계약**
- 입력 파라미터: `propertyId: Long, radiusMeters: int (default 1500)`
- 출력(Resource): `{ transit: { score, pois:[...] }, education: {...}, commerce: {...}, convenience: {...} }` (§5 breakdown 구조)
- PostGIS `property_score` 존재 시 우선 사용, 없으면 온디맨드 계산 후 upsert

**`getReviews` 인터페이스 계약**
- 입력 파라미터: `targetType: String, targetId: Long, maxReviews: int (default 30)`
- 출력(Resource): PII 마스킹(`[REDACTED]`) 완료된 리뷰 텍스트 목록 + `reviewCount`
- 인젝션 방지: 각 리뷰 앞뒤에 구분자 `---USER REVIEW START---` / `---USER REVIEW END---` 삽입 후 Resource에 포함
- 0건이면 `reviewCount: 0`을 Resource로 반환 → Planner가 `summaryAvailable: false` Fallback 결정

#### 6.4.2 Planner 구성

| Planner | ChatClient 구성 | Tool 조합 | 출력 |
|---------|----------------|-----------|------|
| `PropertySummaryPlanner` | System 프롬프트: 부동산 객관 요약·가드레일 | `getPropertyDetail` + `getLocationBreakdown` | 자연어 요약문 (3~5문장) |
| `ReviewSummaryPlanner` | System 프롬프트: JSON 강제 출력·구분자 인식 | `getReviews` | JSON `{ positives:[...], negatives:[...], summary }` |

**공통 프롬프트 원칙**
- **System 역할**: "당신은 한국 부동산 정보를 간결하고 객관적으로 요약하는 AI 어시스턴트입니다. 가격 예측·투자 추천·법률 조언은 절대 하지 않습니다."
- **토큰 한도**: AI-01 입력 max 1,500 / 출력 max 300 tokens. AI-02 입력 max 1,500 / 출력 max 400 tokens.
- **JSON 강제(AI-02)**: structured output 또는 few-shot 예시로 `{ positives, negatives, summary }` 형태 강제.

### 6.5 처리 흐름 (AI-01·AI-02 공통)

```
[클라이언트]
  │ POST /api/v1/ai/property-summary  { propertyId, includeScore }
  ▼
[AiController]  ← JWT Protected
  │ AiService.generatePropertySummary(propertyId)
  ▼
[AiService]
  ①  cache = AiSummaryMapper.findValid(PROPERTY, propertyId, null)
      if cache != null:
          return deserialize(cache.summary_json)       // ← 캐시 HIT, LLM 미호출
  ②  quotaGuard.check(userId)                         // 일일 쿼터 초과 → 429
  ③  PropertySummaryPlanner.call(propertyId)           // Spring AI ChatClient 실행
      │
      │  LLM → getPropertyDetail(propertyId)           // Tool 호출
      │       → [MySQL PropertyMapper 실행]
      │       → Resource 반환 → LLM 수신
      │
      │  LLM → getLocationBreakdown(propertyId, 1500)  // Tool 호출 (includeScore=true)
      │       → [PostGIS PoiMapper / property_score 실행]
      │       → Resource 반환 → LLM 수신
      │
      │  LLM → 자연어 요약 생성 (최종 응답)
      ▼
  ④  AiSummaryMapper.upsert(PROPERTY, propertyId, null, payload, expires_at=+24h)
  ⑤  return payload                                    // 프론트 반환

[예외 분기]
  LLM 타임아웃(>10s) / LLM 5xx  →  Fallback 반환 { summaryAvailable:false }
                                   (DB upsert 생략 — 캐시에 실패 결과 저장 금지)
  PROPERTY_NOT_FOUND Tool 오류   →  404 반환
  AI_QUOTA_EXCEEDED              →  429 반환
```

> AI-02(`/ai/review-summary`)는 ③에서 `ReviewSummaryPlanner.call(targetType, targetId, maxReviews)`를 호출하고, `getReviews` Tool이 `mysql_reviews`를 조회한다. 나머지 흐름(①②④⑤·예외)은 동일.

### 6.6 기능 A — 부동산·인프라 요약 (AI-01)

**엔드포인트**: `POST /api/v1/ai/property-summary`  
**DB**: MySQL `mysql_ai_summaries`(캐시 R/W) + MySQL `PropertyMapper`(Tool) + PostGIS `PoiMapper`(Tool)  
**인증**: Protected

**입력 계약**
```json
{
  "propertyId": 10023,
  "includeScore": true
}
```

**출력 계약 (캐시 HIT 또는 신규 생성)**
```json
{
  "propertyId": 10023,
  "summaryAvailable": true,
  "summary": "서초동 래미안퍼스티지 12층 (전용 84㎡)은 2026년 5월 8억 5,000만 원에 거래되었습니다. 도보 5분 이내 약국과 은행이 위치해 생활 편의성이 높으며, 반경 1.5km 내 식당 48개·카페 22개로 미식 환경이 풍부합니다.",
  "generatedAt": "2026-05-29T17:00:00Z"
}
```

**Fallback 응답 (LLM 오류·타임아웃)**
```json
{ "propertyId": 10023, "summaryAvailable": false, "summary": null, "generatedAt": null }
```

### 6.7 기능 B — 리뷰 요약 (AI-02)

**엔드포인트**: `POST /api/v1/ai/review-summary`  
**DB**: MySQL `mysql_ai_summaries`(캐시 R/W) + MySQL `mysql_reviews`(Tool via `ReviewsTool`)  
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
- `maxReviews`: 1~50 (default 30). LLM 토큰 초과 방지용 상한. **51 이상 → `400 INVALID_PARAM`**.

**캐시 키**: `(REVIEW, targetId, targetType)` 조합으로 `mysql_ai_summaries` 단건 조회.

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

### 6.8 비용·남용 방지
- **토큰 쿼터**: 사용자 1인당 일일 AI 엔드포인트 호출 횟수 제한. 초과 시 `429 Too Many Requests` + `Retry-After: 86400`. 상한 수치는 LLM 단가 확정 후 설계안에서 결정.
- **캐시 24h TTL**: §6.3의 `mysql_ai_summaries.expires_at` 기준. 캐시 HIT 시 LLM 미호출.
- **리뷰 이벤트 무효화**: REV-02(작성)·REV-04(삭제) 직후 `AiSummaryMapper.invalidate(targetId)` 호출하여 해당 `target_id`의 `expires_at`을 `NOW()`로 업데이트(즉시 만료).
- **PII 마스킹**: `ReviewsTool` 내부에서 정규식으로 전화번호·이메일 패턴을 `[REDACTED]`로 치환 후 Resource에 포함. LLM에는 마스킹된 텍스트만 전달.
- **프롬프트 인젝션 방지**: `ReviewsTool`이 리뷰 텍스트 앞뒤에 구분자(`---USER REVIEW START---` / `---USER REVIEW END---`)를 삽입해 반환. 시스템 프롬프트 탈출 시도를 제한.
- **토큰 절단**: `ReviewsTool`이 리뷰 목록의 총 토큰 추정치(한글 1자 ≈ 2~3 tokens)가 1,500 초과 시, 최신 리뷰부터 역순으로 절단하여 반환.

### 6.9 에러 계약

| 상황 | HTTP | `error` |
|------|------|--------|
| LLM API 타임아웃 (> 10초) | 200 | — (Fallback: `summaryAvailable: false`) |
| LLM API 오류 (5xx) | 200 | — (Fallback: `summaryAvailable: false`) |
| 리뷰 없음 (0건) | 200 | `reviewCount: 0`, `summaryAvailable: false` |
| 매물 없음 (AI-01 `getPropertyDetail` Tool 오류) | 404 | `PROPERTY_NOT_FOUND` |
| 잘못된 파라미터 (`maxReviews>50`·`targetType` 불량 등) | 400 | `INVALID_PARAM` |
| 일일 쿼터 초과 | 429 | `AI_QUOTA_EXCEEDED` (+ `Retry-After: 86400`) |
| JWT 누락/만료 | 401 | `TOKEN_MISSING` / `TOKEN_EXPIRED` |
| JWT 위변조 | 403 | `TOKEN_INVALID` |
| Blacklist 토큰(로그아웃 후 재사용) | 401 | `TOKEN_BLACKLISTED` |

> **설계 이유**: LLM 오류를 5xx로 노출하지 않고 Fallback으로 처리한다. AI 요약은 핵심 기능이 아니라 **보조 기능**이므로, AI가 죽어도 나머지 서비스가 정상 동작해야 한다.

> **디버깅 포인트(GIGO 방지)**: Tool이 반환하는 Resource 품질이 요약 품질을 결정한다. `getPropertyDetail`이 거래 금액 `null`을 그대로 반환하거나, `getReviews`가 마스킹 전 텍스트를 포함하면 LLM 출력이 오염된다. Tool 단위 단위 테스트(Mock DB)를 구현 필수.

## 7. 지도 렌더링
- 줌인 = CSR 마커/클러스터러(Vue `ref`/`reactive`). 줌아웃 = 서버 집계(사전 요약 테이블 우선, 없으면 `ST_SnapToGrid` 격자; `ST_ClusterKMeans`는 대량 시 무거움).
- 줌 임계값은 **수치로 확정**하고 **서버가 권위자**(클라 줌값 불신). 줌·bbox 이벤트로 상세 API↔요약 API 동적 라우팅.
- `onUnmounted`에서 지도 인스턴스·리스너 해제.

### 7.1 줌 전략 상태 전이 (서버 권위, 확정값)

클라가 보낸 `zoom`은 `int`로 수신 후 서버가 임계값으로 재판정. 임계값: `ZOOM_THRESHOLD_IN=15`, `ZOOM_THRESHOLD_OUT=14`.

| 클라이언트 `zoom` | 서버 판정 | 전략 | 응답 형태 |
|-------------------|----------|------|----------|
| `< 1` 또는 `> 21` | 거부 | — | `400 ZOOM_OUT_OF_RANGE` |
| `≤ 13` | 줌아웃 | `SUMMARY` | 시·군·구 사전 집계 요약 |
| `14` (전환점) | 줌아웃 | `SUMMARY` | 요약 유지 |
| `≥ 15` | 줌인 | `DETAIL` | 개별 마커 + 페이지네이션 |

> 카카오 맵 레벨 기준 적용 시 `level ≤ 3`=DETAIL, `level ≥ 4`=SUMMARY로 프론트 팀과 협의 후 재조정 가능.

### 7.2 거대 bbox 가드 (대각 상한 150km, `ST_Distance` 서버 계산)

| 조건 | 동작 |
|------|------|
| 대각 ≤ 150km | 정상 (줌 전략대로) |
| 대각 > 150km + 줌인(`zoom ≥ 15`) | `400 BBOX_TOO_LARGE_FOR_DETAIL` (거부) |
| 대각 > 150km + 줌아웃(`zoom ≤ 14`) | `200 SUMMARY` 강제 다운그레이드 + `Warning: bbox-oversized` 헤더 + `bboxOversized:true` |

### 7.3 줌인 폭발 방지 (페이지네이션)

- 마커 cap **200개/페이지**. `page`(0-based, def 0) + `size`(max **200**, def 100).
- 응답에 `totalCount`, `hasNext` 필수.

### 7.4 지도-AI 컴포넌트 통신 규약 (프론트 파편화 방지)

컴포넌트 간 결합을 끊어 두 개발자가 독립적으로 작업한다. **단방향 데이터 흐름**: 지도 → (이벤트/스토어) → 모달.

| 책임 | 컴포넌트 | 규약 |
|------|---------|------|
| **Dev A** | 지도 컴포넌트 | 마커 클릭 상세 로직(모달 띄우기 등)을 지도 내부에서 **직접 구현 금지**. 오직 **이벤트 발신**(`@marker-click="propertyId"`)에 집중. 지도 상태(좌표·줌·bbox)는 지도 내부에서만 관리. |
| **Dev B** | AI 요약/리뷰 모달 | 지도 상태(좌표·줌 등)를 **알 필요 없음**. `propertyId`를 **Props로 전달받아** AI 요약 API(`/api/v1/ai/**`)를 호출하고 결과를 렌더링하는 **독립 컴포넌트**로 개발. |

- 데이터 흐름: 지도 `@marker-click(propertyId)` → 부모/스토어(Pinia) → 모달 `:propertyId` Props 주입.
- **역방향 의존 금지**: 모달 컴포넌트는 지도 인스턴스를 직접 참조하지 않는다.

### 7.5 프론트엔드 디자인 정체성 (Map-First UI)

모든 Vue 3 + Tailwind CSS 컴포넌트는 아래의 'Map-First UI' 디자인 정체성을 AGENTS.md에 엄격하게 반영해야 한다. 기성 어드민 대시보드 형태를 배제하고, 세련된 프롭테크(PropTech) 스타일을 유지한다.

1. **레이아웃 구조 (Map-First Strategy)**
- Base Layer: 전체 화면(`100vw`, `100vh`, `overflow-hidden`)은 항상 카카오맵 인터페이스가 완전히 채운다.
- Overlays: 모든 UI 요소(검색창, 정보 카드, 모달 등)는 지도 위에 독립된 섬(Floating Island)처럼 완전히 떠 있는 레이아웃 구조를 가진다. (`z-index` 정밀 제어 필수)

2. **시각적 무드 (Glassmorphism & Clean Tech)**
- 매끄러운 반투명 효과: 기본 배경색은 순수한 불투명 흰색이 아닌, `bg-white/90 backdrop-blur-lg border border-white/40` 구조를 사용하여 뒷배경(지도)이 은은하게 비치도록 처리한다.
- 부드러운 그림자: 레이어 간의 입체감을 위해 강력하고 부드러운 그림자(`shadow-2xl` 또는 `shadow-xl`)를 필수적으로 적용한다.
- 둥근 라운딩: 딱딱한 각진 모서리를 배제하고, 메인 카드는 `rounded-3xl`, 버튼이나 태그는 `rounded-xl` 또는 `rounded-full`을 사용하여 모던하고 유연한 느낌을 준다.

3. **컬러 시스템 (Color Palette)**
- Primary (포인트/브랜드): Indigo-600 (`#4f46e5`) 또는 Blue-600 (`#2563eb`)을 사용하여 데이터의 신뢰성과 테크니컬한 감성을 강조한다.
- Accent (성공/위험): 데이터 점수 상승 시 Emerald-600, 감점이나 경고 발생 시 Rose-500을 매핑한다.
- Text: 가독성을 위해 제목은 Slate-900, 본문은 Slate-700, 캡션은 Slate-400~500 라인을 유지한다.

4. **인터랙션 및 모션**
- 사이드바나 모달이 등장할 때, `transition-all duration-500 ease-out`을 활용하여 딱딱하게 끊기지 않고 부드럽게 미끄러지듯(Slide-in) 나타나고 사라져야 한다.
- 마커나 버튼 위에 마우스를 올리면 호버 효과(`hover:scale-105 transition`, `hover:bg-indigo-700`)가 자연스럽게 동작해야 한다.

## 8. REST API 규약
전체 엔드포인트 계약은 §8.1(Dev A)·§8.2(Dev B)에, 공통 에러·라우팅은 §8.3에 명세한다(조회·분석 + 인증·회원·리뷰·즐겨찾기 쓰기 포함). 모든 API는 아래 규약을 따른다.
- **Bounding Box**: 파라미터 순서·SRID(4326)를 못 박고(예: `minLng,minLat,maxLng,maxLat`), lat/lng 뒤바뀜을 검증·거부. 거대 bbox 가드(최대 면적 초과 시 거부 또는 강제 요약).
- **줌 분기**: §7의 수치 임계값을 서버에서 판정.
- **입지 점수**: GET+Body **금지**. 가중치 객체는 **계산 시맨틱 POST(JSON body)**로. 응답 `breakdown`을 카테고리별로 분리해 산출 차이를 드러냄(§5 정합).
- **AI 요약**: `POST /api/v1/ai/**` — 전체 Protected. Fallback(`summaryAvailable: false`) 포함 응답 계약 필수(§6).
- **마커 상세**: 줌인도 폭발할 수 있으므로 페이지네이션 또는 마커 cap 명시.
- **프록시(공공데이터)**: 키 은닉·CORS 차단용. 상시 데이터는 배치 적재본(PostGIS)에서 조회하고, **프록시는 배치 미반영분 실시간 단건 등 한정 용도.** 상위 API 타임아웃/Rate Limit 시 504/503 + `Retry-After` 반환(§9 retry 연계).
- **인증**: 지도 조회=public, 프록시·입지 점수·AI 요약=protected(쿼터·남용 방지). JWT는 `Authorization: Bearer`. 토큰 발급·RTR 재발급·Blacklist 로그아웃 상세 흐름은 §10에서 정의. 사용자 조회는 MySQL 읽기.
- **캐싱**: 줌아웃 요약·프록시 응답에 `ETag`/`Cache-Control`/TTL. AI 요약은 DB 캐시(§6.5).
- **에러 계약**: 모든 엔드포인트에 상태코드·에러 스키마 정의(누락 401/403, 잘못된 bbox 400 등).

**공통 규약**: HTTPS · `application/json;charset=UTF-8` · 좌표계 EPSG:4326 고정(`lon`=경도, `lat`=위도, 순서 `(lon,lat)` 불변) · 시각 ISO 8601 · 페이지네이션 `page`(0-based)+`size`, 응답에 `hasNext`·`totalCount` · 응답 헤더 `X-Api-Version: 1`. 공통 에러 body `{ "error", "message", "timestamp" }`.

**인증 분류**: Public = `MAP-01`, `AUTH-01`, `AUTH-02`, `REV-01`(선택 인증) / Protected(Bearer) = 그 외 전체. `AUTH-03`은 Refresh Token 자체 검증(Public).

> **개발 영토 분할**: §8.1 = Dev A 전담, §8.2 = Dev B 전담, §8.3 = 공통 참조. 위 **공통 규약·인증 분류**는 두 개발자 모두 준수.

### 8.1 데이터 & 지도 API (Dev A 전담)

| ID | Method | URI | 인증 | DB / 캐시 | 주요 제약 |
|----|--------|-----|------|-----------|----------|
| MAP-01 | `GET` | `/api/v1/map/markers` | Public | PostGIS / SUMMARY: `ETag`+`max-age=300`+SWR60, DETAIL: `no-store` | `bbox=minLng,minLat,maxLng,maxLat`(4326); `zoom` 1–21 서버 재판정(§7.1); 대각>150km 가드(§7.2); `size` max 200/def 100, `page` 0-based; **검색 필터(DETAIL 한정)** `dealType`·`propertyType`·`priceMin`·`priceMax` 전부 optional(§8.1.1); **DETAIL 마커에 4그룹 base 포함**(`transitBase`/`educationBase`/`commerceBase`/`convenienceBase`, 미계산 매물 `0`, §5.1) |
| MAP-02 | `GET` | `/api/v1/map/pois` | Public | PostGIS / `no-store` | **POI 오버레이(인프라 표시 토글, DETAIL 한정)**; `bbox=minLng,minLat,maxLng,maxLat`(4326); `groups`=`transit`/`education`/`commerce`/`convenience` CSV(1개 이상); `groups`→category 변환은 `Category` enum 그룹 매핑 재사용; 응답 `[{lat,lng,category,group,name}]`; 빈 `groups`→빈 배열 |
| LOC-01 | `POST` | `/api/v1/location/score` | Protected | PostGIS→메모리 / `no-store` | GET+Body 금지; `lon`[-180,180], `lat`[-90,90], `radiusMeters`[100,3000] def 1500, `weights`=그룹 4키(`transit`/`education`/`commerce`/`convenience`) 각[0.0,1.0] 생략=0.0; 응답 `breakdown`=`transit`/`education`/`commerce`/`convenience`(§5) |
| PUB-01 | `GET` | `/api/v1/proxy/public-data` | Protected | PostGIS 배치 적재본 / 없음 | `type`=`REAL_ESTATE`\|`COMMERCE`; `regionCode` 10자리; `size` max 100/def 20; 전일 24:00 이전 데이터 |
| PUB-02 | `GET` | `/api/v1/proxy/public-data/realtime` | Protected | 외부 API 직접 중계 / `no-store` | 배치 미반영 당일 단건 전용(대량 금지); `source`=`MOLIT_APT`\|`MOLIT_ROW`\|`COMMERCE`, `dealYear`, `dealMonth` 1–12, `lawd_cd` 5자리; 인증키 제거; retry 2회(1초) |

#### 8.1.1 MAP-01 검색 필터 (PR A 신설 · DETAIL 한정)

> 줌인(DETAIL, 개별 매물 마커)에만 적용한다. SUMMARY(줌아웃 시·군·구 집계)는 필터를 무시한다(집계 단위라 매물 속성 필터 의미 없음). 모든 필터는 optional·AND 결합, MyBatis 동적 WHERE(`<if>`)로 생략 가능. 필터 부적합 값은 `400 INVALID_PARAM`.

| 파라미터 | 값 | 비고 |
|----------|-----|------|
| `dealType` | `SALE`\|`JEONSE`\|`WOLSE` | 거래유형(매매/전세/월세). 생략 시 전체 |
| `propertyType` | `APT`\|`OFFICETEL`\|`ROW_HOUSE` | 매물유형(아파트/오피스텔/빌라 **3종**, 원룸 제외). 생략 시 전체 |
| `priceMin` / `priceMax` | 정수(만원) | 가격 범위. 대상 컬럼은 `dealType`별 분기 ↓ |

- **가격 필터 대상 컬럼**: `SALE`→`deal_amount`(매매가), `JEONSE`→`deposit`(보증금), `WOLSE`→`deposit`(보증금 기준; 월세 `monthly_rent`는 별도 표시만). **`dealType` 미지정 + 가격 지정** 시 `COALESCE(deal_amount, deposit)` 기준(매매가 있으면 매매가, 없으면 보증금)으로 적용 — 전월세 매물이 매매가 NULL 때문에 통째로 제외되지 않도록 한다.
- **테스트 기준(T-10)**: `dealType=SALE`→전월세 행 제외 ✅ / `propertyType=APT`→오피스텔·빌라 제외 ✅ / `priceMin=10000&priceMax=50000`(매매)→경계 포함 ✅ / `dealType=INVALID`→`400 INVALID_PARAM` / 필터 전부 생략→기존 bbox 전체 조회(회귀) ✅ / **`dealType` 미지정 + `priceMax=80000`→전세 보증금 7.5억(deposit) 매물 포함**(COALESCE) ✅.

### 8.2 AI·인증·유저 API (Dev B 전담)

| ID | Method | URI | 인증 | DB / 캐시 | 주요 제약 |
|----|--------|-----|------|-----------|----------|
| AI-01 | `POST` | `/api/v1/ai/property-summary` | Protected | MySQL `mysql_ai_summaries`(캐시 R/W) + MySQL `PropertyMapper`(Tool) + PostGIS `PoiMapper`(Tool) / DB 캐시 24h(`expires_at`) | `propertyId`(req), `includeScore`(def true); Spring AI `PropertySummaryPlanner`; `gpt-4o-mini`; 입력≤1500/출력≤300; 캐시 HIT 시 LLM 미호출; Fallback(§6.9) |
| AI-02 | `POST` | `/api/v1/ai/review-summary` | Protected | MySQL `mysql_ai_summaries`(캐시 R/W) + MySQL `mysql_reviews`(Tool) / DB 캐시 24h(`expires_at`); 새 리뷰 이벤트 즉시 무효화 | `targetType`=`BUILDING`\|`AREA`, `targetId`, `maxReviews`[1,50] def 30; Spring AI `ReviewSummaryPlanner`; `gpt-4o-mini`; 입력≤1500/출력≤400; PII 마스킹+인젝션 구분자; Fallback(§6.9) |
| AUTH-01 | `POST` | `/api/v1/auth/signup` | Public | MySQL(쓰기) / 없음 | `email`(RFC5322,≤255), `password`(≥8·영문+숫자+특수 각1), `nickname`(2~20); BCrypt 해시; `201` |
| AUTH-02 | `POST` | `/api/v1/auth/login` | Public | MySQL + Redis(RT) / 없음 | AT 30분+RT 14일 발급, Redis `rt:{userId}` SET; 자격 실패는 사용자 열거 방지 동일 에러(`401 TOKEN_INVALID`) |
| AUTH-03 | `POST` | `/api/v1/auth/refresh` | Public(RT 검증) | Redis / 없음 | RTR(§10.2): 구 RT 삭제+신규 AT/RT 발급; 불일치=탈취의심 즉시 삭제 |
| AUTH-04 | `POST` | `/api/v1/auth/logout` | Protected | Redis / 없음 | `rt:{userId}` DEL + `bl:{accessToken}` SET(TTL=AT 잔여); `204` |
| USER-01 | `GET` | `/api/v1/users/me` | Protected | MySQL / 없음 | 프로필 조회 |
| USER-02 | `PATCH` | `/api/v1/users/me` | Protected | MySQL(쓰기) / 없음 | `nickname`(2~20)만 수정, email 변경 불가 |
| USER-03 | `DELETE` | `/api/v1/users/me` | Protected | MySQL(쓰기)+Redis / 없음 | 탈퇴+로그아웃(RT/AT 무효화); 리뷰·즐겨찾기 soft delete/anonymize(별도 설계안); `204` |
| REV-01 | `GET` | `/api/v1/reviews` | Public(선택) | MySQL / 없음 | `targetType`=`BUILDING`\|`AREA`, `targetId`; 최신순; `size` max 50/def 20 |
| REV-02 | `POST` | `/api/v1/reviews` | Protected | MySQL(쓰기) / 없음 | `rating`[1,5], `content` 10~500자; 1인 1리뷰; 저장 후 `ai_summaries` 무효화(§6.5); `201` |
| REV-03 | `PATCH` | `/api/v1/reviews/{reviewId}` | Protected | MySQL(쓰기) / 없음 | 본인만; `rating`/`content` 부분수정(하나 이상 필수) |
| REV-04 | `DELETE` | `/api/v1/reviews/{reviewId}` | Protected | MySQL(쓰기) / 없음 | 본인만; 삭제 후 `ai_summaries` 무효화; `204` |
| FAV-01 | `GET` | `/api/v1/favorites` | Protected | MySQL / 없음 | 최신 추가순; `size` max 100/def 20 |
| FAV-02 | `POST` | `/api/v1/favorites` | Protected | MySQL(쓰기) / 없음 | `propertyId`; 중복 `409`; `201` |
| FAV-03 | `DELETE` | `/api/v1/favorites/{propertyId}` | Protected | MySQL(쓰기) / 없음 | 미존재 `404`; `204` |

### 8.3 공통 에러 및 라우팅 계약

> 두 개발자 공통 참조. 신규 에러 코드·Mapper 추가 시 이 절을 갱신한다.

> **쓰기 라우팅(AGENTS.md §4)**: 회원·리뷰·즐겨찾기 쓰기는 MySQL 전용, double-write 금지, PostGIS 동기화는 배치/아웃박스.

**에러 코드 (도메인별)**

| 도메인 | 코드 | HTTP | 트리거 |
|--------|------|------|--------|
| 인증/JWT | `TOKEN_MISSING` | 401 | Authorization 헤더 없음 |
| 인증/JWT | `TOKEN_EXPIRED` | 401 | Access Token `exp` 초과 |
| 인증/JWT | `TOKEN_INVALID` | 403 | 서명 위변조·형식 불량 (AUTH-02 자격 실패는 401) |
| 인증/JWT | `TOKEN_BLACKLISTED` | 401 | 로그아웃 처리된 토큰 재사용 (Redis `bl:`) |
| AUTH-03 | `REFRESH_TOKEN_INVALID` | 401 | Redis RT 불일치(탈취 의심) → `rt:{userId}` 즉시 삭제 |
| AUTH-03 | `REFRESH_TOKEN_EXPIRED` | 401 | Refresh Token 만료 (Redis TTL 소멸) |
| MAP-01 | `BBOX_PARSE_ERROR` | 400 | 쉼표 구분 숫자 4개 아님 |
| MAP-01 | `BBOX_COORD_SWAPPED` | 400 | `\|minLat\|>90` 또는 `\|maxLat\|>90` (lat/lng 뒤바뀜) |
| MAP-01 | `BBOX_INVALID_RANGE` | 400 | `minLng≥maxLng` 또는 `minLat≥maxLat` |
| MAP-01 | `BBOX_TOO_LARGE_FOR_DETAIL` | 400 | 대각>150km 이고 `zoom≥15` |
| MAP-01 | `ZOOM_OUT_OF_RANGE` | 400 | `zoom<1` 또는 `zoom>21` |
| MAP-01/PUB-01/REV-01/FAV-01 | `PAGE_SIZE_EXCEEDED` | 400 | `size`>max |
| LOC-01 | `COORD_OUT_OF_RANGE` | 400 | `lon`/`lat` 범위 초과 |
| LOC-01 | `RADIUS_OUT_OF_RANGE` | 400 | `radiusMeters`<100 또는 >3000 |
| LOC-01 | `WEIGHT_OUT_OF_RANGE` | 400 | `weights` 값 0.0~1.0 범위 외 |
| PUB-02 | `UPSTREAM_TIMEOUT` | 504 | 외부 API 응답 >5초 |
| PUB-02 | `UPSTREAM_RATE_LIMIT` | 503 | 외부 API 429 (+ `Retry-After: 60`) |
| PUB-02 | `UPSTREAM_CLIENT_ERROR` | 502 | 외부 API 4xx |
| AI-01/02 | `AI_QUOTA_EXCEEDED` | 429 | 일일 AI 쿼터 초과 (+ `Retry-After: 86400`) |
| AI-01/FAV-02 | `PROPERTY_NOT_FOUND` | 404 | 존재하지 않는 `propertyId` |
| AUTH-01 | `EMAIL_ALREADY_EXISTS` | 409 | 이미 가입된 이메일 |
| USER-01~03 | `USER_NOT_FOUND` | 404 | 존재하지 않는 사용자 |
| REV-03/04 | `REVIEW_NOT_FOUND` | 404 | 존재하지 않는 리뷰 |
| REV-03/04 | `REVIEW_FORBIDDEN` | 403 | 본인 작성 리뷰가 아님 |
| FAV-02 | `FAVORITE_ALREADY_EXISTS` | 409 | 이미 즐겨찾기에 추가된 매물 |
| FAV-03 | `FAVORITE_NOT_FOUND` | 404 | 즐겨찾기 항목 없음 |
| 공통 | `INVALID_PARAM` | 400 | 유효하지 않은 파라미터(PUB-01·AI-*·AUTH-*·REV-* 등) |
| 공통 | `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

> LLM 장애(타임아웃·5xx)는 에러 노출 금지 — `200` + `summaryAvailable:false` Fallback(§6.6).

**DB 라우팅 / Mapper**

| 엔드포인트 | DB | Mapper | 쿼리 전략 |
|------------|-----|--------|----------|
| MAP-01 DETAIL | PostGIS | `postgis.MarkerMapper` | `location && ST_MakeEnvelope(bbox)` + GiST |
| MAP-01 SUMMARY | PostGIS | `postgis.MarkerMapper` | `region_summary` 사전 집계 테이블 |
| LOC-01 | PostGIS | `postgis.PoiMapper` | `ST_DWithin(geom::geography,…,radiusM)` |
| PUB-01 | PostGIS | `postgis.PublicDataMapper` | 배치 적재본 직접 조회 |
| PUB-02 | 외부 API | — | HTTP 프록시 (DB 없음) |
| AI-01 (캐시 체크) | MySQL | `mysql.AiSummaryMapper` | `mysql_ai_summaries` 캐시 HIT 여부 확인 (최우선) |
| AI-01 (`PropertyDetailTool`) | MySQL | `mysql.PropertyMapper` | 매물 상세 조회 (캐시 MISS 시 LLM Tool 호출) |
| AI-01 (`LocationBreakdownTool`) | PostGIS | `postgis.PoiMapper` | 입지 점수 breakdown (캐시 MISS 시 LLM Tool 호출) |
| AI-01 (캐시 저장) | MySQL | `mysql.AiSummaryMapper` | `mysql_ai_summaries` Upsert (`expires_at = NOW()+24h`) |
| AI-02 (캐시 체크) | MySQL | `mysql.AiSummaryMapper` | `mysql_ai_summaries` 캐시 HIT 여부 확인 (최우선) |
| AI-02 (`ReviewsTool`) | MySQL | `mysql.ReviewMapper` | `mysql_reviews` 최신순 조회 + PII 마스킹 (캐시 MISS 시) |
| AI-02 (캐시 저장) | MySQL | `mysql.AiSummaryMapper` | `mysql_ai_summaries` Upsert (`expires_at = NOW()+24h`) |
| AUTH-01/02 | MySQL | `mysql.UserMapper` | 이메일/PK 조회·삽입 |
| AUTH-02/03 (RT) | Redis | — | `rt:{userId}` SET + TTL |
| AUTH-04 (로그아웃) | Redis | — | `rt:{userId}` DEL, `bl:{at}` SET + TTL |
| JWT 검증 필터 | Redis | — | `bl:{accessToken}` EXISTS |
| USER-01~03 | MySQL | `mysql.UserMapper` | 단건 PK 조회·수정·삭제 |
| REV-01~04 | MySQL | `ReviewMapper` | 건물/지역 목록 + 단건 CRUD |
| FAV-01~03 | MySQL | `FavoriteMapper` | `userId`+`propertyId` 복합키 |

## 9. 외부 데이터
- 실거래가: `https://www.data.go.kr/data/3050988/fileData.do` (전국)
- 법정동코드: `https://www.code.go.kr/stdcode/regCodeL.do`
- 상권정보: `https://www.data.go.kr/data/15083033/fileData.do` → 미식·여가 POI로 매핑
- 교통(지하철역·버스정류장): 국가대중교통DB/국토부 역사정보 등 공공데이터 → `subway`·`bus_stop` POI로 매핑(TRANSIT 그룹, §5). **적재 미완료 상태**(틀만 선반영).
- 적재: 심야 배치 + §7 요약 지표 동시 갱신. retry 정책·스테이징 테이블·중복 검사 명시.
- **전국 DETAIL(`real_estate_sales`) 배치**: `POST /api/v1/admin/ingest/real-estate/nationwide?dealYmd=YYYYMM&sources=` 가 번들 CSV 244개 시군구 × **6종 소스**를 순회하며 국토부 개별 거래를 지오코딩→적재. **6종 = 아파트/오피스텔/연립다세대 × 매매/전월세**(`APT_TRADE`/`APT_RENT`/`OFFI_TRADE`/`OFFI_RENT`/`RH_TRADE`/`RH_RENT`, `sources` 비면 전부·CSV로 일부 지정). `(시군구,소스)`별 독립 트랜잭션(재실행 멱등), **단지 좌표 캐싱**(`name|jibun`)으로 지오코딩 ~절반↓. 종류별 단지명 필드 상이(`aptNm`/`offiNm`/`mhouseNm`), `estateAgentSggNm` 비면 CSV 풀주소로 보강. 매매=`dealAmount`+`SALE`, 전월세=`deposit`/`monthlyRent`+`monthlyRent>0?WOLSE:JEONSE`. 적재 후 `recompute-scores`. **단일 달 스냅샷** — 다른 달은 dealYmd 변경. **소스별 활용신청 필요**(data.go.kr, 미신청 소스는 `Forbidden`→skip).
- **전국 POI 배치**: `POST /api/v1/admin/ingest/poi/nationwide` 가 전국 bbox 전수가 아니라 **매물이 존재하는 0.02도 격자만**(`IngestionMapper.findPropertyGridCells`) 순회하며 카카오 카테고리 검색(빈 격자 낭비 회피). `clearPoi()` 1회 후 격자×11카테고리×≤3페이지, 전역 `seen` 중복제거, 비트랜잭션(격자별 autocommit). 점수(POI 반경) 전국 정확도 확보용. 적재 후 `recompute-scores` 필수.
- **전국 SUMMARY(`region_summary`) 배치**: 시군구 단위 `LAWD_CD` 목록은 정적 참조 데이터라 **번들 CSV(`resources/data/lawd_sgg.csv`, code·표시명·지오코딩주소)** 로 관리(외부 코드 API 의존 X). `POST /api/v1/admin/ingest/region-summary?dealYmd=YYYYMM` 가 전국 250개 시군구를 순회하며 국토부 아파트매매 API 집계(totalCount·평균·최고가) + 주소 지오코딩(중심좌표) → upsert. **강원=`51`, 전북=`52`**(특별자치도 재부여, 국토부 API 실측 기준), **군위군=대구 `27720`**. 빈 결과(totalCount 0)·지오코딩 실패 시 해당 시군구 스킵.

## 10. JWT 인증 및 Redis 활용 규약
인증 및 인가 시스템은 JWT(JSON Web Token, HMAC-SHA256)를 사용하며, 토큰의 상태 관리 및 보안성 강화를 위해 Redis를 인메모리 저장소로 활용한다. 에이전트는 다음 설계 메커니즘을 엄격히 구현해야 한다.

### 10.1. 토큰 이원화 및 Redis 저장 구조
- **Access Token**: 무상태(Stateless)로 관리하며, 유효 기간은 **30분**으로 고정한다. 클라이언트의 Request Header(`Authorization: Bearer <Token>`)를 통해 검증한다.
- **Refresh Token**: 사용자의 로그인 유지 및 Access Token 재발급을 위해 사용하며, Redis에 저장하여 관리한다.
  - **Key 구조**: `rt:{userId}` (형식 통일, 예: `rt:12`)
  - **Value 구조**: 발행된 Refresh Token 문자열
  - **TTL (Time-To-Live)**: Refresh Token 유효 기간과 정확히 일치시켜 자동 만료한다. **TTL = 14일**로 확정. TTL 변경 시 §11 절차를 따른다.

### 10.2. 토큰 재발급 (RTR: Refresh Token Rotation)
- Access Token 만료 후 재발급 요청 시, 클라이언트가 보낸 Refresh Token과 Redis에 저장된 해당 유효 토큰을 비교 검증한다.
- 검증 성공 시 **기존 Refresh Token을 Redis에서 삭제(또는 만료 처리)하고, 새로운 Access Token과 Refresh Token을 함께 재발급**하여 Redis에 갱신 저장한다(RTR 전략). 만약 Redis에 저장된 토큰과 일치하지 않거나 탈취가 의심되는 경우 즉시 토큰을 무효화하고 로그아웃 처리한다.

### 10.3. 로그아웃 및 Access Token Blacklist 관리
- 로그아웃 요청 시, 세션 무효화를 위해 Redis 내의 Refresh Token(`rt:{userId}`)을 즉시 삭제한다.
- 이미 발행된 Access Token의 잔여 유효 시간 동안 발생할 수 있는 보안 취약점을 막기 위해 **Blacklist 방식**을 도입한다.
  - 로그아웃 요청 시 전달받은 Access Token의 남은 유효 시간(만료 시간 - 현재 시간)을 계산한다.
  - **Key 구조**: `bl:{accessToken}`
  - **Value 구조**: `logout` (상수 문자열)
  - **TTL**: 계산된 Access Token의 남은 유효 시간으로 설정한다.
- **Security Filter 체인 반영**: 모든 API 요청을 처리하는 인증 필터(`JwtAuthenticationFilter`)에서 Request Header로 들어온 Access Token이 Redis Blacklist(`bl:...`)에 존재하는지 확인하는 로직을 필수적으로 포함한다. 존재할 경우, 유효한 토큰 형태이더라도 `401 Unauthorized`(`error: TOKEN_BLACKLISTED`)를 반환해야 한다.

## 11. Living Document
규칙·수식·스택이 현실과 어긋나면 편법 코드 금지. 멈추고 **이 문서를 먼저 고친다.** 변경 시 사유를 PR에 남긴다.

**변경 이력**
- (2026-06-22) **§6 AI 요약 기능 Spring AI Tool/Resource/Planner 구조로 전면 재설계** — 기존 `RestTemplate` 직접 LLM 호출 방식 폐기. `mysql_ai_summaries` Read-Through 캐시(24h TTL, `expires_at` 컬럼 기반) 도입. Spring AI `ChatClient` + `@Tool`(`getPropertyDetail`·`getLocationBreakdown`·`getReviews`) + Planner(`PropertySummaryPlanner`·`ReviewSummaryPlanner`) 구조 확정. LLM 요청 전 캐시 조회 필수 게이트, Tool 단위 단위 테스트 필수화. §8.2 AI 엔드포인트 표·§8.3 DB 라우팅 표 정합 수정. §3 스택 표에 Spring AI 행 추가. GIGO 방지 디버깅 포인트 규칙(§6.9 말미) 명시.
- (2026-06-22) **실거래가 적재 6종 일반화(§9)** — 아파트 매매 하드코딩(`deal_type='SALE'`/`property_type='APT'` 매퍼 고정) 제거. 종류(APT/OFFICETEL/ROW_HOUSE)×거래(매매/전월세) **6종 소스** 일반화: `IngestionService.ingestRealEstate(source,lawd,ymd)` + nationwide `sources` CSV 파라미터. `RealEstateRow`에 `dealType`/`propertyType`/`deposit`/`monthlyRent` 추가, 매퍼 `insertAptTrade`→`insertRealEstate`·`deleteAptTrade`→`deleteRealEstate(+dealType,propertyType)`. 종류별 단지명 필드(`aptNm`/`offiNm`/`mhouseNm`) fallback, `estateAgentSggNm` 공란 시 CSV 풀주소 보강(빌라 대응). 전월세 `monthlyRent>0?WOLSE:JEONSE`. application.yml paths 6종. **소스별 data.go.kr 활용신청 필요**(미신청 `Forbidden`→skip). PR #12(property-search).
- (2026-06-21) **전국 POI 적재(§9)** — 전국 매물 점수 정확도 확보. `PoiIngestService.ingestPoisNationwide()` + `POST /admin/ingest/poi/nationwide` 신설. 전국 bbox 전수(빈 격자 5만+) 대신 **매물 존재 0.02도 격자만**(`findPropertyGridCells`, 강남+전국 실측 2,208개) 순회 → 카카오 호출 ~73k(일 쿼터 내). `clearPoi` 1회 후 격자별 autocommit(중간 실패 진행분 보존). 적재 후 `recompute-scores`로 전국 `property_score` 갱신. PR #12(property-search).
- (2026-06-21) **전국 DETAIL 매물 적재(§9)** — 줌인 개별 매물 마커를 전국으로 확대. `IngestionService.ingestAptTradeNationwide(dealYmd)` + `POST /admin/ingest/real-estate/nationwide` 신설(번들 CSV 244 시군구 순회, 시군구별 독립 트랜잭션 via `ObjectProvider` self-proxy). **단지 좌표 캐싱**(`aptNm|jibun`)으로 지오코딩 호출 절감(강남 실측 unique 단지 46%). 202405 기준 37,595건 적재(지오코딩 성공률 92%). 점수(`property_score`)는 POI 적재 영역(강남권)만 유의미 — 전국 POI 적재는 후속. PR #12(property-search).
- (2026-06-21) **전국 SUMMARY 배치 확장(§9)** — `region_summary` 적재를 서울 25구 → **전국 250개 시군구**로 확장. 종전 `IngestionServiceImpl`의 하드코딩 `SGG_NAME`(서울 25) 맵을 **번들 CSV(`resources/data/lawd_sgg.csv`)** 로드로 교체(법정동 시군구 코드는 정적 참조 데이터 → 매 배치 외부 코드 API 의존 회피). 중심 지오코딩 `"서울 "+name` 하드코딩 → CSV의 풀주소(`시도 시군구`). 강원/전북 특별자치도 재부여 코드(`51`/`52`)·군위군 대구 이전(`27720`)은 국토부 실거래가 API로 실측 확정. PR #12(property-search).
- (2026-06-21) **표시 정규화 그룹별로 변경(§5.1)** — 실 POI 적재 후 그룹 base 스케일 불균형(교통 0.6 vs 편의 57) 확인. 종전 단일 `SCALE` 가중평균은 편의·상업이 지배해 전 매물 만점 뭉침 + 페르소나(교통 강조) 역효과 발생. **그룹별 `GROUP_SCALE` 로 0~100 정규화 후 페르소나 가중평균**으로 변경(`utils/score.js`). 강남 실데이터에서 종합점수 20~100 분포·4색 변별 정상화. 버스정류장 미적재(카카오 카테고리 미지원)는 후속. PR #12.
- (2026-06-21) **POI/실거래가 실데이터 적재(§9) + admin 적재 엔드포인트** — 국토부 아파트 매매 OpenAPI(15126469) → 카카오 지오코딩 → PostGIS `real_estate_sales` 적재(`POST /admin/ingest/real-estate`). 카카오 카테고리 검색 → `poi` 11종 적재(`POST /admin/ingest/poi`, 버스정류장 제외). 점수 재계산(`POST /admin/recompute-scores`). 키는 `.env`(PUBLIC_DATA_SERVICE_KEY·KAKAO_REST_API_KEY). PR #12.
- (2026-06-21) **MAP-02 POI 오버레이 엔드포인트 신설(§8.1)** — 와이어3 좌측 "인프라 표시" 토글 실제 동작. `GET /api/v1/map/pois?bbox&groups`(Public), 로컬 `poi` 테이블 조회(외부 API 아님). `groups`(transit/education/commerce/convenience)→category 변환은 기존 `Category` enum 그룹 매핑 재사용. DETAIL 줌에서만 프론트가 켜진 그룹 조회→지도에 그룹색 점. PR #12(property-search).
- (2026-06-17) **MAP-01 가격 필터: `dealType` 미지정 시 `COALESCE(deal_amount, deposit)` 기준(§8.1.1)** — 종전 "미지정=매매가(deal_amount) 기준"은 전세·월세 매물(매매가 NULL)을 가격 범위에서 통째로 제외해 와이어3 지도검색에서 "전세 7.5억이 사라지는" 직관 위반 발생. 매매가 있으면 매매가, 없으면 보증금으로 비교하도록 변경. T-10에 검증 케이스 추가. 프론트 가격 UI는 듀얼 슬라이더→최소/최대 입력칸으로 교체(정밀·명확). PR #12(property-search).
- (2026-06-15) **매물별 점수 사전계산 신설(§5.1)** — PostGIS `property_score`(매물 1:1, 4그룹 base) + 기동 시 배치(ApplicationRunner, `LocationScoreCalculator` 재사용). MAP-01 DETAIL 응답에 4그룹 base 포함(LEFT JOIN, 미계산 0). 0~100 정규화는 프론트 표시 단계(페르소나 가중치 실시간 반영과 분리). 정식 심야 배치(§9)·페르소나 슬라이더(#6)는 후속. PR #12(property-search).
- (2026-06-15) **MAP-01 검색 필터 신설(§8.1.1) + 실거래가 스키마 확장** — 와이어프레임 3(지도 검색) PR A 착수. `real_estate_sales`에 `deal_type`(SALE/JEONSE/WOLSE)·`property_type`(APT/OFFICETEL/ROW_HOUSE, 원룸 제외 3종)·`deposit`·`monthly_rent`·`build_year` 추가, `deal_amount` NULL 허용(전월세). MAP-01 DETAIL에 `dealType`·`propertyType`·`priceMin`·`priceMax` 필터 추가(SUMMARY 미적용). 매물별 점수 사전계산(`property_score` 신규 테이블)은 동 PR 후속 단계. 국토부 실거래가 공공데이터 실제 필드 기준 설계(재작업 방지). PR #12(property-search).
- (2026-06-13) **입지 점수(§5) 카테고리 전면 재편** — 와이어프레임 4분류(교통/학교·학원/상업/편의)로 갈아엎음. 기존 ESSENTIAL/LEISURE/ENV_PENALTY 폐기, ENV 감점·서울 한정(`isInSeoul`·`seoul_boundary`) 로직 제거. 응답 `breakdown` 키 = `transit`/`education`/`commerce`/`convenience`, **가중치는 그룹 단위 4개**로 변경. 거리 감쇠 W공식·모델(one_is_enough/more_is_better)은 유지. 신규 POI(`school`·`academy`·`hospital`·`convenience_store`·`mart`) 적재는 별도 티켓(§9, 적재 전 `count:0`). 와이어프레임/페르소나 UI 정합 목적. PR #12.
- (2026-06-11) 입지 점수(§5)에 TRANSIT 그룹 신설 — `subway`(지하철역, one_is_enough)·`bus_stop`(버스정류장, more_is_better), 전국 적용. 응답 `breakdown`에 `transit` 키 추가(§5). 교통 POI 데이터 적재는 미완료(틀만 선반영, 적재 전 `count:0`). PR #6 리뷰(CSeongWoo) 반영.
- (2026-06-08) 프론트엔드 최초 랜딩 시 불필요한 토큰 재발급(refresh) 호출 제거 및 보호 구역(requiresAuth) 기반 세션 복구/리다이렉션 무한 루프 방지 로직 구현.
- (2026-06-08) Phase 2 회원 인증(AUTH) 및 사용자 기능 풀스택 구현 완료 (MyBatis/MySQL/Redis 연동 백엔드 API 및 테스트 패스 + Vue 3/Pinia/Axios 자동 토큰 갱신(RTR) 및 Glassmorphism UI 프론트엔드).
- (2026-06-08) 백엔드 및 프론트엔드 파일 구조 파악 후 §3.3 프로젝트 디렉토리 표준 구조 섹션 추가.
- (2026-06-08) 프론트엔드 디자인 정체성(Map-First UI, Glassmorphism, Color Palette, Interaction & Motion) 규칙을 §7.5에 추가.
- (2026-06-05) `docs/specs/rest-api-spec.md`·`Read Only Api.spec.md`를 본 문서로 병합·삭제(SSOT 단일화). 도메인 상세는 §2.1·§4·§5·§7·§8.1로 흡수. JWT 값(Access 30분/Refresh 14일/Redis `rt:{userId}`)은 §10 원안 유지로 확정.

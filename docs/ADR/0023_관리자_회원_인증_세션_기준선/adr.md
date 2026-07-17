# ADR-0023: 관리자·회원 인증과 세션 운영 기준

**날짜**: 2026-03-17  
**최종 갱신**: 2026-07-17
**상태**: Accepted

---

## 왜 이 문서가 필요한가

관리자 인증, 회원 세션, 로컬 API key, 최초 관리자 계정 생성 방식은 제품 요구사항보다 운영과 보안에 가까운 주제다.  
이 문서는 인증과 세션만 따로 묶어 현재 운영 기준을 정리한다.

---

## 결정

### 1. Spring Security 체인을 관리자와 회원·공개 요청으로 분리한다

- Spring Boot 4.0.2 기준 `spring-boot-starter-security`를 사용한다.
- 관리자 경로와 회원·공개 경로는 서로 다른 `SecurityFilterChain`이 처리한다.
- 관리자 체인은 서버 HTTP 세션을 만들지 않고 Redis Bearer 세션 또는 local API key로 인증한다.
- 회원·공개 체인은 `HG_SESSION`에서 회원 ID를 읽어 요청 범위의 회원 principal과 `SecurityContext`를 구성한다.
- `RequestIdFilter`와 `RateLimitFilter`는 인증 여부와 무관하게 모든 요청에 적용되도록 Security 체인 앞단에 유지한다.
- 컨트롤러는 Spring Security의 `@AuthenticationPrincipal`로 `AdminPrincipal` 또는 `CustomerPrincipal`을 직접 주입받고 필요한 ID를 애플리케이션 유스케이스에 전달한다. 회원 인증이 선택인 결제·클라이언트 모니터링 API는 nullable `CustomerPrincipal`로 게스트를 구분한다. `/api/v1/me`는 필터가 이미 조회한 회원 응답 스냅샷을 principal에서 재사용해 중복 조회를 피한다.

### 2. 관리자 인증은 Redis 기반 Bearer 세션을 기본으로 한다

- 로그인은 사용자명/비밀번호 기반이다.
- 로그인 성공 시 UUID 세션 토큰을 발급한다.
- 이후 요청은 `Authorization: Bearer {token}` 헤더를 사용한다.
- 세션 저장소는 Redis 기반 `AdminSessionStore`
- 키 패턴은 `admin:session:{token}`
- 세션 TTL은 8시간
- 관리자 로그인과 최초 계정 setup 경로는 인증 없이 호출할 수 있고, 그 외 관리자 경로는 관리자 principal이 필요하다.
- 인증 정보가 없거나 유효하지 않으면 `401`, 인증은 됐지만 권한이 부족하면 `403`을 기존 `ErrorResponse` JSON 형식으로 반환한다.

### 3. 회원 인증은 `HG_SESSION` 쿠키 + Spring Session + Redis를 사용한다

- 로그인/회원가입 성공 시 `HttpSession`에 `customerUserId`를 기록한다.
- 세션 저장소는 Spring Session + Redis를 사용한다.
- 쿠키 이름은 `HG_SESSION`을 유지한다.
- 세션 네임스페이스는 `hg:session`, 기본 만료는 7일이다.
- 회원 인증이 필요하거나 선택적으로 사용되는 요청마다 `customerUserId`에 해당하는 회원을 확인하고 회원 principal과 `SecurityContext`를 구성한다.
- `/api/v1/me/**`는 회원 principal이 필요하고, 결제·클라이언트 모니터링처럼 회원 인증이 선택인 API는 세션이 있을 때만 회원 principal을 사용한다.
- 로그인·회원가입·소셜 로그인 성공 시 세션 ID를 회전한 뒤 `customerUserId`와 로그인 과정에서 필요한 세션 속성을 유지한다.
- 관리자 Bearer 세션과 회원 HTTP 세션은 분리 유지한다.

### 4. 회원 쿠키 인증에는 SPA CSRF 보호를 적용한다

- 회원·공개 체인은 Spring Security의 SPA CSRF 구성을 사용한다.
- 클라이언트는 `GET /api/v1/auth/csrf`로 `XSRF-TOKEN` 쿠키를 발급받고, 상태를 변경하는 요청에 같은 값을 `X-XSRF-TOKEN` 헤더로 보낸다.
- 로그인과 로그아웃은 기존 CSRF 토큰을 폐기하므로 클라이언트가 다음 상태 변경 요청 전에 토큰을 다시 발급받는다.
- 관리자 체인은 브라우저 쿠키가 아니라 명시적인 Bearer/API key 헤더로 인증하므로 CSRF 검사 대상에서 제외한다.

### 5. 기존 ETag와 명시적 캐시 정책을 보존한다

- Spring Security의 기본 cache-control writer는 비활성화한다.
- 공개 상품·클래스·공지 API의 ETag와 `304 Not Modified` 계약을 유지한다.
- 소셜 로그인 URL처럼 응답별로 지정한 `Cache-Control: no-store`는 그대로 유지한다.
- 다른 기본 보안 응답 헤더는 Spring Security 기준을 따른다.

### 6. API key는 로컬과 테스트용 폴백으로만 허용한다

- 기본값은 `enable-api-key-auth=false`, `apiKey=""`
- 프로덕션에서 설정이 빠져도 API key 경로는 비활성 상태
- `local` 프로필에서만 `enable-api-key-auth=true`와 `ADMIN_API_KEY`를 명시한다.
- 기본 관리자 계정은 Flyway migration에 넣지 않고 `LocalAdminSeedService`로 local 환경에서만 만든다.

### 7. 최초 관리자 계정은 일회성 setup token으로만 만든다

- 운영과 개발 공통으로 기본 관리자 계정을 migration이나 seed로 자동 생성하지 않는다.
- `admin_user` 테이블이 비어 있고 `ADMIN_SETUP_TOKEN`이 설정된 동안에만 `/api/v1/admin/setup`과 `/api/v1/admin/setup/status`를 연다.
- setup 경로는 관리자 인증 예외로 두되, `RateLimitFilter`의 `admin-setup-per-minute` 제한을 적용한다.
- setup token이 없거나 관리자 계정이 이미 있으면 엔드포인트는 `404`로 숨긴다.
- 계정을 만든 뒤에는 운영자가 즉시 `ADMIN_SETUP_TOKEN`을 제거한다.

### 8. 현재 필요하지 않은 Security 기능은 도입하지 않는다

- Google·Naver 로그인은 기존 OAuth 클라이언트와 서버 세션 `state` 검증을 유지하며 OAuth2 Client로 전환하지 않는다.
- 관리자 토큰은 자체 Redis 세션이므로 JWT와 OAuth2 Resource Server를 사용하지 않는다.
- 관리자·회원보다 세분화된 역할 요구가 없으므로 method security를 사용하지 않는다.
- 프런트와 API가 same-origin으로 통신하므로 별도 CORS 허용 정책을 추가하지 않는다.

### 9. 인증 외 운영 주제는 전용 ADR에서 본다

- requestId, 구조화 로그, 에러 추적: `ADR-0015`
- 처리율 제한: `ADR-0017`
- 비밀번호 저장 정책: `ADR-0019`
- 결제 외부 호출 보호: `ADR-0020`
- 외부 HTTP 클라이언트 설정: `ADR-0029`
- 타임아웃과 keep-alive: `ADR-0030`

---

## 결과

### 장점

- URL 인가와 요청별 인증 표현을 Spring Security 표준으로 관리한다.
- 관리자와 회원의 서로 다른 세션 계약을 유지하면서 인증 경계를 분리한다.
- 회원 쿠키 인증에 CSRF 보호와 로그인 시 세션 ID 회전을 적용한다.
- 최초 관리자 계정 생성 규칙이 운영 기준과 함께 정리된다.

### 단점

- 같은 애플리케이션 안에서 관리자·회원 체인을 함께 운영하므로 경로가 추가될 때 적용 체인을 확인해야 한다.
- 상태 변경 API를 호출하는 클라이언트는 CSRF 토큰 발급과 갱신 절차를 따라야 한다.
- 운영 전체를 보려면 다른 ADR도 함께 봐야 한다.

---

## 참고 문서

- `docs/ADR/0015_Observability_로깅과_비즈니스_예외/adr.md`
- `docs/ADR/0017_Filter_처리율_제한/adr.md`
- `docs/ADR/0019_비밀번호_해시_정책/adr.md`
- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
- `docs/ADR/0030_타임아웃_계층과_ingress_keep_alive_기준선/adr.md`
- `docs/Idea/0013_회원_세션_Spring_Session_전환_검토/idea.md`
- `docs/Idea/0015_다중_인스턴스용_Redis_도입/idea.md`
- `docs/Idea/0036_Spring_Security_전면_전환_검토/idea.md`
- `docs/PRD/0004_API_계약/spec.md`

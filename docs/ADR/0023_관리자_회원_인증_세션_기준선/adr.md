# ADR-0023: 관리자·회원 인증과 세션 운영 기준

**날짜**: 2026-03-17  
**상태**: Accepted

---

## 왜 이 문서가 필요한가

관리자 인증, 회원 세션, 로컬 API key, 최초 관리자 계정 생성 방식은 제품 요구사항보다 운영과 보안에 가까운 주제다.  
이 문서는 인증과 세션만 따로 묶어 현재 운영 기준을 정리한다.

---

## 결정

### 1. 관리자 인증은 Redis 기반 Bearer 세션을 기본으로 한다

- 로그인은 사용자명/비밀번호 기반이다.
- 로그인 성공 시 UUID 세션 토큰을 발급한다.
- 이후 요청은 `Authorization: Bearer {token}` 헤더를 사용한다.
- 세션 저장소는 Redis 기반 `AdminSessionStore`
- 키 패턴은 `admin:session:{token}`
- 세션 TTL은 8시간

### 2. 회원 인증은 `HG_SESSION` 쿠키 + Spring Session + Redis를 사용한다

- 로그인/회원가입 성공 시 `HttpSession`에 `customerUserId`를 기록한다.
- 세션 저장소는 Spring Session + Redis를 사용한다.
- 쿠키 이름은 `HG_SESSION`을 유지한다.
- 세션 네임스페이스는 `hg:session`, 기본 만료는 7일이다.
- `CustomerAuthFilter`는 Spring Session filter 이후 실행된다.
- 관리자 Bearer 세션과 회원 HTTP 세션은 분리 유지한다.

### 3. API key는 로컬과 테스트용 폴백으로만 허용한다

- 기본값은 `enable-api-key-auth=false`, `apiKey=""`
- 프로덕션에서 설정이 빠져도 API key 경로는 비활성 상태
- `local` 프로필에서만 `enable-api-key-auth=true`와 `ADMIN_API_KEY`를 명시한다.
- 기본 관리자 계정은 Flyway migration에 넣지 않고 `LocalAdminSeedService`로 local 환경에서만 만든다.

### 4. 최초 관리자 계정은 일회성 setup token으로만 만든다

- 운영과 개발 공통으로 기본 관리자 계정을 migration이나 seed로 자동 생성하지 않는다.
- `admin_user` 테이블이 비어 있고 `ADMIN_SETUP_TOKEN`이 설정된 동안에만 `/api/v1/admin/setup`과 `/api/v1/admin/setup/status`를 연다.
- setup 경로는 관리자 인증 예외로 두되, `RateLimitFilter`의 `admin-setup-per-minute` 제한을 적용한다.
- setup token이 없거나 관리자 계정이 이미 있으면 엔드포인트는 `404`로 숨긴다.
- 계정을 만든 뒤에는 운영자가 즉시 `ADMIN_SETUP_TOKEN`을 제거한다.

### 5. 인증 외 운영 주제는 전용 ADR에서 본다

- requestId, 구조화 로그, 에러 추적: `ADR-0015`
- 처리율 제한: `ADR-0017`
- 비밀번호 저장 정책: `ADR-0019`
- 결제 외부 호출 보호: `ADR-0020`
- 외부 HTTP 클라이언트 설정: `ADR-0029`
- 타임아웃과 keep-alive: `ADR-0030`

---

## 결과

### 장점

- 인증과 세션 규칙을 한 문서에서 바로 확인할 수 있다.
- 최초 관리자 계정 생성 규칙이 운영 기준과 함께 정리된다.

### 단점

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
- `docs/PRD/0004_API_계약/spec.md`

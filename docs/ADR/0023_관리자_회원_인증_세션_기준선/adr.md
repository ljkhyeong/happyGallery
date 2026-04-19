# ADR-0023: 관리자/회원 인증 세션 기준선

**날짜**: 2026-03-17  
**상태**: Accepted

---

## 컨텍스트

기존 `docs/PRD/0001_기준_스펙/spec.md`에는 관리자 인증, 회원 세션, API Key 폴백, requestId, rate limit, 비밀번호 저장 정책이 한 문서에 함께 들어 있었다.

이 중 인증과 세션 저장 방식은 제품 요구사항보다 운영/보안 설계 기준에 가깝다. 반면 관측성, 처리율 제한, 비밀번호 정책, timeout 기준선은 이미 별도 ADR이 있거나 별도 기준선으로 관리하는 편이 읽기 쉽다.

그래서 이 ADR은 인증/세션 결정만 남기고, 나머지 운영 기준은 각 전용 ADR로 분리한다.

---

## 결정 사항

### 1. 관리자 인증의 기본 경로는 Redis 기반 Bearer 세션 토큰이다

- 관리자 로그인은 사용자명/비밀번호 기반으로 처리한다.
- 로그인 성공 시 UUID 세션 토큰을 발급한다.
- 이후 요청은 `Authorization: Bearer {token}` 헤더를 사용한다.
- 세션 저장소는 Redis 기반 `AdminSessionStore`를 사용한다.
- 키 패턴은 `admin:session:{token}` 이다.
- 세션 TTL은 8시간이다.

### 2. 회원 인증은 `HG_SESSION` 쿠키 + Spring Session + Redis를 기준으로 한다

- 회원 로그인/회원가입 성공 시 `HttpSession`에 `customerUserId`를 기록한다.
- 회원 세션 저장소는 Spring Session + Redis를 사용한다.
- 쿠키 이름은 기존 계약과 동일하게 `HG_SESSION`을 유지한다.
- 세션 네임스페이스는 `hg:session`, 기본 세션 만료는 7일이다.
- `CustomerAuthFilter`는 Spring Session filter 이후 실행되며, 세션의 `customerUserId`로 사용자를 다시 조회한다.
- 관리자 Bearer 세션과 회원 HTTP 세션은 서로 다른 인증 모델로 유지한다.

### 3. API Key는 로컬/테스트용 폴백으로만 허용한다

- 기본값은 `enable-api-key-auth=false`, `apiKey=""` 이다.
- 프로덕션에서 설정이 빠져도 API Key 경로는 비활성 상태를 유지한다.
- `local` 프로필에서만 `enable-api-key-auth=true`와 `ADMIN_API_KEY`를 명시적으로 설정한다.
- 기본 관리자 계정은 Flyway migration에 포함하지 않고, `LocalAdminSeedService`(`@Profile("local")`)로 local 환경에서만 seed한다.
- Bearer 세션 경로는 검증된 관리자 ID를 이력에 남기고, API Key 폴백 경로와 배치 이력은 `null`일 수 있다.

### 4. 최초 관리자 계정은 one-time setup token으로만 bootstrap 한다

- 운영/개발 공통으로 기본 관리자 계정을 migration이나 seed로 자동 생성하지 않는다.
- `admin_user` 테이블이 비어 있고 `ADMIN_SETUP_TOKEN`이 설정된 동안에만 `/api/v1/admin/setup`과 `/api/v1/admin/setup/status`를 연다.
- setup 경로는 `AdminAuthFilter` 인증 예외로 두되, `RateLimitFilter`의 `admin-setup-per-minute` 별도 버킷(기본 5/min)을 적용한다.
- setup 토큰이 비어 있거나 이미 관리자 계정이 하나 이상 존재하면 setup 엔드포인트는 `404`로 숨긴다.
- 성공적으로 계정을 만든 뒤에는 운영자가 즉시 `ADMIN_SETUP_TOKEN` 환경 변수를 제거한다.

### 5. 인증 외 운영 기준은 전용 ADR을 기준으로 본다

- requestId, JSON 로그, 에러 응답 추적은 `ADR-0015`와 API 계약 문서를 따른다.
- 필터 기반 처리율 제한은 `ADR-0017`을 따른다.
- 비밀번호 저장 정책은 `ADR-0019`를 따른다.
- 결제 외부 호출 보호는 `ADR-0020`을 따른다.
- Actuator 노출 정책, management port, timeout 계층, ingress keep-alive 기준선은 `ADR-0030`을 따른다.
- 외부 HTTP 클라이언트 풀링 기준선은 `ADR-0029`를 따른다.

---

## 결과

### 장점

- `ADR-0023`만 봐도 인증/세션 결정 범위를 바로 파악할 수 있다.
- 최초 관리자 bootstrap 경로가 운영 상수와 함께 한 문서에 정리된다.
- 인증 모델과 운영 기준 문서의 책임 경계가 분명해진다.
- 제목과 실제 내용이 더 잘 맞는다.

### 단점

- 운영 기준 전체를 이해하려면 관련 ADR을 함께 봐야 한다.
- 인증과 운영 기준이 섞여 있던 기존 문맥에 익숙한 경우 처음에는 링크를 따라가야 한다.

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
- `docs/Idea/0004_관리자_Auth_세션_확장/idea.md`
- `docs/PRD/0004_API_계약/spec.md`

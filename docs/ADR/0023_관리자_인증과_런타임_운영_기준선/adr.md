# ADR-0023: 관리자 인증 및 런타임 운영 기준선

**날짜**: 2026-03-17  
**상태**: Accepted

---

## 컨텍스트

기존 `docs/PRD/0001_기준_스펙/spec.md`에는 관리자 인증, 운영 관측성, 에러 응답 추적, rate limit, 비밀번호 저장 정책이 한 문서에 함께 들어 있었다.

이 내용은 제품 요구사항보다 운영/보안 설계 기준에 가깝다. 그래서 ADR로 분리한다.

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

- **기본값은 `enable-api-key-auth=false`, `apiKey=""`** 이다. 프로덕션에서 설정이 빠져도 API Key 경로는 비활성 상태를 유지한다.
- `local` 프로필에서만 `enable-api-key-auth=true`와 `ADMIN_API_KEY`를 명시적으로 설정한다.
- 기본 관리자 계정은 Flyway migration에 포함하지 않고, `LocalAdminSeedService`(`@Profile("local")`)로 local 환경에서만 seed한다.
- Bearer 세션 경로는 검증된 관리자 ID를 이력에 남기고, API Key 폴백 경로와 배치 이력은 `null`일 수 있다.

### 4. 운영 관측성은 requestId 중심으로 유지한다

- 타임존은 `Asia/Seoul` 고정이다.
- `prod` 프로필 로그는 JSON 구조로 출력한다.
- 요청 단위 추적을 위해 로그 필드에 `requestId`를 포함한다.
- 에러 응답은 가능하면 `requestId`를 함께 반환한다.
- 배치 실행은 `batch-{jobName}-{uuid8}` 형식의 requestId를 MDC에 주입한다.
- Actuator 웹 노출 정책은 `health`, `info`, `metrics`, `prometheus` 기준으로 유지한다.
  - management port를 기본 8081로 분리하여 application 트래픽과 격리한다.
  - `local` 프로필에서는 8080으로 유지한다.
  - `prod`에서 health details는 `never`

로그 레벨 전략:

- `TRACE`/`DEBUG`: `local`에서만 사용
- `INFO`: 운영 기본값
- `WARN`: 장애 전 단계 잠재 위험
- `ERROR`: 운영자 즉시 대응이 필요한 장애

### 5. 장애 대비 정책은 재시도 가능성을 우선한다

- 승인/거절/지연, 환불 실패/재시도, 예약 변경 이력은 감사 로그로 남긴다.
- 환불/알림은 실패 시 `FAILED` 상태를 남기고 운영자가 재시도할 수 있게 유지한다.
- 외부 결제(PG) 호출은 `CircuitBreaker + Timeout`으로 보호한다.
  - 현재 기본 타임아웃은 3초
- 외부 알림/Google OAuth 호출은 downstream별 HTTP connection pool을 분리한다.
  - 세부 기준선은 `ADR-0029`에서 관리한다.
- timeout 기준선은 바깥 계층이 더 길고 안쪽 계층이 더 짧게 잡히도록 유지한다.
  - 현재 기본값은 `frontend 35s > nginx read 30s > transaction 10s > DB query 5s > lock wait 3s > DB/Hikari acquire 2s` 순서를 따른다.
  - 동기 MVC 전체 요청 deadline은 별도 필터/컨테이너 커스터마이저 후보로 남겨 둔다.
- 필터 기반 처리율 제한을 `/api/v1/**` 기준으로 적용한다.
  - 저장소는 Redis 공유 카운터를 사용한다. 다중 인스턴스에서도 같은 제한값을 본다.
  - 인증코드 발송: 10 req/sec/IP
  - 게스트 예약 생성: 30 req/min/IP
  - 이용권 구매: 20 req/min/IP
  - 회원 로그인: 10 req/min/IP
  - 회원 회원가입: 5 req/min/IP
  - 관리자 로그인: 5 req/min/IP
  - Admin API: 120 req/min/IP

### 6. 비밀번호 저장과 에러 처리 구현은 공통 계층에서 유지한다

- DB에는 평문 비밀번호를 저장하지 않는다.
- 단순 해시(SHA-256/MD5) 단독 사용을 금지한다.
- Salt + Key Stretching 기반 알고리즘만 허용한다.
- 기본 구현은 Spring Security `PasswordEncoder`를 사용한다.
- 운영 로그에 비밀번호와 해시 원문을 출력하지 않는다.

현재 구현 위치:

- `ErrorCode` enum — `common/error/ErrorCode.java`
- `HappyGalleryException` — `common/error/HappyGalleryException.java`
- `ErrorResponse` record — `common/error/ErrorResponse.java`
- 개별 예외 클래스 — `common/error/`
- `GlobalExceptionHandler` — `app/web/GlobalExceptionHandler.java`

---

## 결과

### 장점

- core PRD에서 운영/보안 구현 기준을 걷어낼 수 있다.
- 관리자/회원 세션 저장 전략과 런타임 정책을 별도 ADR에서 관리할 수 있다.
- requestId, 에러 응답, rate limit, password 정책의 기준 문서가 더 분명해진다.

### 단점

- 운영 기준을 이해하려면 여러 ADR을 함께 확인해야 한다.
- rate limit과 timeout 같은 수치는 향후 조정 시 ADR/설정 동기화가 필요하다.

---

## 참고 문서

- `docs/ADR/0015_Observability_로깅과_비즈니스_예외/adr.md`
- `docs/ADR/0016_API_버전_전략/adr.md`
- `docs/ADR/0017_Filter_처리율_제한/adr.md`
- `docs/ADR/0019_비밀번호_해시_정책/adr.md`
- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
- `docs/Idea/0013_회원_세션_Spring_Session_전환_검토/idea.md`
- `docs/Idea/0015_다중_인스턴스용_Redis_도입/idea.md`
- `docs/Idea/0004_관리자_Auth_세션_확장/idea.md`
- `docs/PRD/0004_API_계약/spec.md`

# ADR-0017: 필터 기반 API 처리율 제한 도입

**날짜**: 2026-03-06  
**상태**: Accepted

---

## 컨텍스트

무제한 API 호출을 허용하면 특정 IP나 봇 트래픽이 애플리케이션과 DB 자원을 고갈시킬 수 있다.
그러면 정상 사용자 요청도 함께 장애 영향을 받는다.

특히 인증코드 발송, 회원 로그인/회원가입, 결제 준비/확정, 관리자 운영 API는 짧은 시간에 대량 호출될 가능성이 높다. 선제적인 요청 제한이 필요하다.

처음에는 서버 메모리 기반 제한도 가능했다.
하지만 지금 운영 환경은 Redis를 사용하므로 인스턴스 간 카운터를 공유하는 방식을 기준으로 잡는다.

---

## 결정 사항

### 1. Interceptor가 아닌 Servlet Filter 계층에서 제한한다

- 요청 초기 단계에서 차단해 컨트롤러/서비스/DB 진입 전에 자원을 보호한다.
- 필터 순서:
  - `RequestIdFilter` → `RateLimitFilter` → `AdminAuthFilter`

### 2. Redis 공유 카운터 기반 fixed-window 정책을 적용한다

- 키: 기본적으로 클라이언트 IP(`remoteAddr`)
- `app.rate-limit.trust-forwarded-headers=true` 일 때만 `X-Forwarded-For`의 첫 번째 IP를 신뢰한다.
- `app.rate-limit.enabled=false`이면 필터가 제한 계산을 건너뛴다. 기본값은 `true`이며, 로컬 반복 E2E처럼 같은 IP에서 짧게 많은 인증/관리 요청을 보내는 검증에서만 끈다.
- Redis 키 패턴은 `rate:{RULE_ID}:{clientIP}` 를 사용한다.
- 기본 한도:
  - 인증코드 발송: 10 req/sec/IP
  - 회원 로그인: 10 req/min/IP
  - 회원 회원가입: 5 req/min/IP
  - 게스트 예약 생성: 30 req/min/IP (구 생성 API 기준, 결제 전환 후 `/payments/prepare`로 이전 필요)
  - 이용권 구매: 20 req/min/IP (구 생성 API 기준, 결제 전환 후 `/payments/prepare`로 이전 필요)
  - 관리자 로그인: 5 req/min/IP
  - Admin API: 120 req/min/IP

### Update (2026-04-26)

주문/예약/8회권 생성은 `POST /api/v1/payments/prepare` -> `POST /api/v1/payments/confirm`로 전환됐다. 현재 필터 구현에는 아직 구 `BOOKING_CREATE`, `PASS_PURCHASE` 경로 규칙이 남아 있으므로 `plan.md`의 `P1R-T2`에서 결제 API 기준 rate limit으로 이전한다.

### 3. Redis 증분과 TTL 설정은 Lua script로 원자적으로 처리한다

- `INCR`와 최초 `EXPIRE`를 하나의 Lua script로 실행한다.
- count가 1일 때만 TTL을 설정해 윈도우를 시작한다.
- 별도 인메모리 bucket 상태나 cleanup 스케줄러는 두지 않는다.

### 4. 제한 초과 시 표준 에러 응답을 반환한다

- HTTP `429 TOO_MANY_REQUESTS`
- 본문: `{"code":"TOO_MANY_REQUESTS","message":"..."}`
- 헤더: `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 대량 호출을 조기에 차단해 서버/DB 보호 효과가 크다 |
| 장점 | Redis 공유 카운터로 다중 인스턴스 환경에서도 같은 제한값을 적용할 수 있다 |
| 장점 | 운영자가 `429` 지표로 비정상 트래픽을 빠르게 탐지할 수 있다 |
| 단점 | 과도하게 낮은 제한값은 정상 사용자 UX에 영향을 줄 수 있다 |
| 단점 | Redis 장애 시 제한 처리 경로도 영향을 받는다 |
| 대응 | 제한값을 `application.yml`로 외부화해 환경별 튜닝 |

---

## 구현 반영

- `adapter-in-web/.../RateLimitFilter` 추가
- `adapter-in-web/.../RequestIdFilter`, `adapter-in-web/.../AdminAuthFilter` 필터 순서/경로 보강
- `StringRedisTemplate` 기반 Redis 카운터 사용
- Lua script로 `INCR` + `EXPIRE` 원자 처리
- `domain/error/ErrorCode`에 `TOO_MANY_REQUESTS` 추가
- `bootstrap/src/main/resources/application.yml`에 `app.rate-limit.*` 설정 추가
- `CUSTOMER_LOGIN`, `CUSTOMER_SIGNUP`, `ADMIN_LOGIN` 한도와 `trust-forwarded-headers` 정책 추가

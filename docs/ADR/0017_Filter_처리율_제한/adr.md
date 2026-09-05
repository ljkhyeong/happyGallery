# ADR-0017: 애플리케이션 처리율 제한 기준

**날짜**: 2026-03-06  
**최종 갱신**: 2026-08-08
**상태**: Accepted

---

## 컨텍스트

무제한 API 호출은 애플리케이션, DB와 외부 PG 자원을 고갈시켜 정상 요청까지 지연시킨다. 인증 코드 발송, 로그인, 결제, 비밀번호 확인과 쓰기 API는 호출 비용이나 남용 위험이 특히 크다.

운영 환경은 외부 WAF에 의존하지 않는 단일 노트북 Kubernetes 자가 호스팅을 목표로 한다. 따라서 ingress의 보조 제한 여부와 관계없이 애플리케이션 자체가 모든 공개 API에 최소 보호선을 제공해야 한다.

## 결정

### 1. IP 제한은 Servlet Filter에서 조기에 적용한다

- 필터 순서는 `RequestIdFilter -> Spring Security HeaderWriterFilter -> RateLimitFilter -> 나머지 Security FilterChain`이다.
- `RequestIdFilter`는 모든 응답에 추적 ID를 남기기 위해 Servlet 바깥 필터로 유지한다.
- `RateLimitFilter`는 Servlet 자동 등록을 끄고 두 `SecurityFilterChain`에 명시적으로 등록한다.
  이 위치에서는 컨트롤러와 외부 호출 전에 요청을 차단하면서도 `429`·`503` 단락 응답에 공통 보안 헤더가 적용된다.
- matcher, 실패 모드와 설정 선택자를 하나의 순서 있는 규칙 목록으로 관리한다.
  구체 경로 규칙을 먼저 확인하고, 마지막에 `/api/v1/**`의 `DEFAULT_API_IP` 규칙을 적용한다.
- `/actuator/**`, 정적 파일과 Kubernetes health probe는 `/api/v1/**` 밖에 두어 사용자 트래픽 버킷과 분리한다.
- `app.rate-limit.enabled=false`는 로컬 반복 E2E처럼 동일 IP 요청이 집중되는 검증에서만 사용한다.

기본 IP 규칙은 다음과 같다.

| 규칙 | 대상 | 한도 |
| --- | --- | --- |
| `DEFAULT_API_IP` | 나머지 `/api/v1/**` | 300회/1분 |
| `PHONE_VERIFICATION_IP` | 인증 코드 발송 | 5회/1분 |
| `EMAIL_VERIFICATION_IP` | 회원 이메일 인증 코드 발송·등록 | 5회/1분 |
| `CUSTOMER_LOGIN_IP` | 회원 로그인 | 10회/1분 |
| `CUSTOMER_SIGNUP_IP` | 회원가입 | 5회/1분 |
| `SOCIAL_LOGIN_IP`, `SOCIAL_LOGIN_INIT_IP` | 소셜 callback, 로그인 시작 | 각각 10회/1분 |
| `ADMIN_LOGIN_IP`, `ADMIN_SETUP_IP` | 관리자 로그인, 최초 설정 | 각각 5회/1분 |
| `ADMIN_API_IP` | 나머지 관리자 API | 120회/1분 |
| `PAYMENT_PREPARE_IP` | 결제 준비 | 30회/1분 |
| `PAYMENT_CONFIRM_IP` | 결제 확정 | 60회/1분 |
| `GUEST_CLAIM_VERIFY_IP` | 비회원 이력 연결 인증 | 10회/1분 |
| `CLIENT_MONITORING_IP` | 클라이언트 이벤트 수집 | 60회/1분 |

결제 prepare와 confirm은 서로 다른 버킷을 사용한다. prepare 트래픽이 confirm의 멱등 재시도를 차단해서는 안 되기 때문이다.

### 2. 본문과 인증 주체가 필요한 제한은 검증 이후 적용한다

Filter는 request body를 읽지 않는다. 본문이나 인증 주체가 필요한 제한은 `@Valid` DTO와
`@AuthenticationPrincipal` 처리 뒤의 웹 진입점 또는 공통 application 오케스트레이션 경계에서 적용한다.
인증 코드 소비 시도는 회원가입, 예약·주문, 계정 복구와 결제 상태 복구가 같은
application 검증 경계를 사용하므로 컨트롤러마다 반복하지 않는다.
`PhoneVerificationAttemptGuard`, `EmailVerificationRateLimitGuard`,
`AdminMfaRecoveryAttemptGuard` 출력 포트로 검사하고
composition root인 `bootstrap`이 `SubjectRateLimitGuard`의 해당 동작을 포트 빈으로 결선한다. application은 웹 어댑터를 알지
않고 웹 어댑터도 application `port.out`을 직접 import하지 않는다.
회원 전화번호 변경, 비밀번호 재설정, 비회원 이력 귀속과 비회원 결제 prepare는
비트랜잭션 오케스트레이터에서 이 Redis 제한을 먼저 확인한다. 인증 코드 잠금 소비와 보호 대상 변경 또는
`PaymentAttempt` 저장은 그 뒤의 짧은 DB 트랜잭션에 함께 두어, Redis 지연이 사용자 행 잠금이나
DB connection을 점유하지 않게 한다.
관리자 MFA 초기화는 세션의 `RECOVERY_CODE` 인증 수단을 먼저 확인한 뒤 관리자 ID 버킷을 소비하고,
그 다음 별도 트랜잭션에서 관리자 행 잠금과 현재 비밀번호 BCrypt 확인을 수행한다. TOTP·비밀번호·local API key
세션은 이 버킷을 소비하지 않아 인증되지 않은 요청이 관리자별 복구 버킷을 소진하지 못한다.

| 규칙 | 식별 기준 | 한도 |
| --- | --- | --- |
| `CUSTOMER_LOGIN_EMAIL` | 정규화된 회원 로그인 이메일 | 10회/10분 |
| `PHONE_VERIFICATION_PHONE` | 정규화된 전화번호 | 3회/10분 |
| `PHONE_VERIFICATION_ATTEMPT_PHONE` | 모든 업무에서 인증 코드를 소비하려는 정규화된 전화번호 | 5회/10분 |
| `EMAIL_VERIFICATION_USER`, `EMAIL_VERIFICATION_EMAIL` | 발송을 요청한 회원 ID와 정규화 이메일 | 각각 3회/10분 |
| `EMAIL_VERIFICATION_ATTEMPT_USER`, `EMAIL_VERIFICATION_ATTEMPT_EMAIL` | 등록을 시도한 회원 ID와 정규화 이메일 | 각각 5회/10분 |
| `PAYMENT_CONFIRM_ORDER` | 외부 주문번호 | 20회/1분 |
| `GUEST_CLAIM_USER` | 회원 ID | 5회/1분 |
| `ADMIN_MFA_RECOVERY_USER` | 복구 코드로 인증한 관리자 ID | 5회/10분 |

### 3. Redis fixed window 카운터를 공유한다

- `RedisRateLimiter`가 IP와 subject 제한의 Lua `INCR + 최초 PEXPIRE`를 공통 처리한다.
- 키는 `{key-prefix}:{RULE_ID}:{HMAC(subject)}` 형식이다. IP, 이메일, 전화번호, 주문번호와 회원 ID 원문을 Redis 키나 로그에 남기지 않는다.
- 기본 prefix는 `happygallery:rate`이며 배포 환경에서 `RATE_LIMIT_KEY_PREFIX`로 분리할 수 있다.
- Redis 연결과 명령 대기 상한은 각각 1초다. 장애와 복구 상태 전환에만 규칙 ID와 예외 유형을 기록하고, 개별 초과 요청은 WARN 로그로 남기지 않는다.
- `DEFAULT_API_IP`와 결제 confirm IP·주문번호 규칙은 Redis 장애 시 fail-open한다. 일반 조회 가용성과 이미 시작한 결제의 멱등 재시도를 우선한다.
- 로그인·가입·비밀번호 재설정·관리자 로그인과 MFA 복구·휴대폰/이메일 인증 코드·비회원 이력 인증·모니터링 수집과 payment prepare는 fail-closed하고 `503 SERVICE_UNAVAILABLE`을 반환한다. 장바구니 결제도 표준 payment prepare 버킷을 사용한다. edge 방어가 없는 상태에서 Redis 장애가 고위험 경로의 무제한 허용으로 바뀌지 않게 한다.
- 관리자 로그인은 `ADMIN_LOGIN_IP`와 MFA·감사 이력을 사용한다. 인증되지 않은 공격자가 공유 사용자명 버킷이나 계정 실패 횟수를 소진해 정상 운영자를 막을 수 있으므로 사용자명 전역 hard lock이나 공유 subject 버킷은 두지 않는다.
- 인메모리 fallback은 pod마다 카운터가 갈리므로 두지 않는다.

### 4. 전달 헤더는 서버의 단일 신뢰 경계에서만 해석한다

- 처리율 제한은 항상 `request.getRemoteAddr()`를 사용하고 `X-Forwarded-For`를 직접 파싱하지 않는다.
- 기본값은 `FORWARD_HEADERS_STRATEGY=none`이다.
- ingress가 외부의 전달 헤더를 제거하고 실제 값으로 덮어쓰며, Service/방화벽으로 애플리케이션 직접 접근을 차단한 뒤에만 `FORWARD_HEADERS_STRATEGY=native`로 Tomcat의 전달 헤더 처리를 활성화한다.
- 프록시 헤더 신뢰 여부를 애플리케이션 처리율 제한 설정에 중복하지 않아 두 설정이 어긋나는 우회를 막는다.
- ingress 처리율 제한은 추가 방어선일 뿐 이 ADR의 애플리케이션 제한을 대체하지 않는다.

### 5. 초과 응답 계약을 통일한다

- HTTP `429 TOO_MANY_REQUESTS`
- 에러 코드 `TOO_MANY_REQUESTS`
- `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining` 헤더
- `Duration` 설정은 밀리초 정밀도로 Redis TTL에 전달하고 `Retry-After`만 올림한 초 단위 정수로 반환한다.
- IP 제한은 필터가 직접 응답하고 subject 제한은 `GlobalExceptionHandler`가 같은 계약으로 응답한다.
- fail-closed 규칙의 Redis 장애는 `503 SERVICE_UNAVAILABLE`과 `Retry-After: 1`을 반환한다.
- 필터가 직접 끝내는 응답에도 Spring Security의 공통 보안 헤더를 적용한다.

## 결과

### 장점

- edge 인프라가 없어도 모든 공개 API가 최소 보호를 받는다.
- 비용이 큰 경로와 특정 수신자·결제·회원 대상 남용을 더 엄격하게 제한한다.
- 다중 pod에서도 Redis 카운터와 HMAC 키 계약이 일관된다.
- 필터가 본문을 소비하지 않아 JSON 이중 파싱과 request stream 문제를 피한다.

### 단점과 대응

- fixed window 경계에서는 순간 호출이 몰릴 수 있다. 실제 운영 지표를 보고 sliding window가 필요한 규칙만 별도로 검토한다.
- fail-open 경로는 Redis 장애 중 제한이 적용되지 않는다. ingress 제한을 보조선으로 구성하고 Redis 장애를 모니터링한다.
- 낮은 한도는 정상 사용을 막을 수 있다. `app.rate-limit.ip.*`, `subject.*` 설정을 ConfigMap으로 조정한다.
- 전화번호·이메일 수신자 버킷은 특정 대상의 발송 비용을 보호하지만 제3자가 한도를 소진하면 정상 사용자도 최대 10분 지연될 수 있다. 회원 이메일 경로는 회원 ID 버킷도 함께 사용하고, 실제 남용이 관측되면 CAPTCHA 또는 발송 전 challenge를 추가한다.

## 구현

- `adapter-in-web/.../RateLimitFilter`
- `adapter-in-web/.../ratelimit/RedisRateLimiter`
- `adapter-in-web/.../ratelimit/SubjectRateLimitGuard`
- `adapter-in-web/.../config/properties/RateLimitProperties`
- `application/.../customer/port/out/PhoneVerificationAttemptGuard`
- `application/.../customer/port/out/EmailVerificationRateLimitGuard`
- `bootstrap/.../config/RateLimitPortConfig`
- `bootstrap/src/main/resources/application.yml`

## 참고

- [ADR-0027 테스트 전략](../0027_테스트_전략과_최소_테스트_세트_기준선/adr.md)
- [ADR-0036 개인정보와 블라인드 인덱스](../0036_개인정보_평문_제거와_블라인드_인덱스_기준/adr.md)
- [ADR-0037 자가 호스팅 배포 토폴로지](../0037_자가_호스팅_배포_토폴로지_기준/adr.md)

- 회원·비회원 배송지 수정 PUT도 `ORDER_CUSTOMER_ACTION_IP`를 적용한다. 비회원 경로는 CSRF 확인 후 조회 코드로 소유권을 검증한다.

- `GROUP_INQUIRY_IP`는 비회원 `/api/v1/group-inquiries`와 회원 `/api/v1/me/group-inquiries`의 POST를 동일 IP 버킷 5회/10분으로 제한한다. `app.rate-limit.ip.group-inquiry`에서 조정한다. 외부 채널 문의를 입력하는 관리자 API는 기존 관리자 한도를 사용한다. 공개 접수는 Redis 장애 시 fail-closed한다.

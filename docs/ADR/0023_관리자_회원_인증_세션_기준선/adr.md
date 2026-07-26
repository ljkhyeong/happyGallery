# ADR-0023: 관리자·회원 인증과 세션 운영 기준

**날짜**: 2026-03-17  
**최종 갱신**: 2026-07-26
**상태**: Accepted

---

## 왜 이 문서가 필요한가

관리자 인증, 회원 세션, 로컬 API key, 최초 관리자 계정 생성 방식은 제품 요구사항보다 운영과 보안에 가까운 주제다.  
이 문서는 인증과 세션만 따로 묶어 현재 운영 기준을 정리한다.

---

## 결정

### 1. Spring Security 체인을 관리자와 회원·공개 요청으로 분리한다

- Spring Boot 4.0.7 기준 `spring-boot-starter-security`를 사용한다.
- 관리자 경로와 회원·공개 경로는 서로 다른 `SecurityFilterChain`이 처리한다.
- 관리자 체인은 서버 HTTP 세션을 만들지 않고 Redis Bearer 세션 또는 local API key로 인증한다.
- 회원·공개 체인은 `HG_SESSION`에서 회원 ID를 읽어 요청 범위의 회원 principal과 `SecurityContext`를 구성한다.
- `RequestIdFilter`와 `RateLimitFilter`는 인증 여부와 무관하게 모든 요청에 적용되도록 Security 체인 앞단에 유지한다.
- 컨트롤러는 Spring Security의 `@AuthenticationPrincipal`로 `AdminPrincipal` 또는 `CustomerPrincipal`을 직접 주입받고 필요한 ID를 애플리케이션 유스케이스에 전달한다. 회원 인증이 선택인 결제·클라이언트 모니터링 API는 nullable `CustomerPrincipal`로 게스트를 구분한다. `/api/v1/me`는 필터가 이미 조회한 회원 응답 스냅샷을 principal에서 재사용해 중복 조회를 피한다.

### 2. 관리자 인증은 Redis 기반 Bearer 세션을 기본으로 한다

- 로그인은 사용자명/비밀번호 기반이다.
- 존재하지 않는 사용자명도 고정된 dummy BCrypt 해시를 확인하고, 잘못된 비밀번호·잠긴 계정·잘못된 MFA에는 모두 같은 `401 INVALID_CREDENTIALS`를 반환한다. 응답으로 계정 존재 여부나 잠금 상태를 구분하지 않는다.
- 비밀번호 또는 MFA가 연속 5회 실패하면 15분간 계정을 잠근다. 정상 인증이 완료되면 실패 횟수와 만료된 잠금을 명시적으로 초기화해 저장한다.
- MFA가 비활성화된 계정은 비밀번호 확인 뒤, 활성화된 계정은 MFA 확인까지 끝난 뒤에만 UUID 세션 토큰을 발급한다.
- 이후 요청은 `Authorization: Bearer {token}` 헤더를 사용한다.
- 세션 저장소는 Redis 기반 `AdminSessionStore`
- 키 패턴은 `admin:session:{tokenHmac}`이며 원문 Bearer 토큰을 Redis 키에 남기지 않는다.
- 세션 TTL은 8시간
- 세션 값 저장, 관리자·자격 버전 인덱스 추가, 두 키의 TTL 설정은 Redis Lua 한 번으로 실행한다.
  중간 명령이 실패하면 새 세션과 새 인덱스 항목을 정리해 부분 생성 상태를 남기지 않는다.
- `admin_user.credential_version`과 세션 발급 당시 버전을 매 요청 비교한다. 비밀번호 또는 MFA 설정 변경이 커밋되면 이전 버전 세션은 Redis 삭제 성공 여부와 무관하게 인증할 수 없다.
- 관리자·자격 증명 버전별 Redis 세션 인덱스는 비밀번호 또는 MFA 설정 변경 커밋 후 이전 버전 키를 일괄 삭제하는 정리 용도다. 변경 후 새 자격 증명으로 발급한 세션과 경합하지 않으며, 삭제 실패는 기록하되 이미 커밋된 변경을 실패 응답으로 되돌리지 않는다.
- 관리자 로그인과 최초 계정 setup 경로는 인증 없이 호출할 수 있고, 그 외 관리자 경로는 관리자 principal이 필요하다.
- 인증 정보가 없거나 유효하지 않으면 `401`, 인증은 됐지만 권한이 부족하면 `403`을 기존 `ErrorResponse` JSON 형식으로 반환한다.
- 로그인 성공·실패·잠금과 MFA 요구·실패·활성화·비활성화·복구 코드 사용은 `admin_auth_history`에 기록한다. 입력 사용자명과 challenge 원문은 저장하지 않고 HMAC만 남기며, 이력은 개인정보 보존 배치에서 180일 뒤 삭제한다.

### 2.1. 관리자 MFA는 TOTP와 일회용 복구 코드를 사용한다

- MFA 등록·확인·해제는 계정 ID가 있는 관리자 Bearer 세션에서만 허용한다. local API key는 MFA 설정을 바꿀 수 없다.
- 등록 시작은 TOTP 비밀키와 `otpauth` URI를 한 번 응답한다. 서버는 비밀키를 필드 AES-GCM 암호문으로만 저장하며, 등록 코드 확인 전에는 MFA가 활성화되지 않는다.
- TOTP는 SHA-1, 6자리, 30초 주기와 앞뒤 1주기 허용 범위를 사용한다. 로그인 challenge는 원문 대신 HMAC을 저장하고 5분 뒤 만료되며, 성공 시 한 번만 소비한다.
- 등록 확인 시 `xxxx-xxxx-xxxx-xxxx` 형식의 복구 코드 10개를 한 번만 응답한다. 서버는 각 코드를 무작위 salt가 있는 `PasswordEncoder` 해시로만 저장하고, 한 번 사용한 코드는 다시 사용할 수 없다.
- 6자리 숫자만 TOTP 검증기에 전달하고 복구 코드 형식이 맞는 입력만 복구 코드 해시와 비교한다. 형식이 다른 입력에 불필요한 외부 검증이나 반복 BCrypt 비교를 수행하지 않는다.
- MFA 활성화·비활성화는 `credential_version`을 증가시키고 기존 관리자 세션을 모두 무효화한다. 비활성화에는 현재 비밀번호와 유효한 TOTP 또는 미사용 복구 코드가 모두 필요하다.
- 인증 앱과 모든 복구 코드를 함께 잃은 경우의 자동 계정 복구는 현재 지원하지 않는다. 복구 코드는 MFA 등록 직후 별도 오프라인 장소에 보관해야 한다. `ADMIN_SETUP_TOKEN`을 재사용하거나 DB에서 MFA 컬럼을 직접 변경해 우회하지 않으며, 접근 상실 시에는 별도 검토·승인을 거친 오프라인 복구 절차를 새 배포로 마련하기 전까지 관리자 접근이 불가능한 운영 위험을 수용한다.

### 3. 회원 인증은 `HG_SESSION` 쿠키 + Spring Session + Redis를 사용한다

- 로그인/회원가입 성공 시 `HttpSession`에 `customerUserId`, `customerCredentialVersion`, `userId:credentialVersion` 형식의 Spring Session principal 인덱스를 기록한다.
- 세션 저장소는 회원·자격 증명 버전별 세션 조회를 지원하는 Spring Session `RedisIndexedSessionRepository`를 사용한다.
- 쿠키 이름은 `HG_SESSION`을 유지한다.
- 세션 네임스페이스는 `hg:session`, 기본 만료는 7일이다.
- 회원 인증이 필요하거나 선택적으로 사용되는 요청마다 `customerUserId`에 해당하는 회원을 확인하고 DB의 `credential_version`과 세션의 `customerCredentialVersion`이 같을 때만 회원 principal과 `SecurityContext`를 구성한다.
- `/api/v1/me/**`는 회원 principal이 필요하고, 결제·클라이언트 모니터링처럼 회원 인증이 선택인 API는 세션이 있을 때만 회원 principal을 사용한다.
- 로그인·회원가입·소셜 로그인 성공 시 세션 ID를 회전하고 회원 ID·자격 증명 버전·principal 인덱스를 저장한다. OAuth authorization request는 callback에서 소비하며 로그인 이후 유지하지 않는다.
- 비밀번호 변경·재설정은 `users.credential_version` 증가를 보안상 성공 기준으로 삼는다. 변경 전 버전을 이벤트에 담고 DB 커밋 뒤 해당 `userId:credentialVersion` 인덱스의 Redis 세션만 일괄 삭제한다. 변경 후 새 비밀번호로 발급된 세션은 새 버전 인덱스에 있으므로 동시 삭제하지 않는다. 삭제 실패는 로그와 `happygallery.customer.session.revocation_failed` 메트릭으로 남긴다.
- Redis 삭제가 실패하거나 삭제와 동시에 진행 중이던 요청이 세션을 다시 저장해도, 다음 요청의 자격 증명 버전 비교에서 이전 세션을 즉시 폐기한다. 비밀번호를 바꾼 현재 요청 세션은 응답 종료 시 다시 저장되지 않도록 `HttpSession.invalidate()`를 별도로 호출한다.
- `users.version` 낙관적 락을 최종 stale update 방어선으로 사용한다. 비밀번호·휴대폰 확인과 같은 회원 변경은 행 잠금으로 직렬화하고, 로그인과 기존 소셜 로그인도 최근 로그인 시각을 갱신하므로 같은 잠금 조회를 사용한다.
- Indexed session의 만료·principal 인덱스 정리를 위해 self-hosted Redis는 `notify-keyspace-events=Egx`로 실행한다. 저장소가 관리형 Redis로 바뀌면 Spring의 `CONFIG` 권한을 허용하거나 서버에서 같은 값을 사전 설정해야 한다.
- 관리자 Bearer 세션과 회원 HTTP 세션은 분리 유지한다.

### 4. 회원 쿠키 인증에는 SPA CSRF 보호를 적용한다

- 회원·공개 체인은 Spring Security의 SPA CSRF 구성을 사용한다.
- 클라이언트는 `GET /api/v1/auth/csrf`로 `XSRF-TOKEN` 쿠키를 발급받고, 상태를 변경하는 요청에 같은 값을 `X-XSRF-TOKEN` 헤더로 보낸다.
- 로그인과 로그아웃은 기존 CSRF 토큰을 폐기하므로 클라이언트가 다음 상태 변경 요청 전에 토큰을 다시 발급받는다.
- 관리자 체인은 브라우저 쿠키가 아니라 명시적인 Bearer/API key 헤더로 인증하므로 CSRF 검사 대상에서 제외한다.

### 5. 기존 ETag와 명시적 캐시 정책을 보존한다

- Spring Security의 기본 cache-control writer는 비활성화한다.
- 공개 상품·클래스·공지 API의 ETag와 `304 Not Modified` 계약을 유지한다.
- 소셜 로그인 시작·callback 응답에는 `Cache-Control: no-store`를 명시한다.
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
- 생성 트랜잭션은 `admin_setup_lock` 단일 행을 비관적 잠금한 뒤 관리자 존재 여부를 다시 확인한다. 서로 다른 사용자명으로 동시 요청해도 최초 한 건만 생성된다.
- 계정을 만든 뒤에는 운영자가 즉시 `ADMIN_SETUP_TOKEN`을 제거한다.

### 8. OAuth2 Client는 로그인 프로토콜만 담당한다

- `spring-boot-starter-oauth2-client`가 Google/Naver authorization request, `state`, code 교환과 UserInfo/OIDC 검증을 담당한다.
- 시작 경로는 `/api/v1/auth/social/authorization/{provider}`, backend callback은 `/api/v1/auth/social/callback/{provider}`로 고정한다.
- authorization request와 `state`는 callback 전까지만 Spring Session Redis에 저장하고 검증 후 제거한다.
- 제공자 인증이 끝난 뒤 application에는 `provider`, `providerId`, `email`, `name`만 전달한다. 소셜 계정 조회·이메일 충돌·회원 생성 정책은 application이 계속 소유한다.
- OAuth 인증 `SecurityContext`와 authorized client의 access/refresh token은 저장하지 않는다. 로그인 성공 후 장기 인증 상태는 `customerUserId`와 `customerCredentialVersion`만 사용한다.
- Spring Security의 기본 세션 고정 보호는 customer 체인에서 중복 적용하지 않고, 모든 회원 인증 성공 경로가 `CustomerSessionBinder`에서 세션 ID를 한 번 회전한다.
- 기존 Google/Naver별 Apache HttpClient 연결 풀과 acquire/connect/read timeout은 token·UserInfo 호출에도 유지한다.

### 9. 현재 필요하지 않은 나머지 Security 기능은 도입하지 않는다

- 회원은 Spring Session, 관리자는 즉시 폐기 가능한 opaque Redis 세션을 사용하므로 JWT와 OAuth2 Resource Server를 도입하지 않는다. 별도 인증 서버와 여러 Resource Server가 생길 때 재검토한다.
- 현재 역할은 `CUSTOMER`, `ADMIN` 두 개이고 리소스 소유권은 application 조회·변경 유스케이스에서 검증하므로 `@EnableMethodSecurity`, `@PreAuthorize`를 중복 적용하지 않는다. 직원별 세부 권한이나 HTTP 외 진입점의 공통 권한 요구가 생길 때 재검토한다.
- 프런트와 API는 Vite proxy와 운영 ingress 모두 same-origin이므로 CORS 허용 정책을 추가하지 않는다. origin을 분리할 때 exact allowlist와 credential 정책을 함께 도입한다.
- 평상시 동시 로그인 수 제한은 회원 다중 기기 정책이 정해질 때까지 보류한다. 다만 관리자 비밀번호 변경과 회원 비밀번호 변경·재설정은 보안 경계이므로 각각 기존 세션을 모두 무효화한다. custom 관리자 Bearer 저장소에는 `maximumSessions()`를 직접 적용하지 않는다.
- remember-me는 별도 로그인 유지 UX가 없고 회원 세션 TTL이 7일이므로 도입하지 않는다. 브라우저 재시작 후 유지 요구가 생기면 persistent session cookie와 별도 remember token을 먼저 비교한다.
- ACL은 단일 회원 소유 또는 전체 관리자 접근 모델에 비해 저장소·캐시·동기화 비용이 크므로 사용하지 않는다. 여러 사용자·그룹이 한 리소스의 권한을 공유할 때만 재검토한다.

### 10. OAuth state와 redirect URI는 서버가 소유한다

- Google/Naver callback URI는 provider별 `GOOGLE_OAUTH_REDIRECT_URI`, `NAVER_OAUTH_REDIRECT_URI` 설정값과 정확히 일치해야 한다.
- 브라우저가 `redirectUri`를 보내지 않는다. Spring의 `ClientRegistration`이 고정 callback URI를 authorization·token 요청에 동일하게 사용한다.
- Spring Security가 만든 authorization request와 `state`를 같은 HTTP 세션에 저장하고 callback에서 일치 여부를 확인한 뒤 한 번에 제거한다.
- Naver token 요청에 필요한 `state`는 외부 callback 파라미터를 다시 신뢰하지 않고 세션에서 복원한 authorization request의 값을 사용한다.

### 11. 인증 외 운영 주제는 전용 ADR에서 본다

- requestId, 구조화 로그, 에러 추적: `ADR-0015`
- 처리율 제한: `ADR-0017`
- 비밀번호 저장 정책: `ADR-0019`
- 결제 외부 호출 보호: `ADR-0020`
- 외부 HTTP 클라이언트 설정: `ADR-0029`
- 타임아웃과 keep-alive: `ADR-0030`
- Actuator 관리 포트와 ingress·NetworkPolicy 노출 경계: `ADR-0037`

---

## 결과

### 장점

- URL 인가와 요청별 인증 표현을 Spring Security 표준으로 관리한다.
- 관리자와 회원의 서로 다른 세션 계약을 유지하면서 인증 경계를 분리한다.
- 회원 쿠키 인증에 CSRF 보호와 로그인 시 세션 ID 회전을 적용한다.
- OAuth 프로토콜 구현을 표준화하면서도 서비스 회원과 장기 세션 소유권은 기존 application 경계에 유지한다.
- 최초 관리자 계정 생성 규칙이 운영 기준과 함께 정리된다.
- 관리자 비밀번호 대입 공격과 2단계 인증 우회 경계를 서버 상태와 감사 이력으로 통제한다.

### 단점

- 같은 애플리케이션 안에서 관리자·회원 체인을 함께 운영하므로 경로가 추가될 때 적용 체인을 확인해야 한다.
- 상태 변경 API를 호출하는 클라이언트는 CSRF 토큰 발급과 갱신 절차를 따라야 한다.
- 운영 전체를 보려면 다른 ADR도 함께 봐야 한다.
- 인증 앱과 모든 복구 코드를 함께 잃으면 현재 자동 복구 수단이 없어 관리자 접근을 잃는다.

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

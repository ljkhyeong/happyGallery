# happyGallery API 계약

이 문서는 현재 구현 기준의 요청/응답 계약과 운영용 에러 코드를 정리한다.
비즈니스 규칙 원문은 `docs/PRD/0001_기준_스펙/spec.md`, 설계 배경은 `docs/ADR/`를 먼저 본다.

---

## 0. 문서 목적

- 클라이언트와 서버가 맞춰야 하는 HTTP 계약을 한 곳에 모은다.
- 기준 PRD에서 분리된 요청/응답 예시와 에러 포맷을 유지한다.
- 현재 운영 중인 v1 기준 API의 기본 계약을 문서화한다.
- 이 문서는 사람이 읽는 API 카탈로그와 정책의 기준이다.
- 상세 요청/응답 스니펫은 `./gradlew --no-daemon :adapter-in-web:restDocsTest`로 생성되는 Spring REST Docs 결과(`adapter-in-web/build/generated-snippets`)를 기준으로 검증한다.
- 기계 판독 계약은 Controller/웹 DTO에서 생성하는 `openapi3.json`이다. 이 파일과 `frontend/src/generated/api`는 직접 편집하지 않는다.
- 신규 또는 변경 API는 REST Docs 테스트와 이 문서를 갱신하고 `:adapter-in-web:openapi3`, `cd frontend && npm run api:generate`를 같은 변경에서 실행한다.
- 전체 `/api/v1/**` OpenAPI를 생성하고 React feature 계층에서 실제 호출하는 JSON·multipart API는 모두 생성 client를 사용한다. OAuth authorization URL로 브라우저가 직접 이동하거나 provider가 backend callback으로 돌아오는 흐름은 HTTP API wrapper가 아니므로 예외다.
- 생성 client 대상 Controller는 Java 메서드명과 독립된 고유 `operationId`를 명시하고, nullable 객체 참조는 OpenAPI 3.1의 `oneOf`로 표현한다.
- Java primitive 요청 필드는 런타임 기본값과 별개로 OpenAPI의 `required`를 명시한다. 다형 요청은 web DTO 경계에서 사용 지점의 `oneOf`와 공통 schema의 discriminator를 선언해 생성 TypeScript가 subtype과 필수값을 보존하되, 부모 `oneOf`와 자식 `allOf`가 순환하지 않게 한다.

---

## 1. 공통 계약

### 1.1 API 버전 정책

- 기본 전략: `URI Versioning`
  - 표준 경로: `/api/v1/**`
  - 예시: `/api/v1/bookings`, `/api/v1/admin/orders`
- 지원하는 API 경로는 `/api/v1/**`로 한정한다. 기존 무버전 경로는 더 이상 제공하지 않는다.
- 신규 기능 추가와 문서화도 `/api/v1/**`를 기준으로 한다.
- 브레이킹 변경은 `/api/v2/**`로 분리하고, `/api/v1/**`는 공지한 지원 종료 기간 이후 제거한다.

### 1.2 관리자 인증 정책

#### 운영 환경 인증

- 관리자 로그인 API를 통해 사용자명/비밀번호 기반으로 인증한다.
- MFA 비활성 계정은 비밀번호 확인 뒤 UUID 세션 토큰을 발급한다. 단, 운영 환경에서는 이 토큰에
  MFA 상태 조회·등록 시작·등록 확인 권한만 부여하며 등록을 마친 뒤 새로 로그인해야 일반 관리자 API를
  사용할 수 있다. MFA 활성 계정은 2단계 확인까지 끝난 뒤 일반 세션 토큰을 발급한다. 이후 요청에
  `Authorization: Bearer {token}` 헤더를 사용한다.
- 잘못된 비밀번호, 존재하지 않는 계정과 잘못된 MFA는 모두 같은 `401 INVALID_CREDENTIALS`로 응답해 계정 상태를 노출하지 않는다. 공격자가 사용자명만으로 운영자 계정을 잠그지 못하도록 계정 단위 하드 잠금은 두지 않고, IP 처리율 제한·MFA·감사 이력으로 로그인 남용을 통제한다.
- 세션 만료: 8시간
- 세션 저장소는 Redis 기반 `AdminSessionStore`를 사용한다. 여러 인스턴스가 떠 있어도 같은 세션을 본다.
- Redis에는 관리자 토큰 원문을 키로 쓰지 않고 토큰 HMAC을 사용하며, 세션 JSON도 AES-GCM 암호문으로 저장한다.
- 세션에는 발급 당시 `credentialVersion`을 저장한다. 비밀번호 또는 MFA 설정 변경으로 DB 버전이 증가하면 기존 버전의 모든 세션은 즉시 인증에 실패하고, Redis 세션 키는 커밋 후 일괄 삭제한다.
- 세션에는 마지막 인증 수단도 저장한다. 복구 코드로 로그인한 세션만 MFA 복구 초기화 권한을 가지며,
  인증 수단이 없던 기존 세션 payload는 비밀번호 전용 최소 권한으로 처리한다.
- 로그인과 MFA 결과는 사용자명이나 challenge 원문 대신 HMAC을 사용한 감사 이력으로 남기고 180일 뒤 삭제한다.

#### 인증 엔드포인트

```http
POST /api/v1/admin/auth/login
Content-Type: application/json

{ "username": "admin", "password": "..." }
```

```json
{
  "status": "AUTHENTICATED",
  "token": "uuid-session-token",
  "challengeToken": null
}
```

- 인증 완료: `200 OK`, `status=AUTHENTICATED`
- MFA 필요: `200 OK`, `status=MFA_REQUIRED`, `token=null`, 5분 유효한 `challengeToken`
- 두 응답 모두 `Cache-Control: no-store`
- 실패: `401 UNAUTHORIZED`
  - `{ "code": "INVALID_CREDENTIALS", "message": "관리자 인증 정보가 올바르지 않습니다." }`

```http
POST /api/v1/admin/auth/mfa/verify
Content-Type: application/json

{ "challengeToken": "...", "code": "123456" }
```

- `code`는 인증 앱의 6자리 TOTP 또는 `xxxx-xxxx-xxxx-xxxx` 형식의 미사용 복구 코드다.
- 성공: 로그인과 같은 `AUTHENTICATED` 응답과 `Cache-Control: no-store`
- challenge는 성공 시 한 번만 소비하며 생성 5분 뒤 만료된다.
- 등록 확인 또는 이전 challenge에서 이미 수락한 30초 TOTP 시간 구간은 새 challenge에서도 다시 사용할 수 없다.
- 실패: `401 INVALID_CREDENTIALS`

#### 관리자 MFA 관리

MFA 관리 API는 계정 ID가 있는 Bearer 관리자 세션에서만 호출할 수 있다. local API key는 `403 FORBIDDEN`이다.

```http
GET /api/v1/admin/auth/mfa
Authorization: Bearer {token}
```

```json
{
  "enabled": false,
  "enrollmentPending": false,
  "recoveryCodesRemaining": 0,
  "recoveryResetAvailable": false
}
```

- `recoveryResetAvailable`은 현재 Bearer 세션이 복구 코드로 로그인해 MFA 초기화를 호출할 수 있을 때만 `true`다.

```http
POST /api/v1/admin/auth/mfa/enrollment
Authorization: Bearer {token}
```

```json
{
  "secret": "BASE32-SECRET",
  "provisioningUri": "otpauth://totp/..."
}
```

- 등록 시작 응답은 `Cache-Control: no-store`이며, 비밀키는 인증 앱에 등록한 뒤 별도 보관하지 않는다.
- 서버는 비밀키를 AES-GCM 암호문으로 저장한다. 등록 시작만으로 MFA를 활성화하지 않는다.

```http
POST /api/v1/admin/auth/mfa/enrollment/confirm
Authorization: Bearer {token}
Content-Type: application/json

{ "code": "123456" }
```

```json
{
  "recoveryCodes": [
    "abcd-1234-efgh-5678"
  ]
}
```

- 유효한 TOTP를 확인하면 복구 코드 10개를 한 번만 응답하고 MFA를 활성화한다. 복구 코드는 오프라인에 보관하며 한 코드는 한 번만 사용할 수 있다.
- 성공 시 현재 세션을 포함한 기존 관리자 세션이 모두 무효화된다.

```http
DELETE /api/v1/admin/auth/mfa
Authorization: Bearer {token}
Content-Type: application/json

{ "currentPassword": "...", "code": "123456" }
```

- 현재 비밀번호와 유효한 TOTP 또는 미사용 복구 코드를 모두 확인한 뒤 `204 No Content`로 MFA를 해제하고 기존 세션을 모두 무효화한다.

```http
POST /api/v1/admin/auth/mfa/recovery
Authorization: Bearer {recovery-code-authenticated-token}
Content-Type: application/json

{ "currentPassword": "..." }
```

- 미사용 복구 코드로 MFA 로그인을 완료한 같은 Bearer 세션과 현재 비밀번호를 모두 확인한 뒤
  `204 No Content`로 MFA를 초기화한다. 성공하면 TOTP 비밀키와 모든 복구 코드를 삭제하고
  `credentialVersion`을 증가시켜 현재 세션을 포함한 기존 관리자 세션을 모두 무효화한다.
- TOTP·비밀번호 전용 Bearer 세션과 local API key는 `403 FORBIDDEN`, 현재 비밀번호가 틀리면
  `401 INVALID_CREDENTIALS`다. 관리 화면은 `recoveryResetAvailable=true`일 때만 복구 폼을 표시하고,
  성공 응답을 받으면 로컬 토큰과 관리자 캐시를 먼저 제거한 뒤 새 로그인·MFA 등록을 안내한다.
- 로그인·MFA 확인과 같은 fail-closed IP 처리율 제한을 공유하며 기본값은 분당 5회다. Redis 제한 상태를
  확인할 수 없으면 현재 비밀번호 BCrypt 검증과 관리자 행 잠금에 진입하지 않고 `503`으로 거절한다.
- 인증 앱과 모든 복구 코드를 함께 잃었을 때의 자동 복구는 지원하지 않는다. `ADMIN_SETUP_TOKEN`을 재사용하거나 DB에서 MFA를 직접 해제하지 않는다. 별도 검토된 오프라인 복구 기능을 배포하기 전까지 관리자 접근을 복구할 수 없으므로 복구 코드는 MFA 등록 직후 별도 장소에 보관한다.

```http
POST /api/v1/admin/auth/logout
Authorization: Bearer {token}
```

- 성공: `204 No Content`
- 관리자 클라이언트는 로그아웃 요청을 시작할 때 `sessionStorage`의 토큰과 관리자 캐시를 먼저 제거한다.
  요청 실패나 응답 유실 때도 로컬 로그인 상태를 복구하지 않으며, 서버 세션 폐기를 확인하지 못했다는
  경고를 표시한다. 별도 API 요청에서 `401 UNAUTHORIZED`를 받은 경우에도 이미 무효인 토큰이므로
  서버 로그아웃 호출 없이 로컬 토큰을 제거한다.

```http
PATCH /api/v1/admin/auth/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "admin123456",
  "newPassword": "new-admin-123456"
}
```

- 새 비밀번호는 10~72자이면서 UTF-8 72바이트 이하다. 현재 비밀번호를 포함한 모든 BCrypt 입력도 UTF-8 72바이트를 넘길 수 없다.
- 성공: `204 No Content`. 현재 세션을 포함해 해당 관리자에게 발급된 기존 세션을 모두 폐기하므로 새 비밀번호로 다시 로그인해야 한다.
- 실패:
  - `401 INVALID_CREDENTIALS` — 현재 비밀번호 불일치
  - `403 FORBIDDEN` — 계정 ID가 없는 local API key 인증으로 변경 시도
  - `422 PASSWORD_UNCHANGED` — 현재 비밀번호와 새 비밀번호가 같음
- 관리자 클라이언트는 `401 INVALID_CREDENTIALS`를 폼 입력 오류로 표시하고 현재 관리자 세션과 입력값을
  유지한다. 실제 세션 만료·폐기를 뜻하는 `401 UNAUTHORIZED`만 로컬 관리자 토큰을 제거한다.

#### 최초 관리자 계정 생성

- `admin_user`가 비어 있고 `app.admin.setup.token`(`ADMIN_SETUP_TOKEN`)이 설정된 동안에만 노출된다.
- 최초 관리자 계정 생성 경로는 관리자 Bearer 인증 없이 호출할 수 있지만, `RateLimitFilter`의 `admin-setup-per-minute` 별도 제한(기본 5/min)을 적용한다.
- 초기 설정 토큰이 비어 있거나 이미 관리자 계정이 생성된 뒤에는 엔드포인트를 `404 NOT_FOUND`로 숨긴다.

```http
GET /api/v1/admin/setup/status
```

```json
{ "required": true }
```

- 성공: `200 OK`

```http
POST /api/v1/admin/setup
Content-Type: application/json

{
  "token": "one-time-setup-token",
  "username": "admin",
  "password": "admin123456"
}
```

- 성공: `201 Created`
- 실패:
  - `401 UNAUTHORIZED` — 초기 설정 토큰 불일치
  - `404 NOT_FOUND` — 초기 관리자 계정 생성 비활성(토큰 없음 또는 이미 관리자 계정 존재)
  - `409 EMAIL_ALREADY_EXISTS` — 같은 username이 이미 존재
- 운영 규칙:
  - `username`은 개인 이메일·실명 대신 3~50자의 영문, 숫자, `.`, `_`, `-`로 구성한 운영 식별자를 사용한다.
- 계정 생성 직후 `status.required`는 `false`가 된다.
- 동시에 여러 설정 요청이 들어와도 DB의 단일 setup 잠금 행으로 직렬화하며 한 요청만 생성에 성공한다.
- 운영자는 최초 관리자 계정 생성이 끝나면 즉시 `ADMIN_SETUP_TOKEN`을 제거한다.

#### API Key 폴백

- **기본값은 `enable-api-key-auth=false`, `apiKey=""`** 이다. 프로덕션에서 설정이 빠져도 API Key 경로는 비활성 상태를 유지한다.
- `local` 프로필에서만 `enable-api-key-auth=true`와 `ADMIN_API_KEY`를 명시적으로 설정한다.
- 기본 관리자 계정은 Flyway migration에 포함하지 않고, `LocalAdminSeedService`(`@Profile("local")`)로 local 환경에서만 seed한다.
- 인증키 소스: 서버 설정 `app.admin.api-key`, 환경 변수 `ADMIN_API_KEY`
- API Key는 사람 관리자 계정 ID를 나타내지 않는다. nullable 감사 행위자를 허용한 운영 작업만 수행할 수 있고, 주문 클레임 처리·비밀번호·MFA처럼 관리자 ID가 필수인 작업은 `403 FORBIDDEN`이다.
- 주문 승인·거절·제작·배송·픽업과 예약 운영 이력의 adminId는 Bearer 세션이면 검증된 관리자 ID, 로컬 API key면 `null`이다.
  - 배치 자동 처리 이력도 `decided_by_admin_id = null`일 수 있다.

#### 적용 대상

- `/api/v1/admin/**` (로그인/로그아웃 제외)

#### 인증 실패 응답

- `401 UNAUTHORIZED`
- `{ "code": "UNAUTHORIZED", "message": "관리자 인증이 필요합니다." }`
- 관리자 인증은 됐지만 요청 권한이 없으면 `403 FORBIDDEN`과 기존 에러 JSON 형식을 반환한다.

### 1.3 회원 세션과 CSRF 정책

- 회원 세션은 `HG_SESSION` HttpOnly 쿠키로 유지하고, 로그인·회원가입·소셜 로그인 성공 시 세션 ID를 회전한다.
- 세션 principal 인덱스는 `userId:credentialVersion`으로 구분한다. 비밀번호 변경·재설정 커밋 뒤에는 변경 전 버전 인덱스만 삭제하므로, 동시에 새 버전으로 로그인한 세션은 유지된다.
- 회원 전용 API는 회원 인증 정보가 없으면 `401 UNAUTHORIZED`, 인증은 됐지만 권한이 없으면 `403 FORBIDDEN`을 기존 에러 JSON 형식으로 반환한다.
- 관리자 API는 Bearer/API key 헤더로 인증하므로 CSRF 토큰을 요구하지 않는다.
- `Bearer` 인증 scheme은 대소문자를 구분하지 않는다. Bearer 형식이 감지됐지만 토큰이 비었거나
  공백을 포함한 경우 유효한 API key가 함께 있어도 API key로 폴백하지 않고 인증 실패로 처리한다.
- 그 외 회원·공개 API의 `POST`, `PUT`, `PATCH`, `DELETE` 요청은 아래 SPA CSRF 절차를 따른다. 개별 API 예시에서 헤더를 생략해도 같은 규칙이 적용된다.

#### CSRF 토큰 발급

```http
GET /api/v1/auth/csrf
```

```json
{
  "cookieName": "XSRF-TOKEN",
  "headerName": "X-XSRF-TOKEN"
}
```

- 성공: `200 OK`
- 응답 캐시 정책: `Cache-Control: no-store`
- 응답은 JSON 본문과 함께 브라우저에서 읽을 수 있는 `XSRF-TOKEN` 쿠키를 설정한다. JSON 본문에는 토큰 값을 중복 노출하지 않는다.
- 클라이언트는 상태를 변경하는 요청에 `cookieName`이 가리키는 쿠키 값을 `headerName`이 가리키는 요청 헤더로 복사한다.

```http
X-XSRF-TOKEN: {XSRF-TOKEN 쿠키 값}
```

- 토큰이 없거나 일치하지 않으면 `403 FORBIDDEN`과 기존 에러 JSON 형식을 반환한다.
- 로그인과 로그아웃 성공 후에는 기존 CSRF 토큰이 폐기되므로 다음 상태 변경 요청 전에 `GET /api/v1/auth/csrf`를 다시 호출한다.
- 회원 클라이언트는 로그아웃 성공 응답 뒤에만 로컬 회원 상태를 제거한다. 실패나 응답 유실 때는 기존 상태를 유지하고 완료 여부를 확인하지 못했음을 표시한다.

#### 응답 캐시 정책

- 응답 생성에 부수효과가 없는 공개 조회 API만 `ETag`, `If-None-Match`, `304 Not Modified`를 지원한다.
- 상세 조회마다 조회수를 기록하는 `GET /api/v1/notices/{id}`는 ETag 대상에서 제외하고 `Cache-Control: no-store`를 반환한다.
- 관리자 전체, `/api/v1/auth/**`, `/api/v1/me`와 `/api/v1/me/**`, `/api/v1/payments/**`, `/api/v1/guest-records/**`, `X-Access-Token`을 제출한 응답은 중앙 보안 정책으로 `Cache-Control: no-store`를 적용한다. 그 밖의 API가 명시한 캐시 정책도 그대로 적용된다.

### 1.4 민감정보 형식과 오류 노출

- 회원가입 전화번호는 공백·하이픈을 제거한 숫자 형식으로 통일하며, 회원 응답의 `phone`도 같은 형식을 사용한다.
- 휴대폰 인증과 비회원 결제 payload의 표준 전화번호 형식은 `^01[0-9]{8,9}$`이다.
- 서버 로그에는 전화번호, 인증 코드, 결제 키, 관리자 세션 토큰과 외부 서비스 오류 원문을 남기지 않는다.
- JSON 요청 본문은 파싱 단계에서 문서 2MiB, 토큰 50,000개, 단일 문자열 1MiB를 상한으로 두며 초과하면 `400 INVALID_INPUT`으로 거절한다. 개별 DTO의 더 작은 문자열·목록 상한은 이 공통 파싱 한도보다 우선한다.
- 모든 `/api/v1/**` 요청은 IP 기준 기본 처리율 제한을 적용하고, 인증·결제·검증처럼 비용이 큰 경로는 더 엄격한 독립 버킷을 사용한다.
- 휴대폰·이메일 인증 코드 발송과 확인 시도, 고객 로그인, 결제 확정과 비회원 이력 인증은 검증 대상 전화번호·정규화 이메일·주문번호·회원 ID 기준 제한도 함께 적용한다.
- Redis 처리율 제한 버킷은 IP, 전화번호, 이메일, 주문번호 또는 회원 ID 원문 대신 HMAC 식별자를 사용한다.
- IP 식별자는 서버가 통제된 ingress 전달 헤더를 반영해 정규화한 `remoteAddr`만 사용한다.
  처리율 제한 코드가 `X-Forwarded-For`를 별도로 파싱하지 않는다.
- 제한 초과는 `429 TOO_MANY_REQUESTS`와 `Retry-After`, `X-RateLimit-Limit`, `X-RateLimit-Remaining` 헤더를 반환한다.
- Redis 장애 시 일반 API와 결제 확정은 제한만 건너뛰며, 인증·관리·결제 준비·비밀번호 확인과 비용이 큰 쓰기 API는 `503 SERVICE_UNAVAILABLE`, `Retry-After: 1`을 반환한다.
- 로그인·회원가입·관리자 로그인 클라이언트는 실패를 `boolean`으로 축약하지 않고 공통 `ErrorResponse`의 코드를 표시 규칙에 전달한다. 따라서 `401`, `409`, `429`, `503`을 자격 증명 오류 하나로 오인하지 않는다.
- 웹 클라이언트는 offset 없는 `DATETIME` 응답을 `Asia/Seoul` 현지시각으로 해석한다. `Z` 또는 명시적 offset이 있는 값은 해당 절대 시각을 보존하며, 브라우저의 시스템 timezone에 의존해 offset 없는 값을 해석하지 않는다.

---

## 2. API 카탈로그

### 2.1 Admin API — 클래스/슬롯 관리

#### 2.1.1 클래스 생성

```http
POST /api/v1/admin/classes
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "향수 원데이",
  "category": "PERFUME",
  "durationMin": 120,
  "price": 50000,
  "bufferMin": 30,
  "capacity": 6,
  "passEligible": false,
  "description": "나만의 향을 조합하는 원데이 클래스입니다.",
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
  "preparationInfo": "향에 민감하면 미리 알려주세요.",
  "targetAudience": "만 14세 이상"
}
```

```json
{
  "id": 1,
  "name": "향수 원데이",
  "category": "PERFUME",
  "durationMin": 120,
  "price": 50000,
  "bufferMin": 30,
  "capacity": 6,
  "passEligible": false,
  "description": "나만의 향을 조합하는 원데이 클래스입니다.",
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
  "preparationInfo": "향에 민감하면 미리 알려주세요.",
  "targetAudience": "만 14세 이상",
  "status": "ACTIVE"
}
```

- 성공: `201 Created`
- 에러:
  - `400 INVALID_INPUT` — 이름/카테고리 공란, durationMin/price/bufferMin/capacity 형식 오류, `passEligible` 누락 또는 콘텐츠 길이 초과
- 정책:
  - `category`는 앞뒤 공백을 제거하고 대문자 토큰으로 정규화해 저장·응답한다.
  - `price`는 10원 이상 `9,007,199,254,740,991원` 이하의 정수다. 10% 일반 예약금이 최소 1원이 되는 하한이다.
  - `capacity`는 1명 이상이며 자동 생성되는 모든 회차가 이 정원을 사용한다. 기존 클래스는 8명으로 이관한다.
  - `description`, `imageUrl`, `preparationInfo`, `targetAudience`는 선택값이다. `imageUrl`은 상품과 같은 공용 도메인 정책을 적용해 `/`로 시작하되 `//`가 아닌 서비스 경로 또는 호스트가 있는 `http(s)` URL만 허용한다.
  - 새 클래스는 `ACTIVE`로 생성된다. `passEligible`은 구매한 이용권 계획의 카테고리 정책과 함께 8회권 사용 가능 여부를 결정한다.

#### 2.1.2 기본 개방 예약 캘린더

관리자는 슬롯을 날짜별로 미리 만들지 않고 기본 운영시간을 설정한 뒤 운영하지 않는 날짜·시간만 닫는다.

```http
GET /api/v1/admin/slots/calendar?dateFrom=2026-08-01&dateTo=2026-08-31
Authorization: Bearer {token}
```

```json
{
  "settings": {
    "openTime": "10:00:00",
    "closeTime": "19:00:00",
    "slotIntervalMin": 30,
    "blockPublicHolidays": true,
    "version": 0
  },
  "days": [
    {
      "date": "2026-08-15",
      "publicHoliday": true,
      "effectiveAvailability": "CLOSED",
      "overrideMode": "DEFAULT",
      "reason": null,
      "timeBlocks": []
    }
  ]
}
```

- 조회 기간은 시작일·종료일을 포함해 최대 93일이다.
- `effectiveAvailability`는 `OPEN|CLOSED`, `overrideMode`는 `DEFAULT|OPEN|CLOSED`다.
- 법정·대체공휴일은 설정에 따라 기본 차단하며, 공휴일 날짜를 `OPEN`으로 지정하면 예약을 받는다. 정기 일요일은 공휴일 기본 차단 대상이 아니다.

```http
PATCH /api/v1/admin/slots/calendar/settings
Authorization: Bearer {token}
Content-Type: application/json

{
  "expectedVersion": 0,
  "openTime": "10:00",
  "closeTime": "19:00",
  "slotIntervalMin": 30,
  "blockPublicHolidays": true
}
```

- 시작 시각은 종료 시각보다 빨라야 하고 예약 시작 간격은 10~120분이다.
- 설정을 읽은 뒤 다른 관리자가 먼저 수정했으면 `409 CONFLICT`를 반환한다.

```http
PUT /api/v1/admin/slots/calendar/days/2026-08-15
Authorization: Bearer {token}
Content-Type: application/json

{ "mode": "OPEN", "reason": "광복절 특별 운영" }
```

- `DEFAULT`는 날짜 예외를 제거하고 기본 공휴일 설정을 다시 적용한다.
- `OPEN`은 공휴일을 포함해 해당 날짜를 열고, `CLOSED`는 종일 닫는다.
- 오늘 이후 날짜만 변경할 수 있고 사유는 선택값·최대 200자다.

```http
POST /api/v1/admin/slots/calendar/time-blocks
Authorization: Bearer {token}
Content-Type: application/json

{
  "date": "2026-08-18",
  "startTime": "12:00",
  "endTime": "13:00",
  "reason": "점심시간"
}
```

- 성공: `201 Created`; 응답은 `id`, `date`, `startTime`, `endTime`, `reason`을 반환한다.
- `DELETE /api/v1/admin/slots/calendar/time-blocks/{id}`는 등록한 차단을 해제하고 `204 No Content`를 반환한다.
- 시간 차단과 수업 시간이 한 번이라도 겹치는 자동 회차는 공개하지 않는다.
- 캘린더 변경은 클래스 행을 ID 순서로 먼저 잠근 뒤 기존 슬롯의 `calendarActive`를 갱신한다.

#### 2.1.3 슬롯 비활성화

```http
PATCH /api/v1/admin/slots/{id}/deactivate
Authorization: Bearer {token}
```

```json
{
  "id": 42,
  "classId": 1,
  "startAt": "2026-03-01T10:00:00",
  "endAt": "2026-03-01T12:00:00",
  "capacity": 8,
  "bookedCount": 0,
  "adminActive": false,
  "calendarActive": true,
  "bufferBlocked": false,
  "isActive": false
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — slotId에 해당하는 슬롯 없음
- 정책:
  - 관리자 비활성 상태는 예약 취소·변경으로 버퍼 차단이 자동 해제되어도 유지된다.

#### 2.1.4 슬롯 활성화

```http
PATCH /api/v1/admin/slots/{id}/activate
Authorization: Bearer {token}
```

- 성공: `200 OK` + 2.1.3과 같은 슬롯 응답
- 에러:
  - `404 NOT_FOUND` — slotId에 해당하는 슬롯 없음
- 정책:
  - `adminActive`만 `true`로 복구한다.
  - `bufferBlocked=true`이면 활성화 후에도 `isActive=false`다.

#### 2.1.5 클래스 전체 조회·수정·상태 변경

- `GET /api/v1/admin/classes` — `ACTIVE`, `INACTIVE` 클래스를 모두 반환한다.
- `PATCH /api/v1/admin/classes/{id}` — 이름·카테고리·가격·`passEligible`·설명·대표 이미지·준비물·대상 안내를 수정한다. 운영 시간·버퍼·회차 정원은 이미 생성된 회차와 예약에 영향을 주므로 이 API에서 바꾸지 않는다.
- `PATCH /api/v1/admin/classes/{id}/status` — `{ "status": "ACTIVE|INACTIVE" }`로 공개·예약 가능 상태를 변경한다.
- 성공: `200 OK`, 응답은 2.1.1의 클래스 응답과 같다.
- `INACTIVE` 클래스는 공개 목록, 자동 회차 조회와 결제 prepare 대상에서 제외한다. 기존 예약 이력은 유지한다.

### 2.2 공개 조회 API

#### 2.2.1 공개 상품 목록 조회

```http
GET /api/v1/products
```

```json
[
  {
    "id": 1,
    "name": "시그니처 캔들",
    "type": "READY_STOCK",
    "category": "CANDLE",
    "price": 39000,
    "description": "천연 소이 왁스로 만든 캔들입니다.",
    "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
    "specification": "소이 왁스 200g · 유리 용기",
    "careInstructions": "첫 사용은 표면 전체가 녹을 때까지 태워 주세요.",
    "productionLeadDays": null,
    "optionGroups": [],
    "variants": [],
    "stockQuantity": 3,
    "available": true
  }
]
```

- 성공: `200 OK`
- 정책:
  - `ACTIVE` 상태 상품만 노출한다.
  - 응답은 상품 상세 조회와 동일한 필드 구조를 사용한다.
  - `stockQuantity`는 현재 재고 수량을 반환한다. 기성품은 상품 재고, 주문제작은 활성 조합의 재고 합계이며, 특정 조합의 제한에는 `variants[].quantity`를 사용한다. 재고를 예약하는 값은 아니며 서버는 결제 시 다시 확인한다.
  - `200 OK` 응답에는 `ETag` 헤더를 포함한다.
  - `If-None-Match`가 현재 ETag와 같으면 `304 Not Modified`를 반환한다.

#### 2.2.2 공개 상품 상세 조회

```http
GET /api/v1/products/{id}
```

```json
{
  "id": 1,
  "name": "시그니처 캔들",
  "type": "READY_STOCK",
  "category": "CANDLE",
  "price": 39000,
  "description": "천연 소이 왁스로 만든 캔들입니다.",
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
  "specification": "소이 왁스 200g · 유리 용기",
  "careInstructions": "첫 사용은 표면 전체가 녹을 때까지 태워 주세요.",
  "productionLeadDays": null,
  "optionGroups": [],
  "variants": [],
  "stockQuantity": 3,
  "available": true
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — productId 미존재 또는 판매 중지 상품
- 정책:
  - `ACTIVE` 상품만 반환하며, 판매 중지 상품은 존재 여부를 구분하지 않고 `404 NOT_FOUND`로 응답한다.
  - 주문제작 상품은 `optionGroups`에 `SELECT|TEXT` 그룹과 선택값·필수 여부·직접입력 제한을, `variants`에 선택 조합별 `id`, 추가 금액, 재고와 판매 여부를 반환한다. 기성품은 두 배열이 비어 있다.
  - `200 OK` 응답에는 `ETag` 헤더를 포함한다.
  - `If-None-Match`가 현재 ETag와 같으면 `304 Not Modified`를 반환한다.

#### 2.2.3 공개 클래스 목록 조회

```http
GET /api/v1/classes
```

```json
[
  {
    "id": 1,
    "name": "향수 클래스",
    "category": "PERFUME",
    "durationMin": 120,
    "price": 50000,
    "bufferMin": 30,
    "capacity": 6,
    "passEligible": false,
    "description": "나만의 향을 만드는 원데이 클래스입니다.",
    "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
    "preparationInfo": null,
    "targetAudience": "만 14세 이상",
    "status": "ACTIVE"
  }
]
```

- 성공: `200 OK`
- 정책:
  - `ACTIVE` 클래스만 반환한다. 관리자는 별도 전체 조회 API를 사용한다.
  - 프론트 예약 생성 화면은 이 응답을 기준으로 클래스 선택지를 구성한다.
  - `200 OK` 응답에는 `ETag` 헤더를 포함한다.
  - `If-None-Match`가 현재 ETag와 같으면 `304 Not Modified`를 반환한다.

공개 클래스 상세는 `GET /api/v1/classes/{id}`로 `ACTIVE` 클래스의 같은 `ClassResponse` 한 건을 반환한다.
없거나 비활성인 클래스는 존재 여부를 구분하지 않고 `404 NOT_FOUND`로 응답한다. 예약·슬롯·결제 내부 흐름에서는 기존처럼 비활성 클래스를 `422 CLASS_INACTIVE`로 거절한다.

#### 2.2.4 공개 예약 가능 슬롯 조회

```http
GET /api/v1/slots?classId=1&date=2026-03-01
```

```json
[
  {
    "id": 42,
    "classId": 1,
    "startAt": "2026-03-01T10:00:00",
    "endAt": "2026-03-01T12:00:00",
    "capacity": 8,
    "bookedCount": 3,
    "remainingCapacity": 5
  }
]
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — `classId`, `date` 파라미터 누락 또는 형식 오류
- 정책:
  - `classId` + `date` 기준으로 당일 슬롯만 조회한다.
  - 조회 트랜잭션이 클래스 행을 잠근 뒤 기본 운영시간·날짜 예외·공휴일·시간 차단에 맞는 슬롯 행을 자동 생성하거나 기존 `calendar_active`를 갱신한다.
  - `admin_active = true`, `calendar_active = true`, `buffer_block_count = 0`이고 `booked_count < capacity`인 슬롯만 노출한다.
  - 정렬은 `startAt` 오름차순이다.

#### 2.2.4.1 향후 예약 가능 슬롯 조회

```http
GET /api/v1/slots/upcoming?classId=1&days=14&includeFull=true
```

응답 항목은 2.2.4의 공개 슬롯 응답과 같다.

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — `classId` 누락 또는 `days`가 1~30 범위를 벗어남
- 정책:
  - `days`는 선택값이며 기본 14일, 최대 30일이다.
  - `includeFull`은 선택값이며 기본 `false`다. `true`이면 다른 활성 조건은 충족하지만 `bookedCount=capacity`인 만석 회차도 `remainingCapacity=0`으로 포함한다.
  - 현재 시각 이후부터 KST 기준 오늘을 포함한 조회 마지막 날의 다음 날 00:00 전까지 예약 가능한 슬롯을 `startAt` 오름차순으로 반환한다.
  - 예약 화면은 `includeFull=true` 결과를 날짜별로 묶고, 남은 자리가 있는 회차는 예약 선택, 만석 회차는 빈자리 알림 신청으로 연결한다.

#### 2.2.4.2 만석 회차 빈자리 알림

비회원 신청:

```http
POST /api/v1/slots/{slotId}/vacancy-alerts

{
  "name": "홍길동",
  "phone": "01012345678",
  "verificationCode": "123456"
}
```

회원 신청:

```http
POST /api/v1/me/slots/{slotId}/vacancy-alerts
Cookie: HG_SESSION=...
```

회원 신청 목록 조회:

```http
GET /api/v1/me/vacancy-alerts
Cookie: HG_SESSION=...
```

```json
{
  "alertId": 700,
  "slotId": 42,
  "className": "가죽 카드지갑 원데이",
  "startAt": "2026-09-05T14:00:00",
  "endAt": "2026-09-05T16:00:00",
  "status": "WAITING",
  "accessToken": "비회원-취소용-토큰"
}
```

- 비회원 취소: `DELETE /api/v1/slots/{slotId}/vacancy-alerts`, 발급받은 `X-Access-Token` 필수
- 회원 취소: `DELETE /api/v1/me/slots/{slotId}/vacancy-alerts`, 회원 세션 필수
- 성공: 신청·목록 조회·취소 `200 OK`; 목록은 현재 회원의 `WAITING` 신청만 신청 순서로 반환하고 `accessToken`은 `null`이다.
- 에러:
  - `400 INVALID_INPUT` — 활성·미래 만석 회차가 아니거나 비회원 입력 형식 오류
  - `404 NOT_FOUND` — 회차, 회원 또는 비회원 알림 토큰 불일치
  - `422 PHONE_VERIFICATION_REQUIRED` — 휴대폰 인증을 완료하지 않은 회원
- 정책:
  - 빈자리 알림은 좌석을 예약하거나 결제하지 않는다. 알림을 받은 고객이 예약 화면에서 선착순으로 직접 예약한다.
  - 회원은 등록된 인증 휴대폰, 비회원은 `GUEST_BOOKING` 목적의 6자리 SMS 인증으로 수신 번호 소유권을 확인한다.
  - 같은 회차·수신자에는 `WAITING` 알림 한 건만 유지한다. 비회원이 다시 인증해 신청하면 취소용 접근 토큰만 새로 발급한다.
  - 회원 화면은 `GET /api/v1/me/vacancy-alerts`를 서버 원본으로 사용해 새로고침 뒤 신청 상태를 복원한다. 응답의 클래스명·시작·종료 시각으로 마이페이지에 현재 대기 목록을 표시하며, 운영·캘린더·버퍼 사유로 회차가 예약 화면에서 사라져도 회원은 마이페이지에서 신청을 취소할 수 있다. 비회원 화면은 취소 토큰을 현재 고객 세션 소유권과 함께 `sessionStorage`에 저장하며, 같은 탭의 새로고침까지만 복원하고 로그인·로그아웃·계정 전환 뒤에는 이전 상태를 적용하지 않는다.
  - 만석이었던 활성 회차가 전체취소·부분취소·예약 변경으로 1석 이상 열리는 순간 모든 `WAITING` 신청을 `NOTIFIED`로 전환하고 알림 outbox를 같은 트랜잭션에 한 번씩 저장한다.
  - 회차가 운영·캘린더·버퍼 사유로 닫혀 있으면 자리가 반환돼도 알리지 않는다. 관리자가 다시 열어 실제 예약 가능해진 시점에 대기 알림을 발송한다.
  - 알림 발송은 한 번으로 끝나며 자동 재신청하지 않는다.
  - 시작 시각이 지난 미발송 신청은 개인정보 보존 배치에서 삭제하고, 발송·취소된 신청은 30일 뒤 삭제한다. 실제 발송 감사 이력은 알림 로그·outbox 보존 정책을 따른다.

### 2.3 관리자 상품 API

#### 2.3.1 상품 등록

```http
POST /api/v1/admin/products
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "시그니처 캔들",
  "type": "READY_STOCK",
  "category": "CANDLE",
  "price": 39000,
  "quantity": 5,
  "description": "천연 소이 왁스로 만든 캔들입니다.",
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
  "specification": "소이 왁스 200g · 유리 용기",
  "careInstructions": "첫 사용은 표면 전체가 녹을 때까지 태워 주세요.",
  "productionLeadDays": null,
  "optionGroups": [],
  "variants": []
}
```

```json
{
  "id": 1,
  "name": "시그니처 캔들",
  "type": "READY_STOCK",
  "category": "CANDLE",
  "price": 39000,
  "description": "천연 소이 왁스로 만든 캔들입니다.",
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
  "specification": "소이 왁스 200g · 유리 용기",
  "careInstructions": "첫 사용은 표면 전체가 녹을 때까지 태워 주세요.",
  "productionLeadDays": null,
  "status": "ACTIVE",
  "available": true,
  "quantity": 5,
  "optionGroups": [],
  "variants": []
}
```

- 성공: `201 Created`
- 에러:
  - `400 INVALID_INPUT` — 이름/유형/가격/수량 검증 실패
  - `401 UNAUTHORIZED` — 관리자 인증 실패
- 정책:
  - `category`는 선택값이며, 입력하면 앞뒤 공백을 제거하고 대문자 토큰으로 정규화해 저장·응답한다.
  - 공백 카테고리는 미입력과 동일하게 처리한다.
  - `price`는 1원 이상 `9,007,199,254,740,991원` 이하의 정수다. 상한은 웹 클라이언트가 원 단위 금액을 정밀도 손실 없이 전달할 수 있는 기술 경계다.
  - `description`, `imageUrl`, `specification`, `careInstructions`는 선택값이며 `imageUrl`은 `/`로 시작하는 서비스 경로 또는 `http(s)` URL이어야 한다.
  - `MADE_TO_ORDER`는 `specification`과 1~180일 `productionLeadDays`가 필수다. `READY_STOCK`은 `productionLeadDays=null`이어야 한다.
  - 주문제작은 선택형 옵션 그룹 최대 3개, 직접입력형 최대 5개, 전체 조합 최대 500개다. 선택 그룹은 미선택 조합을 포함하며 각 variant에 선택값, 가격 추가금, 재고, 판매 여부를 보낸다.
  - 선택형 옵션이 없는 주문제작 상품은 `optionGroups`, `variants`를 생략하고 `quantity`를 보내면 기본 variant 한 개를 만든다. 기성품의 옵션 배열은 생략하거나 빈 배열로 보낸다.

#### 2.3.2 전체 상품 목록 조회

```http
GET /api/v1/admin/products
Authorization: Bearer {token}
```

```json
[
  {
    "id": 1,
    "name": "시그니처 캔들",
    "type": "READY_STOCK",
    "price": 39000,
    "status": "ACTIVE",
    "available": true,
    "quantity": 5
  }
]
```

- 성공: `200 OK`
- 정책:
  - `ACTIVE`, `INACTIVE` 상품을 모두 최신 등록순으로 반환한다.
  - 주문제작 `variants`는 현재 선택형 그룹·값·필수 여부로 만들 수 있는 조합만 반환한다. 현재 조합의 판매 중지 행은 포함하지만 구조 변경으로 사라진 조합과 V164 과거 보존 행은 제외한다. 상품 수정 응답도 같은 기준을 사용한다.
  - `available`은 `status=ACTIVE`이면서 재고가 1개 이상일 때만 `true`다.

#### 2.3.3 상품 판매 상태 변경

```http
PATCH /api/v1/admin/products/{id}/status
Authorization: Bearer {token}
Content-Type: application/json

{ "status": "INACTIVE" }
```

- 성공: `200 OK` — 변경된 상품과 현재 재고를 `ProductResponse`로 반환
- 에러:
  - `400 INVALID_INPUT` — `status` 누락 또는 지원하지 않는 상태
  - `404 NOT_FOUND` — 상품 미존재
- 정책:
  - `ACTIVE`, `INACTIVE`를 지원하며 같은 상태로의 요청은 성공한 것으로 처리한다.
  - `INACTIVE` 상품은 재고가 남아 있어도 공개 목록에 노출하지 않고 결제 대상으로 확정하지 않는다.

#### 2.3.4 재고 수동 조정

```http
POST /api/v1/admin/products/{id}/inventory-adjustments
Authorization: Bearer {token}
Content-Type: application/json

{
  "productVariantId": null,
  "type": "DECREASE",
  "quantity": 2,
  "reason": "오프라인 매장 판매"
}
```

```json
{
  "id": 10,
  "productId": 1,
  "productVariantId": null,
  "type": "DECREASE",
  "quantity": 2,
  "quantityBefore": 12,
  "quantityAfter": 10,
  "reason": "오프라인 매장 판매",
  "adjustedByAdminId": 99,
  "adjustedBy": "admin",
  "adjustedAt": "2026-05-01T21:05:00"
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 유형 누락, 1 미만 수량, 빈 사유 또는 500자 초과 사유
  - `404 NOT_FOUND` — 상품 재고 미존재
  - `409 INVENTORY_NOT_ENOUGH` — 감소 수량이 현재 재고보다 큼
- 정책:
  - `type`은 `INCREASE`, `DECREASE`를 지원한다.
  - 기성품은 `productVariantId=null`, 주문제작 상품은 조정할 variant ID를 필수로 보낸다.
  - 재고 행을 비관적 쓰기 잠금으로 조회한 뒤 수량 변경과 조정 이력을 같은 트랜잭션에 저장한다.
  - `adjustedByAdminId`는 관리자 Bearer 세션이면 관리자 ID, 로컬 API key 인증이면 `null`이다. `adjustedBy`에는 관리자명 또는 `local-api-key`를 남긴다.

#### 2.3.5 최근 재고 조정 이력 조회

```http
GET /api/v1/admin/products/{id}/inventory-adjustments
Authorization: Bearer {token}
```

- 성공: `200 OK` — 최신순 최대 50건, 각 항목은 재고 수동 조정 응답과 동일
- 에러:
  - `404 NOT_FOUND` — 상품 미존재

#### 2.3.6 스마트스토어 재고 연동 설정

연동할 상품과 옵션은 네이버 상품 목록과 원상품 상세에서 선택한다.

```http
GET /api/v1/admin/products/smartstore-catalog?page=1&size=100
GET /api/v1/admin/products/smartstore-catalog/{originProductNo}
Authorization: Bearer {token}
```

- 목록은 `products`, 1부터 시작하는 `page`, `size`, `totalElements`, `totalPages`를 반환한다. 상품 항목은 `originProductNo`, `channelProductNo`, 상품명, 판매 상태·판매가·재고와 nullable 대표 이미지 주소를 포함한다.
- 원상품 상세는 판매가·판매 상태와 옵션별 `optionId`, 조합명, 재고, 옵션가, 사용 여부를 반환한다. 관리자는 이 정보를 보고 내부 주문제작 옵션 조합마다 네이버 옵션을 하나씩 선택한다.
- `page`는 1 이상, `size`는 1~100이다. 스마트스토어 연동이 비활성화됐으면 `409 CONFLICT`를 반환한다.

```http
PUT /api/v1/admin/products/{id}/smartstore-inventory
Authorization: Bearer {token}
Content-Type: application/json

{
  "originProductNo": 123456789,
  "enabled": true,
  "expectedMappingVersion": 17,
  "previousOriginConfirmed": false,
  "variants": [
    { "productVariantId": 31, "optionId": 90001 },
    { "productVariantId": 32, "optionId": 90002 }
  ]
}
```

- 성공: `200 OK` — 저장된 매핑의 불투명 `mappingVersion`과 `PENDING|PROCESSING|SYNCED|FAILED` 동기화 상태, 시도 횟수, 마지막 오류와 완료 시각 반환
- 최초 등록은 `expectedMappingVersion=null`, 수정은 직전 조회·저장 응답의 `mappingVersion`을 보낸다. 현재 값과 다르면 다른 화면에서 설정이 변경·삭제·재등록된 것이므로 `409 CONFLICT`를 반환하며 자동 병합하지 않는다.
- 기성품은 `variants=[]`로 보내고 스마트스토어 원상품 재고를 갱신한다.
- 주문제작 상품은 관리자 상품 응답에 표시되는 현재 조합을 판매 중지 여부와 관계없이 정확히 한 번씩 보내야 한다. 과거 주문 보존용 조합은 입력하지 않는다. 각 `optionId`는 같은 원상품 안에서 중복할 수 없다.
- 매핑 응답의 `variants`에는 현재 연결만 반환한다. 같은 원상품에서 옵션 구조 변경 또는 같은 조합의 원격 옵션 번호 변경으로 해제된 연결은 내부에 보존하며 자동 재고 전송에서는 0개를 보낸다. 실패하면 기존 재시도 경로에서 현재·과거 옵션을 함께 다시 전송한다. 현재 조합이 그 원격 옵션 번호를 재사용하면 과거 연결은 제거해 중복 전송하지 않는다. 전체 연동 해제는 보존된 매핑도 제거하며, 원상품 번호를 바꾸는 경우 이전 원상품 판매 상태는 스마트스토어에서 별도로 관리한다.
- 원상품 번호가 바뀌면 `previousOriginConfirmed=true`가 필수다. 서버는 기존 매핑이 남아 있는 동안 변경 주문을 현재 시점까지 수집하고, 수집 실패·진행 중·남은 페이지가 있으면 `409 CONFLICT`로 저장을 보류한다. 외부 수집 트랜잭션이 끝난 뒤 상품 행을 잠그고 `mappingVersion`을 다시 비교한 경우에만 새 원상품을 저장한다.
- 원상품 변경 전 수집한 기존 주문에 `MAPPING_REQUIRED|STOCK_SHORTAGE|STATUS_REVIEW`가 남아 있으면 기존 매핑을 유지하고 `409 CONFLICT`를 반환한다. 아직 내부 상품 번호가 없는 주문도 기존 원상품 번호로 확인한 뒤에만 새 원상품으로 전환한다.
- 저장·해제 전 연결은 주문 식별 전용 이력에 종료 시각과 함께 보존한다. 이후 늦게 수집된 주문은 결제 시각에 해당하는 과거 원상품·옵션 연결을 먼저 사용해 기존 내부 상품·SKU 재고에 반영한다. 과거 옵션을 확정하지 못한 `MAPPING_REQUIRED|STATUS_REVIEW` 주문도 과거 원상품 이력으로 내부 상품을 찾아 현재 원상품 재고 전송을 보류한다. 이 이력은 현재 매핑 응답, 상품 미리보기와 외부 재고 전송에는 포함하지 않는다.
- `enabled=true`로 저장하거나 재시도하면 최신 로컬 재고 반영 요청을 같은 트랜잭션에서 생성한다. `enabled=false`는 매핑을 보존하되 대기 중 동기화를 제거한다.
- 비활성화·해제 후 다시 등록한 동기화는 이전 전송과 구분한다. 이전 전송의 성공·실패 응답은 새 요청의 완료 상태, 시도 횟수, 오류와 재시도 시각을 바꾸지 않는다.
- 조회: `GET /api/v1/admin/products/{id}/smartstore-inventory`, 미설정이면 `404 NOT_FOUND`
- 재시도: `POST /api/v1/admin/products/{id}/smartstore-inventory/retry`
- 해제: `DELETE /api/v1/admin/products/{id}/smartstore-inventory?expectedMappingVersion=17&previousOriginConfirmed=true`, 성공 `204 No Content`
- 해제는 직전 응답의 `mappingVersion`과 기존 원상품의 판매 중지·재고 확인 완료를 필수로 받는다. 서버는 원상품 변경과 같은 주문 선수집·미반영 주문 확인·상품 잠금·개정 재검사를 거치며, 조건이 달라졌으면 `409 CONFLICT`로 현재 매핑을 유지한다.
- 변경 이력: `GET /api/v1/admin/products/{id}/smartstore-inventory/history`
- 변경 이력은 최근 20건을 최신순으로 반환한다. 각 항목은 `id`, `action(CREATED|UPDATED|ORIGIN_CHANGED|ENABLED|DISABLED|DELETED)`, nullable 변경 전후 원상품 번호·사용 여부·옵션 연결 요약·매핑 개정, 기존 원상품 확인 여부, nullable 관리자 ID, 관리자 이름과 처리 시각을 포함한다. 저장·해제 성공과 같은 트랜잭션에서 기록하며 Bearer 세션은 관리자 ID·이름, 로컬 API key는 nullable ID와 API key 주체 이름을 남긴다.

가격·상태·옵션가를 반영하기 전에는 다음 미리보기를 조회한다.

```http
GET /api/v1/admin/products/{id}/smartstore-product-preview
POST /api/v1/admin/products/{id}/smartstore-product-sync
Authorization: Bearer {token}
```

- 미리보기는 불투명 문자열 `previewVersion`, 양쪽 판매가·판매 상태와 옵션별 양쪽 옵션가·사용 여부·차이 여부를 반환한다.
- 미리보기에는 과거 연결도 포함하며 추가금 0원·사용 불가로 반환한다. 같은 `productVariantId`가 여러 원격 옵션에 남을 수 있으므로 옵션 행은 `optionId`로 구분한다. 관리자 화면은 원격 옵션 번호와 이전 연결 여부를 표시하고 매핑 저장 후 미리보기를 다시 조회한다.
- 반영 요청은 `{ "previewVersion": "미리보기에서 받은 값" }`을 받는다. 상품 버전·가격·판매 상태·원상품 번호·현재 및 과거 옵션 연결·옵션가·사용 여부와 연결 행 식별자를 비교해 변경됐으면 `409 CONFLICT`로 거절한다. 재고 수량만 바뀌고 판매 상태가 같으면 최신 수량을 사용한다. 연결을 해제·재등록한 경우에는 같은 번호여도 다시 비교해야 한다.
- 기존 숫자 `productVersion` 요청은 허용하지 않으며 서버와 화면을 함께 배포한다. 관리 화면은 반영 대상 원상품 번호를 표시하고, 충돌 시 미리보기를 다시 조회하되 반영은 자동 재시도하지 않는다.
- 주문제작 상품은 판매가와 모든 옵션의 옵션가·사용 여부·현재 재고를 한 요청으로 보내고, 기성품은 판매가와 현재 재고를 함께 반영한다. 그 뒤 현재 재고를 고려한 `SALE|OUTOFSTOCK|SUSPENSION` 상태를 적용한다.
- 외부 반영을 호출한 뒤에는 성공·부분 실패와 관계없이 현재 활성 연결의 재고 동기화를 다시 요청한다. 먼저 완료된 자동 전송을 늦은 수동 요청이 덮어써도 다음 배치가 최신 수량으로 보정한다. 가격·판매 상태는 자동 재시도하지 않는다.
- 수동 반영 직전에 주문 변경 피드를 한 페이지 수집한다. 수집 비활성·실패·다른 실행의 처리 중 상태, 남은 페이지나 이전 날짜 구간이 있으면 `409 CONFLICT`로 보류한다. 연결된 원상품에 매핑 누락·재고 부족·알 수 없는 주문 상태로 미반영된 주문이 있어도 같은 응답으로 거절한다. 수집 완료 뒤 최신 재고로 `previewVersion`을 확인하므로 새 주문으로 품절 여부가 바뀌면 미리보기를 다시 확인해야 한다. 미리보기 조회 자체는 보류하지 않는다.

네이버 검수 반려 상품과 상품 공지사항은 로컬에 복제하지 않고 실시간 관리한다.

```http
GET /api/v1/admin/products/smartstore-inspections?page=1&size=100
PUT /api/v1/admin/products/smartstore-inspections/{channelProductNo}/restore
GET /api/v1/admin/smartstore-notices?page=1&size=100
GET /api/v1/admin/smartstore-notices/{sellerNoticeId}
POST /api/v1/admin/smartstore-notices
PUT /api/v1/admin/smartstore-notices/{sellerNoticeId}
DELETE /api/v1/admin/smartstore-notices/{sellerNoticeId}
PUT /api/v1/admin/smartstore-notices/{sellerNoticeId}/products
Authorization: Bearer {token}
```

- 검수 목록은 `channelProductNo`, 반려 사유, 필요한 조치, 복원 요청 가능 여부를 반환한다. 복원 요청은 상품 수정 뒤 네이버에 전달하며 성공 시 `204`다.
- 공지 목록은 유형·제목·중요/전체 공지 여부와 전시 기간을, 상세는 팝업 기간과 본문을 추가로 반환한다. 유형은 `ORDINARY|EVENT|DELIVERY|PRODUCT`다.
- 등록·수정은 제목·본문과 중요/전체/팝업 여부 및 각 기간을 받는다. 저장 성공은 `sellerNoticeId`를 반환하고 삭제 성공은 `204`다. 상품에 적용된 공지의 삭제 가능 여부는 네이버가 최종 판정한다.
- 상품 적용 요청은 비어 있지 않은 `channelProductNos`를 받고 성공 시 `204`다.

#### 2.3.7 스마트스토어 채널 주문 관리

```http
GET /api/v1/admin/smartstore-orders/return-delivery-companies
Authorization: Bearer {token}
```

- 성공: `200 OK` — 네이버에 등록된 반품·교환 택배사 계약을 `[{id, name, priorityType}]`로 반환한다. 계약이 없으면 빈 배열이며 우선순위가 없으면 `priorityType`은 `null`이다. 우선순위는 `PRIMARY`, `SECONDARY_1`~`SECONDARY_9` 등 네이버 문자열을 그대로 전달한다.
- `id`는 반품 택배사 계약번호이며 주문 발송·수거용 택배사 코드가 아니다. 연동이 비활성화되어 있으면 `409 CONFLICT`를 반환한다.

```http
GET /api/v1/admin/smartstore-orders?attentionOnly=true&attentionReason=STOCK_SHORTAGE&cursor={cursor}&size=50
Authorization: Bearer {token}
```

- 성공: `200 OK` — 최신 변경순 채널 상품 주문을 `{content, nextCursor, hasMore}`로 반환한다. `size`는 1~100이고 기본값은 50이다. 다음 페이지는 응답의 불투명 `nextCursor`를 그대로 보내며 필터를 바꾸면 커서를 비운다.
- `attentionOnly=true`이면 관리자 확인이 필요한 주문만 반환하고, `attentionReason`을 함께 보내면 해당 사유만 조회한다.
- 응답의 `attentionReason`:
  - `MAPPING_REQUIRED`: 네이버 원상품·옵션 ID에 연결된 내부 상품·옵션 조합 없음
  - `STOCK_SHORTAGE`: 내부 재고가 네이버 주문의 추가 차감 필요 수량보다 부족함
  - `RETURN_REVIEW`: 반품 완료품의 판매 가능 여부와 재고 복원 여부 확인 필요
  - `STATUS_REVIEW`: 서버가 아직 재고 정책을 정하지 않은 네이버 주문 상태
- `inventoryAppliedQuantity`는 해당 상품 주문 때문에 현재 내부 공유 재고에서 차감된 수량이다. 다음 동기화는 잔여 주문 수량과 아직 복원하지 않은 완료 반품 수량을 합한 목표 수량과의 차이만 변경한다. 검수 대기 또는 판매 불가로 종료한 반품은 계속 차감된 수량에 포함된다.
- 주문 응답의 `pendingReturnQuantity`는 현재 미검수 반품 수량이며 `returnReviewVersion`은 그 검수 대상의 확인값이다. `inventoryResolutionVersion`은 수동 재고 결정 대상의 확인값이다. 세 값은 항상 반환하며 화면에서 만들거나 해석하지 않고 확인창을 열 때 받은 값을 요청에 그대로 보낸다.

```http
POST /api/v1/admin/smartstore-orders/{productOrderId}/inventory-resolution
Authorization: Bearer {token}
Content-Type: application/json

{
  "productId": 1,
  "productVariantId": 31,
  "action": "APPLY_REMAINING",
  "reason": "스마트스토어 옵션과 내부 옵션 조합을 확인",
  "resolutionVersion": "..."
}
```

- `MAPPING_REQUIRED|STATUS_REVIEW` 주문만 처리한다. `productId`는 필수이며 기성품의 `productVariantId`는 명시적 `null`, 주문제작 상품은 해당 상품의 옵션 조합 ID가 필수다.
- `action`은 남은 주문 수량까지 차감하는 `APPLY_REMAINING`, 현재 차감 수량을 모두 복원하는 `RESTORE_ALL`, 현재 차감을 유지하고 확인만 끝내는 `KEEP_CURRENT`다. 처리 사유는 500자 이하로 필수다.
- 주문 잠금 안에서 `resolutionVersion`을 다시 비교하며 대상이 바뀌었으면 `409 CONFLICT`를 반환한다. 이미 재고가 반영된 주문은 다른 상품·옵션 조합으로 바꿀 수 없다.
- 성공하면 상품 연결·재고 변경·`INVENTORY_RESOLVED` 처리 이력을 같은 트랜잭션으로 저장한다. 재고가 부족하면 연결은 유지하고 `STOCK_SHORTAGE`와 거절 이력을 반환 주문에 남긴다.

```http
POST /api/v1/admin/smartstore-orders/{productOrderId}/inventory/retry
Authorization: Bearer {token}
```

- 매핑 누락 또는 재고 부족 주문의 현재 상태를 기준으로 내부 재고 반영을 다시 시도하고 `200 OK`로 갱신된 주문을 반환한다.
- 재시도 뒤에도 반영할 수 없으면 오류 응답으로 버리지 않고 현재 `attentionReason`을 유지한다.
- 상품 주문이 없으면 `404 NOT_FOUND`를 반환한다.

```http
POST /api/v1/admin/smartstore-orders/{productOrderId}/return-resolution
Authorization: Bearer {token}
Content-Type: application/json

{ "restoreStock": true, "reviewVersion": "R2:0" }
```

- `RETURN_REVIEW` 주문만 처리한다. 배송 중 등 일반 주문 상태를 유지하는 부분반품도 포함하며 `RETURNED` 상태로 제한하지 않는다. `restoreStock=true`이면 미검수 반품 수량을 기존 내부 상품·옵션 재고에 복원하고, `false`이면 판매 불가 반품으로 재고를 복원하지 않는다.
- `reviewVersion`은 필수다. 주문을 잠근 뒤 현재 검수 대상과 일치하는지 확인하며, 새 반품이 수집되거나 다른 관리자가 검수를 끝냈으면 재고·검수 기록을 바꾸지 않고 `409 CONFLICT`로 거절한다. 이전 검수를 끝낸 뒤 같은 수량의 새 반품이 생겨도 기존 확인값은 재사용할 수 없다.
- 두 선택 모두 누적 검수 완료 수량을 저장한다. 재시도·재수집은 같은 수량을 다시 검수 대상으로 만들지 않으며, 추가 반품이 발생하면 새 반품 수량만 처리한다. 반품 뒤 다른 수량이 취소되어도 이전에 복원 없이 종료한 반품을 재고로 돌리지 않는다.
- 성공: `200 OK` — 확인 사유를 해제한 주문 반환
- 에러: 상품 주문 미존재 `404 NOT_FOUND`, 확인값 누락·빈 값 또는 현재 반품 확인 대상이 아닌 주문 `400 INVALID_INPUT`, 검수 대상 변경 `409 CONFLICT`
- 확인값을 보내지 않는 구 관리자 화면은 검수 요청을 처리할 수 없다. 잘못된 재고 복원을 막기 위한 필수값 추가이므로 서버·관리자 화면·생성 클라이언트를 함께 배포한다. 충돌 시 목록만 새로 조회하며 검수 요청을 자동 재전송하지 않는다.

```http
GET /api/v1/admin/smartstore-orders/{productOrderId}
Authorization: Bearer {token}
```

- 성공: `200 OK` — 채널 주문 기본 정보와 `deliveryInfo`, `placeOrderStatus`, `shippingDueDate`, 배송수단·택배사·운송장, 단가·결제액·수수료·정산 예정액, 네이버에서 단건 조회한 현재 `claimDetail`을 반환한다. 클레임 상세에는 사유·상세 사유·요청 수량·요청일·수거 상태와 운송장·배송비·보류 상태·첨부 이미지 주소를 포함한다.
- `deliveryInfo`는 암호문을 관리자 단건 조회에서만 복호화한 결과이며 목록 응답에는 포함하지 않는다.

```http
GET /api/v1/admin/smartstore-orders/{productOrderId}/actions
Authorization: Bearer {token}
```

- 성공: `200 OK` — 해당 상품 주문의 최근 처리 이력을 최신순 최대 50건 반환한다. 각 이력은 `productOrderId`, `action`, `status`, 요청 요약, 결과 코드·메시지, 요청 관리자 ID·이름, 요청·완료 시각과 nullable 대사 결과·근거·관리자·시각을 포함한다.
- 외부 주문 요청은 호출 전에 `REQUESTED`로 저장한다. 인증 토큰 준비 실패처럼 주문 API를 호출하지 않은 요청은 `NOT_SENT`, 명시적 네이버 거절은 `REJECTED`, 성공은 `SUCCEEDED`, 전송 뒤 통신 실패·본문 누락처럼 실제 처리 여부를 확정할 수 없으면 `RESULT_UNKNOWN`으로 완료한다. 프로세스 종료 등으로 완료 기록을 남기지 못한 요청은 `REQUESTED` 상태가 유지된다.

```http
GET /api/v1/admin/smartstore-orders/actions/unresolved?cursor={cursor}&size=20
Authorization: Bearer {token}
```

- 성공: `200 OK` — 아직 대사하지 않은 `RESULT_UNKNOWN`과 요청 후 5분 넘게 `REQUESTED`인 이력을 최신 요청순 `{content, nextCursor, hasMore}`로 반환한다. `size`는 1~100이고 기본값은 20이다.
- `NOT_SENT`와 `REJECTED`는 네이버 처리 여부가 이미 확정됐으므로 이 목록에 포함하지 않는다.

```http
GET /api/v1/admin/smartstore-orders/{productOrderId}/current-status
Authorization: Bearer {token}
```

- 성공: `200 OK` — 네이버에서 현재 주문·발주·클레임 상태, 잔여 수량, 발송 기한·배송수단·택배사·운송장과 nullable 클레임 상세를 실시간 조회한다.
- 조회 결과는 로컬 주문 원장과 마지막 변경 시각을 갱신하지 않는다. 연동 비활성은 `409 CONFLICT`, 로컬 또는 네이버 주문 미존재는 `404 NOT_FOUND`다.

```http
POST /api/v1/admin/smartstore-orders/actions/{historyId}/reconciliation
Authorization: Bearer {token}
Content-Type: application/json

{
  "outcome": "APPLIED",
  "note": "네이버 판매자센터에서 발주 확인 완료 상태를 확인"
}
```

- `outcome`은 실제 반영을 확인한 `APPLIED` 또는 미반영을 확인한 `NOT_APPLIED`이며, `note`는 확인 근거를 1~500자로 필수 입력한다.
- 성공: `200 OK` — 원래 요청 상태는 바꾸지 않고 대사 결과·근거·관리자·시각이 추가된 처리 이력을 반환한다.
- 이미 대사했거나 아직 5분이 지나지 않은 `REQUESTED`, 결과가 확정된 이력은 `409 CONFLICT`다. 같은 외부 요청을 자동으로 다시 전송하지 않는다.

| 기능 | 메서드와 경로 | 요청 본문 | 성공 |
|---|---|---|---|
| 발주 확인 | `POST /api/v1/admin/smartstore-orders/{id}/confirm` | 없음 | `204` |
| 발송 처리 | `POST /api/v1/admin/smartstore-orders/{id}/dispatch` | `deliveryMethod`, nullable `deliveryCompanyCode`, nullable `trackingNumber`, `dispatchDate` | `204` |
| 발주 일괄 확인 | `POST /api/v1/admin/smartstore-orders/confirm` | `productOrderIds` 최대 30건 | 성공·실패 주문별 결과 |
| 발송 일괄 처리 | `POST /api/v1/admin/smartstore-orders/dispatch` | `orders` 최대 30건, 항목별 발송 정보 | 성공·실패 주문별 결과 |
| 발송 지연 | `POST /api/v1/admin/smartstore-orders/{id}/delay` | `dispatchDueDate`, `reasonCode`, `detailedReason` | `204` |
| 취소 승인 | `POST /api/v1/admin/smartstore-orders/{id}/claims/cancel/approve` | 없음 | `204` |
| 반품 승인 | `POST /api/v1/admin/smartstore-orders/{id}/claims/return/approve` | 없음 | `204` |
| 반품 거부 | `POST /api/v1/admin/smartstore-orders/{id}/claims/return/reject` | 없음 | `204` |
| 반품 보류 | `POST /api/v1/admin/smartstore-orders/{id}/claims/return/hold` | `holdbackClassType`, `detailedReason`, nullable `extraReturnFeeAmount` | `204` |
| 반품 보류 해제 | `POST /api/v1/admin/smartstore-orders/{id}/claims/return/hold/release` | 없음 | `204` |
| 판매자 반품 요청 | `POST /api/v1/admin/smartstore-orders/{id}/claims/return/request` | `returnReason`, `collectDeliveryMethod`, nullable `collectDeliveryCompany`, nullable `collectTrackingNumber`, nullable `returnQuantity` | `204` |
| 교환품 재배송 | `POST /api/v1/admin/smartstore-orders/{id}/claims/exchange/dispatch` | `deliveryMethod`, `deliveryCompanyCode`, `trackingNumber` | `204` |
| 교환 수거 완료 | `POST /api/v1/admin/smartstore-orders/{id}/claims/exchange/collect/complete` | 없음 | `204` |
| 교환 거절 | `POST /api/v1/admin/smartstore-orders/{id}/claims/exchange/reject` | `reason` | `204` |
| 교환 보류 | `POST /api/v1/admin/smartstore-orders/{id}/claims/exchange/hold` | `holdbackClassType`, `detailedReason`, nullable `extraExchangeFeeAmount` | `204` |
| 교환 보류 해제 | `POST /api/v1/admin/smartstore-orders/{id}/claims/exchange/hold/release` | 없음 | `204` |
| 판매자 취소 요청 | `POST /api/v1/admin/smartstore-orders/{id}/claims/cancel/request` | `reason`, nullable `detailedReason`, nullable `quantity` | `204` |

- 서버는 관리자 입력 형식과 연동 활성화 여부만 확인하며, 작업 가능 상태는 최신 상태를 보유한 네이버가 판정한다. 같은 상태 규칙을 로컬에서 중복 구현하지 않는다.
- 외부 요청 중에는 DB 트랜잭션을 열지 않는다. 호출 전 요청 이력을 별도 트랜잭션으로 저장하고 호출 뒤 결과만 짧은 별도 트랜잭션으로 갱신한다. 성공 뒤 로컬 주문 상태는 변경 피드에서 다시 수집한다.
- 일괄 응답은 `successProductOrderIds`와 `failures[{productOrderId, code, message}]`를 반환한다. 일부 실패가 있어도 성공 결과를 유지하고 실패 주문만 다시 선택할 수 있게 한다.

```http
GET /api/v1/admin/smartstore-inquiries?unansweredOnly=true&limit=100
GET /api/v1/admin/smartstore-inquiries/page?from=2026-07-01&to=2026-07-31&unansweredOnly=true&page=0&size=50
GET /api/v1/admin/smartstore-inquiries/template
PUT /api/v1/admin/smartstore-inquiries/{questionId}/answer
GET /api/v1/admin/smartstore-inquiries/customers?unansweredOnly=true&limit=100
GET /api/v1/admin/smartstore-inquiries/customers/page?from=2026-07-01&to=2026-07-31&unansweredOnly=true&page=0&size=50
PUT /api/v1/admin/smartstore-inquiries/customers/{inquiryNo}/answer
PUT /api/v1/admin/smartstore-inquiries/customers/{inquiryNo}/answer/{answerContentId}
Authorization: Bearer {token}
```

- 기존 배열 목록 API는 최근 30일 네이버 상품 문의와 주문·배송 고객 문의를 구분해 최대 200건 반환한다. 상품 문의는 최대 두 페이지, 고객 문의는 한 페이지를 읽으며 전체 페이지를 미리 수집하지 않는다.
- 페이지 목록 API의 operationId는 상품 문의 `listSmartStoreInquiriesPage`, 고객 문의 `listSmartStoreCustomerInquiriesPage`다. 필수 `from`, `to`는 `yyyy-MM-dd`이며 양 끝 날짜를 포함한다. 상품 문의는 한국 시간 `00:00:00`부터 `23:59:59.999`까지, 고객 문의는 날짜 그대로 전달한다. 시작일이 종료일보다 늦으면 `400 INVALID_INPUT`이며 별도 최대 기간은 정하지 않는다.
- 페이지 요청은 `page` 0~999999(기본 0), `size` 10~100(기본 50), `unansweredOnly` 기본 `true`다. 네이버에는 페이지 번호를 1부터 전달하고, 미답변 조회일 때만 `answered=false`를 보낸다. 응답은 `{content, page, size, totalCount, totalPages}`이며 `content`는 기존 문의 DTO 배열이다. 합계는 네이버 응답을 사용하고 선택한 한 페이지만 요청한다.
- 템플릿 조회는 네이버가 제공한 단일 상품 문의 템플릿의 `questionType`, `subject`, `content`를 반환한다. 답변 요청은 모두 `{ "content": "..." }`를 받고 성공 시 `204`를 반환한다.
- 상품 문의의 `PUT /{questionId}/answer`는 신규 답변 등록과 기존 답변 수정에 함께 사용한다. 관리자 화면은 기존 본문을 편집하고, 취소 시 요청을 보내지 않으며 저장 실패 시 초안을 유지한다. 조회 실패는 목록·템플릿 영역의 재시도로 처리하고 문의 유형 탭과 조회 조건은 계속 표시한다.
- 고객 문의 목록의 `answerContentId`는 최근 답변번호이며 답변이 없으면 `null`이다. 수정은 문의번호와 답변번호(각각 1 이상)를 경로에 지정하고 기존 답변과 같은 본문 계약을 사용한다. 연동 비활성화는 `409 CONFLICT`, 빈 답변이나 잘못된 번호는 `400 INVALID_INPUT`이다. 수정 가능 상태는 네이버가 최종 판정한다.
- 네이버 커머스 API는 후기 조회를 제공하지 않으므로 후기 연동은 포함하지 않는다.

#### 2.3.8 상품 표시 정보 수정

```http
PATCH /api/v1/admin/products/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "시그니처 캔들 리뉴얼",
  "category": "CANDLE",
  "price": 42000,
  "description": "리뉴얼한 향과 용기를 적용했습니다.",
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
  "specification": "소이 왁스 220g · 내열 유리 용기",
  "careInstructions": "심지를 5mm로 정리해 주세요.",
  "productionLeadDays": null,
  "optionGroups": [],
  "variants": []
}
```

- 성공: `200 OK`, 현재 재고를 포함한 `ProductResponse` 반환
- 상품 유형과 판매 상태는 이 API에서 바꾸지 않는다. 기성품 재고와 주문제작 variant 재고의 수동 변경은 재고 조정 API를 사용한다.
- 주문제작은 현재 전체 `optionGroups`, `variants`를 보내 옵션 구성을 교체한다. 서버가 가능한 조합의 누락·중복을 검증하고 유지된 조합은 같은 선택 키로 식별한다.
- 기존 조합은 가격 추가금·판매 여부만 수정하며 `variants[].quantity`나 `quantity`에 과거 수량을 보내도 현재 재고를 보존한다. 재고는 기본 조합을 포함해 재고 조정 API로만 변경한다. 새로 만드는 조합에만 `variants[].quantity`를 최초 재고로 사용하며, 선택형과 `variants`가 모두 없는 신규 기본 조합은 `quantity`를 사용한다.
- 선택형 그룹의 표시 순서는 조합 식별에 포함하지 않는다. 그룹·값 키가 같으면 조합 번호와 재고를 유지한다. 관리자 화면에서는 기존 조합 재고를 읽기 전용으로 보여 주고 신규 조합만 최초 재고를 입력받는다.
- 이미 결제된 주문은 `order_items`의 상품명·기본가·옵션 추가금·최종 단가·선택 옵션·직접입력 문구·고정 사양·관리 방법·예상 제작 기간 스냅샷을 사용하므로 이후 상품 변경의 영향을 받지 않는다.

### 2.4 예약 API

#### 2.4.1 휴대폰 인증 코드 발송

```http
POST /api/v1/bookings/phone-verifications

{
  "phone": "01012345678",
  "purpose": "GUEST_BOOKING"
}
```

```json
{
  "verificationId": 1,
  "phone": "01012345678"
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 전화번호 형식 불일치 (`^01[0-9]{8,9}$`)
  - `503 SERVICE_UNAVAILABLE` — 인증 코드를 저장했지만 SMS 발송이 실패하거나 발송 실행기가 요청을 수용하지 못함
- 정책:
  - `purpose`는 `SIGNUP`, `PASSWORD_RESET`, `MEMBER_PHONE_REGISTRATION`, `MEMBER_PHONE_CHANGE`, `GUEST_BOOKING`, `GUEST_ORDER`, `GUEST_CLAIM`, `GUEST_RECORD_RECOVERY`, `GUEST_PAYMENT_STATUS_RECOVERY` 중 하나다.
  - 인증 코드는 응답과 서버 로그에 포함하지 않는다.
  - 인증 코드는 독립 트랜잭션으로 먼저 저장하고 외부 SMS는 트랜잭션 밖에서 호출한다. NHN이 발송 요청을 정상 접수했다고 기록된 코드만 발급 당시 `purpose`에서 사용할 수 있다.
  - 발송 요청 실패 응답 뒤 재요청하면 새 코드를 발급한다. 발급 ID가 더 큰 코드의 접수 완료만 같은 전화번호·같은 목적의 이전 미소모 코드를 무효화하며, 이전 요청의 접수 완료가 늦게 돌아와도 최신 코드 상태를 덮지 않는다.
  - 모든 인증 코드 소비 경로는 정규화 전화번호별 확인 시도 제한을 공통 적용하며 Redis 장애 시 fail-closed한다.
  - 개발/테스트 환경에서는 `GET /api/v1/admin/dev/phone-verifications/latest?phone=&purpose=`로 같은 전화번호·목적의 최신 코드를 조회할 수 있다.

#### ~~2.4.2 게스트 예약 생성~~ (2026-04-22 제거)

> 예약 생성은 `POST /api/v1/payments/prepare` (`context=BOOKING`) → `POST /api/v1/payments/confirm`으로 단일화됨. 2.15 결제 API 참조.

#### 2.4.3 비회원 예약 조회

```http
GET /api/v1/bookings/{bookingId}
X-Access-Token: {accessToken}
```

```json
{
  "bookingId": 1,
  "bookingNumber": "BK-00000001",
  "classId": 1,
  "slotId": 42,
  "startAt": "2026-03-01T10:00:00",
  "endAt": "2026-03-01T12:00:00",
  "className": "향수 클래스",
  "status": "BOOKED",
  "participantCount": 3,
  "depositAmount": 15000,
  "balanceAmount": 135000,
  "guestName": "홍길동",
  "guestPhone": "010****5678",
  "cancelPolicy": {
    "cancellable": true,
    "refundable": true,
    "deadlineAt": "2026-03-01T00:00:00",
    "passCreditRestorable": false,
    "manualCompensationRequired": false,
    "warningCode": null
  },
  "refund": null
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — bookingId 미존재 또는 token 불일치
- `cancelPolicy.cancellable`은 고객이 현재 예약을 직접 취소할 수 있는지를 뜻한다. 잔금 결제가 완료된 유료 예약은 `false`며 관리자 정산이 필요하다.
- `cancelPolicy.refundable`은 지금 취소하면 예약금 환불 또는 8회권 크레딧 복구가 가능한지를 뜻한다.
- `cancelPolicy.deadlineAt`은 체험일 00:00 KST 기준 취소 보상 마감 시각이다.
- 8회권 예약에서 마감이 지났으면 `cancelPolicy.warningCode=PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE`을 내린다. 프론트는 이 코드를 사용자에게 노출하지 않고 크레딧 미복구 한국어 경고로 변환해 취소 전에 표시한다.
- 환불 이력이 있으면 `refund`에 `amount`, `status`를 반환하고, 없으면 `null`이다. 고객 응답에는 `refundId`, 실패 사유, 시도 횟수를 노출하지 않는다.

#### 2.4.4 예약 변경

```http
PATCH /api/v1/bookings/{bookingId}/reschedule
X-Access-Token: {accessToken}

{
  "newSlotId": 43
}
```

```json
{
  "bookingId": 1,
  "bookingNumber": "BK-00000001",
  "slotId": 43,
  "startAt": "2026-03-01T14:00:00",
  "endAt": "2026-03-01T16:00:00",
  "className": "향수 클래스",
  "status": "BOOKED",
  "participantCount": 3
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 동일 슬롯 또는 다른 클래스의 슬롯으로 변경 시도
  - `404 NOT_FOUND` — 예약 미존재 또는 token 불일치
  - `409 CAPACITY_EXCEEDED` — 새 슬롯 정원 초과
  - `409 DUPLICATE_BOOKING` — 동일 전화번호 + 새 슬롯에 활성 예약이 이미 존재
  - `409 SLOT_NOT_AVAILABLE` — 새 슬롯 비활성
  - `409 BOOKING_CONFLICT` — 낙관적 락 충돌
  - `422 CHANGE_NOT_ALLOWED` — 현재 슬롯 시작 1시간 이내
- 정책:
  - 현재 슬롯 시작 1시간 전까지 횟수 제한 없이 변경 가능
  - 현재 예약과 같은 클래스의 활성·미래·예약 가능 슬롯으로만 변경 가능
  - 기존 예약의 `participantCount`만큼 새 슬롯 정원을 점유하고 이전 슬롯에서 같은 인원을 반납한다.
  - 변경마다 `booking_history`에 `RESCHEDULED` 이력 누적
  - `bookings` 행은 항상 1건 유지한다.

#### 2.4.5 비회원 예약 인원 부분취소

```http
PATCH /api/v1/bookings/{bookingId}/participants
X-Access-Token: {accessToken}

{
  "participantCount": 2
}
```

```json
{
  "bookingId": 1,
  "status": "BOOKED",
  "participantCount": 2,
  "canceledParticipantCount": 1,
  "depositAmount": 10000,
  "balanceAmount": 90000,
  "refundAmount": 5000,
  "refund": {
    "amount": 5000,
    "status": "REQUESTED"
  }
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 변경 인원이 1명 미만이거나 현재 인원 이상
  - `404 NOT_FOUND` — 예약 미존재 또는 token 불일치
  - `409 BOOKING_CONFLICT` — 인원 변경 중 예약 슬롯이 바뀌거나 동시 수정 충돌
  - `422 CHANGE_NOT_ALLOWED` — 취소 보상 마감 경과, 잔금 결제 완료, 8회권 예약 또는 오프라인 예약금 예약
- 정책:
  - 예약은 `BOOKED`를 유지하며 1명 이상만 남길 수 있다. 전원 취소는 전체 예약 취소 API를 사용한다.
  - 취소 보상 마감 전의 PG 예약금 결제 예약만 고객이 직접 인원을 줄일 수 있다.
  - 현재 예약금과 잔금을 변경 전 인원 대비 변경 후 인원 비율로 정수 내림해 다시 계산하고, 줄어든 예약금만 PG 부분환불로 요청한다.
  - 줄인 인원만큼 슬롯 정원을 즉시 반납하고 `PARTICIPANTS_REDUCED` 이력을 누적한다.
  - 같은 예약을 여러 번 줄일 수 있으며 각 요청은 별도 환불 이력을 가진다. 응답의 `refund`는 이번 요청의 환불 진행 상태다.
  - 회원은 같은 계약을 `PATCH /api/v1/me/bookings/{id}/participants`로 사용하며 `X-Access-Token` 대신 회원 세션 소유권을 검증한다.

#### 2.4.6 비회원 예약 취소

```http
DELETE /api/v1/bookings/{bookingId}
X-Access-Token: {accessToken}
```

```json
{
  "bookingId": 1,
  "status": "CANCELED",
  "participantCount": 3,
  "refundable": true,
  "refundAmount": 15000,
  "refund": {
    "amount": 15000,
    "status": "REQUESTED"
  },
  "manualCompensationRequired": false
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — bookingId 미존재 또는 token 불일치
  - `400 INVALID_INPUT` — `BOOKED` 상태가 아닌 예약 취소 시도
  - `422 CHANGE_NOT_ALLOWED` — 잔금 결제가 완료되어 관리자 정산이 필요한 예약의 고객 취소 시도
- 환불 정책:
  - 예약금 결제: `refundable=true`이면 PG 환불 요청
  - 8회권 결제: `refundable=true`이면 `REFUND` ledger와 remaining credit 복구
  - `refundable=false`이면 크레딧 소멸 유지
  - `200 OK`는 예약 취소와 환불 요청 이력 저장 완료를 뜻하며 PG 환불 완료를 뜻하지 않는다.
  - 예약금 환불을 요청했을 때만 `refund`가 `{amount,status}`로 채워진다. 8회권 크레딧 복구 또는 환불 불가 취소에서는 `refund=null`, `refundAmount=0`이다.
  - 운영자 수기 예약에서 받은 오프라인 예약금은 PG 거래가 없으므로 `manualCompensationRequired=true`와 관리자 후속 작업을 남기고 `refund=null`이다.

### 2.5 8회권 API

#### ~~2.5.1 게스트 8회권 구매~~ (2026-03-19 제거)

> 8회권 구매는 회원 전용으로 전환됨. 현재 구매 생성은 `POST /api/v1/payments/prepare` (`context=PASS`) → `POST /api/v1/payments/confirm`으로 처리한다. 2.15 결제 API 참조.
> 비회원 소유 8회권 상태는 지원하지 않는다.

#### ~~2.5.2 휴대폰 인증 기반 8회권 구매~~ (2026-03-19 제거)

> 상동. 회원 8회권 구매는 결제 API `context=PASS`로 단일화.

#### 2.5.3 결석 처리

```http
POST /api/v1/admin/bookings/{bookingId}/no-show
Authorization: Bearer {token}
```

```json
{
  "bookingId": 1,
  "status": "NO_SHOW"
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — bookingId 미존재
  - `400 INVALID_INPUT` — `BOOKED` 상태가 아니거나 수업 종료 전인 예약
- 정책:
  - 서버 `Clock` 기준으로 슬롯 종료 시각에 도달한 뒤에만 처리할 수 있다.
  - 크레딧은 예약 시 `USE` ledger로 이미 소모되어 추가 변동이 없다.

#### 2.5.3.1 관리자 8회권 검색·상세 조회

```http
GET /api/v1/admin/passes/search?keyword=01096355608&page=0&size=20
Authorization: Bearer {token}
```

```json
{
  "content": [
    {
      "passId": 12,
      "passNumber": "PASS-00000012",
      "customerName": "홍길동",
      "customerPhone": "010****5608",
      "status": "ACTIVE",
      "remainingCredits": 5,
      "totalCredits": 8,
      "expiresAt": "2026-06-20T00:00:00",
      "futureBookingCount": 2,
      "expectedRefundAmount": 210000,
      "refundStatus": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalCount": 1,
  "totalPages": 1
}
```

```http
GET /api/v1/admin/passes/{passId}
Authorization: Bearer {token}
```

- 성공: `200 OK`
- 에러:
  - `401 UNAUTHORIZED` — 관리자 인증 실패
  - `404 NOT_FOUND` — 상세 `passId` 미존재
- 정책:
  - 검색어는 선택값이다. 이름·정규화한 전화번호는 HMAC 정확 일치, `PASS-{8자리 숫자}`는 ID 정확 일치를 지원한다. PK 인덱스를 사용할 수 없는 일반 숫자 ID 부분 검색은 제공하지 않는다.
  - `status`는 `ACTIVE`, `USED_UP`, `EXPIRED`, `REFUND_PENDING`, `REFUND_FAILED`, `REFUNDED` 중 하나다. 환불 이력이 있으면 환불 상태를 이용권 상태보다 우선한다.
  - `expectedRefundAmount`는 현재 잔여 횟수와 자동 취소될 미래 `BOOKED` 예약 수를 합하되 총 횟수를 넘지 않도록 계산한다. 이미 환불 요청이 있으면 저장된 요청 금액을 반환한다.
  - 전화번호는 가운데 자리를 마스킹한다. 목록에서 상세를 선택한 뒤 2.5.4의 환불 액션을 실행한다.

#### 2.5.4 8회권 전체 환불

```http
POST /api/v1/admin/passes/{passId}/refund
Authorization: Bearer {token}
```

관리자 응답은 운영용 환불 식별자를 포함한다.

```json
{
  "canceledBookings": 2,
  "refundCredits": 8,
  "refundAmount": 240000,
  "refundId": 42,
  "refundStatus": "REQUESTED"
}
```

이용권 소유 회원은 같은 정산 흐름을 아래 경로로 호출한다. 회원 응답에는 내부 환불 식별자를 포함하지 않는다.

```http
POST /api/v1/me/passes/{passId}/refund
Cookie: HG_SESSION={sessionToken}
```

```json
{
  "canceledBookings": 2,
  "refundCredits": 8,
  "refundAmount": 240000,
  "refundStatus": "REQUESTED"
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — passId 미존재
  - `401 UNAUTHORIZED` — 회원 경로에 세션 없음
  - `422 PASS_EXPIRED` — 만료된 8회권 환불 요청. 남아 있던 크레딧은 `EXPIRE` 처리되고 환불·미래 예약 취소는 실행하지 않음
  - `429 TOO_MANY_REQUESTS` — 회원 환불 요청 처리율 제한 초과
- 정책:
  - 미래 `BOOKED` 예약 자동 취소
  - `refundCredits = remainingCredits + 자동 취소한 미래 예약 수`
  - `refundAmount = (totalPrice × refundCredits) / totalCredits` (원 단위 미만 버림, 전체 횟수 환불은 원결제액과 동일)
  - `REFUND` ledger 기록 후 `remaining_credits = 0`
  - `payment_key` 기반 PG 환불 요청 이력을 `refunds`에 `REQUESTED`로 남기고, 부모 트랜잭션 커밋 이후 PG 환불을 실행
  - PG 결과는 비동기로 `SUCCEEDED`, `FAILED`, `RETRYABLE`, `RECONCILIATION_REQUIRED` 중 하나에 반영된다. 미완료 상태는 같은 멱등키로 자동 복구하며 운영자가 수동 재처리할 수도 있다.
  - `200 OK`와 `refundStatus=REQUESTED`는 미래 예약 취소·크레딧 정산·환불 요청 접수 완료를 뜻한다. `refundAmount=0`이면 관리자 응답의 `refundId`와 두 응답의 `refundStatus`는 `null`이며 PG 환불은 실행하지 않는다.
  - 관리자 환불 응답의 `refundId`는 `GET /api/v1/admin/refunds/{refundId}`로 실제 PG 처리 상태를 조회한다. 회원은 관리자 API를 사용하지 않고 `GET /api/v1/me/passes` 또는 `GET /api/v1/me/passes/{passId}`의 `refund.amount`, `refund.status`로 자신의 환불 진행 상태를 확인한다.
  - 단가 = `totalPrice / totalCredits`

#### 2.5.5 만료 배치 수동 트리거

```http
POST /api/v1/admin/passes/expire
Authorization: Bearer {token}
```

```json
{
  "successCount": 3,
  "failureCount": 0,
  "failureReasons": {}
}
```

- 성공: `200 OK`
- 정책:
  - `expiresAt <= now`인 pass의 `remaining_credits = 0`, `EXPIRE` ledger 기록
  - `failureReasons`는 내부 예외명을 그대로 노출하지 않고 `CONFLICT`, `NOT_FOUND`, `ALREADY_PROCESSED`, `BUSINESS_ERROR`, `INTERNAL_ERROR`로 정규화한다.

### 2.6 사용자 주문 API

#### ~~2.6.1 주문 생성~~ (2026-04-22 제거)

> 주문 생성은 `POST /api/v1/payments/prepare` (`context=ORDER`) → `POST /api/v1/payments/confirm`으로 단일화됨. 2.15 결제 API 참조.

#### 2.6.2 주문 상세 조회

```http
GET /api/v1/orders/{orderId}
X-Access-Token: {accessToken}
```

```json
{
  "orderId": 12,
  "orderNumber": "ORD-00000012",
  "status": "PAID_APPROVAL_PENDING",
  "totalAmount": 111000,
  "productAmount": 118000,
  "shippingFee": 3000,
  "couponDiscountAmount": 10000,
  "rewardUsedAmount": 5000,
  "pgPaidAmount": 106000,
  "rewardEarnBase": 103000,
  "issuedCouponId": 81,
  "paidAt": "2026-03-08T20:30:00",
  "approvalDeadlineAt": "2026-03-09T20:30:00",
  "items": [
    {
      "orderItemId": 21,
      "productId": 1,
      "productName": "시그니처 캔들",
      "productType": "READY_STOCK",
      "qty": 2,
      "unitPrice": 39000,
      "grossAmount": 78000,
      "couponDiscountAmount": 6610,
      "rewardUsedAmount": 3305,
      "netPaidAmount": 68085,
      "specification": "소이 왁스 200g · 유리 용기",
      "careInstructions": "첫 사용은 표면 전체가 녹을 때까지 태워 주세요.",
      "productionLeadDays": null
    },
    {
      "orderItemId": 22,
      "productId": 3,
      "productName": "우드 트레이",
      "productType": "MADE_TO_ORDER",
      "qty": 1,
      "unitPrice": 40000,
      "grossAmount": 40000,
      "couponDiscountAmount": 3390,
      "rewardUsedAmount": 1695,
      "netPaidAmount": 34915,
      "specification": "월넛 300×200mm",
      "careInstructions": "물에 오래 담가 두지 마세요.",
      "productionLeadDays": 14
    }
  ],
  "fulfillment": {
    "type": "SHIPPING",
    "expectedShipDate": null,
    "pickupDeadlineAt": null,
    "carrier": null,
    "trackingNumber": null,
    "shippingAddress": {
      "recipientName": "홍길동",
      "phone": "01012345678",
      "postalCode": "27360",
      "addressLine1": "충북 충주시 계명대로 1",
      "addressLine2": "101호"
    }
  },
  "refund": null
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — orderId 미존재 또는 token 불일치
- 정책:
  - 비회원 접근 토큰은 HMAC 서명과 만료 시각을 검증한 뒤 서명 토큰 전체의 SHA-256 해시를 DB 저장값과 비교한다. 서명 없는 32자 16진수 토큰은 허용하지 않으며, 신규 토큰에서 추출한 nonce만으로 서명·만료 검사를 우회할 수 없다.
  - 비회원·회원 주문 상세는 수령인 이름·전화·주소를 포함하므로 `Cache-Control: no-store`로 반환한다.
  - 신규 주문의 `fulfillment`는 결제 confirm 시 함께 생성되며 고객이 선택한 `type`, 예상 출고일, 픽업 마감, 배송 추적 정보와 배송지를 반환한다. 배송 출발 뒤에는 `carrierCode`, `carrier`, `trackingNumber`, 외부 배송조회 등록 상태, 현재 택배 상태·표시 문구·갱신 시각과 시간순 `trackingEvents`를 반환한다. 배송지는 소유권이 확인된 상세에서만 복호화하며 `PICKUP`은 `shippingAddress=null`이다.
  - `shippingFee`는 prepare 당시 서버 정책 스냅샷이다. `productAmount`는 할인 전 상품 합계, `totalAmount`는 상품 합계와 배송비에서 쿠폰만 차감한 금액, `pgPaidAmount`는 여기서 적립금까지 차감해 PG로 승인한 금액이다. 픽업 주문의 배송비는 0원이다.
  - 쿠폰은 배송비를 제외한 상품 금액에 회원당 1장만 적용한다. `rewardEarnBase`는 상품 금액에서 쿠폰 할인과 적립금 사용을 뺀 신규 적립 기준이며, `issuedCouponId`는 쿠폰을 쓰지 않은 주문에서 `null`이다.
  - 각 항목의 `grossAmount`, `couponDiscountAmount`, `rewardUsedAmount`, `netPaidAmount`는 주문 전체 혜택을 원 단위로 비례 배분한 불변 스냅샷이다. 항목 합계는 주문의 상품·쿠폰·적립금·적립 기준 금액과 각각 일치한다.
  - 각 항목의 `productName`, `productType`, `unitPrice`, `specification`, `careInstructions`, `productionLeadDays`는 prepare 당시 스냅샷이다. 스냅샷 도입 전 주문은 `productType`과 구매조건 필드가 `null`일 수 있다.
  - 환불 이력이 있으면 `refund`에 고객 반환 총액 `amount`, `pgRefundAmount`, `rewardRestoreAmount`, `rewardRevokeAmount`, `restoreCoupon`, `status`를 반환하고, 없으면 `null`이다. 고객 응답에는 `refundId`, 실패 사유, 시도 횟수를 노출하지 않는다.
  - 주문 전액 취소는 PG 결제액과 사용 적립금을 각각 취소·복원하고, 취소 시점에도 유효한 쿠폰만 다시 사용할 수 있게 한다. 사용 쿠폰이 이미 만료됐다면 상태를 `EXPIRED`로 바꾸되 원 결제 시도·주문 연결과 사용 시각은 감사 이력으로 유지한다.
  - `status=PICKUP_EXPIRED`는 관리자 예외 환불 또는 정책 변경 전 자동 환불된 미수령 주문이며 `refund`에 진행 상태를 반환한다. `status=PICKUP_FORFEITED`는 상품 유형과 관계없는 미환불 미수령 종료이며 `refund=null`이다.
  - `DELETE /api/v1/orders/{id}`는 비회원 접근 토큰으로, `DELETE /api/v1/me/orders/{id}`는 회원 세션으로 본인 주문을 확인한다. `PAID_APPROVAL_PENDING`만 `CUSTOMER_CANCELED`로 전이하고 재고 복구·이력·환불 요청을 함께 처리한다.
  - `POST /api/v1/orders/{id}/delay-response`와 `POST /api/v1/me/orders/{id}/delay-response`는 `{ "decision": "ACCEPT|REJECT" }`를 받는다. `DELAY_CONSENT_PENDING`에서 수락하면 `DELAY_ACCEPTED`, 거절하면 `DELAY_REJECTED_CANCELED`와 전액 환불 요청으로 전이한다.
  - 고객 액션 응답은 `{orderId,status,refund}`다. `refund`가 `REQUESTED`여도 PG 완료를 뜻하지 않으며 주문 상세에서 진행 상태를 다시 확인한다.

#### 2.6.3 주문 가격 정책 조회

```http
GET /api/v1/orders/policy
```

```json
{
  "shippingFee": 3000,
  "madeToOrderConsentVersion": "2026-07-21-v1",
  "madeToOrderConsentText": "주문제작 상품은 결제 후 관리자 승인으로 제작이 시작되면 ..."
}
```

- 인증 없이 결제 전 고정 배송비를 조회한다. 실제 결제 금액은 prepare 시 서버 설정으로 다시 확정한다.
- `PICKUP`은 이 값과 무관하게 배송비 0원이다. 현재 무료 배송 임계값은 없고 운영 기본값은 `ORDER_SHIPPING_FEE=0`이다.
- 주문제작 상품이 포함된 화면은 같은 응답의 동의 문구를 별도 필수 체크로 표시한다. 클라이언트는 prepare에 조회한 `madeToOrderConsentVersion`과 `madeToOrderConsent=true`를 제출한다. 서버 현재 버전과 다르면 새 안내를 다시 조회하도록 `400 INVALID_INPUT`으로 거절한다.

#### 2.6.4 주문 클레임 접수·조회

회원:

```http
GET|POST /api/v1/me/orders/{orderId}/claims
Cookie: HG_SESSION={sessionToken}
```

비회원:

```http
GET|POST /api/v1/orders/{orderId}/claims
X-Access-Token: {accessToken}
```

POST 요청:

```json
{
  "type": "DAMAGED",
  "requestedResolution": "REFUND",
  "reason": "수령한 상품이 파손되었습니다.",
  "items": [{ "orderItemId": 21, "quantity": 1 }]
}
```

- `type`: `DAMAGED`, `WRONG_ITEM`, `CHANGE_OF_MIND`, `OTHER`
- `requestedResolution`: `REFUND`, `EXCHANGE`
- 성공: POST `200 OK` 단건, GET `200 OK` 최신 접수순 목록
- 정책:
  - `DELIVERED`, `PICKED_UP`, `COMPLETED` 주문만 접수한다.
  - 한 요청의 `items`는 1~100건이다.
  - 거절되지 않은 기존 접수 수량을 제외해 주문 항목의 결제 수량을 초과할 수 없다.
  - 응답은 클레임 상태, 관리자 메모, 품목명·단가·수량, 최대 환불액, nullable 환불 금액·상태·교환 배송 정보를 포함한다.
  - 상태는 `REQUESTED`, `REFUND_REQUESTED`, `EXCHANGE_APPROVED`, `REJECTED`, `COMPLETED`다.

### 2.7 주문 Admin API

#### 2.7.1 관리자 주문 목록 조회

```http
GET /api/v1/admin/orders?status=PAID_APPROVAL_PENDING&cursor=MjAyNi0wMy0yNFQxMTo0MDozMHwxMjM&size=20
Authorization: Bearer {token}
```

```json
{
  "content": [
    {
      "orderId": 123,
      "orderNumber": "ORD-00000123",
      "status": "PAID_APPROVAL_PENDING",
      "totalAmount": 121000,
      "shippingFee": 3000,
      "fulfillmentType": "SHIPPING",
      "items": [
        { "productId": 1, "productName": "시그니처 캔들", "productType": "READY_STOCK", "qty": 2, "unitPrice": 39000 },
        { "productId": 3, "productName": "우드 트레이", "productType": "MADE_TO_ORDER", "qty": 1, "unitPrice": 40000 }
      ],
      "paidAt": "2026-03-24T11:32:10",
      "createdAt": "2026-03-24T02:32:10Z"
    }
  ],
  "nextCursor": "MjAyNi0wMy0yNFQxMTozMjoxMHwxMjM",
  "hasMore": true
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — `size`가 1~100 범위를 벗어난 경우
- 정책:
  - 상태 필터가 없으면 전체 주문을 `createdAt DESC, id DESC` 기준으로 조회한다.
  - DB 생성 시각인 `createdAt`은 UTC 오프셋(`Z`)을 포함하고, 결제·승인 마감 같은 업무 시각은 서울 현지시각으로 반환한다.
  - `cursor`는 `Base64("{ISO_LOCAL_DATE_TIME}|{id}")` 형식이다.
  - 프론트는 `hasMore=true`일 때만 `nextCursor`로 다음 페이지를 요청한다.
  - 목록에는 운영 분기를 위한 `fulfillmentType`만 포함하며 배송지 개인정보는 포함하지 않는다.

#### 2.7.1.1 관리자 주문 이행 상세 조회

```http
GET /api/v1/admin/orders/{id}/fulfillment
Authorization: Bearer {token}
```

```json
{
  "orderId": 123,
  "type": "SHIPPING",
  "shippingAddress": {
    "recipientName": "홍길동",
    "phone": "01012345678",
    "postalCode": "06236",
    "addressLine1": "서울시 강남구 테헤란로 1",
    "addressLine2": "2층"
  },
  "expectedShipDate": null,
  "pickupDeadlineAt": null,
  "carrier": null,
  "trackingNumber": null
}
```

- 관리자 인증 후에만 배송지 암호문을 복호화한다. `PICKUP` 주문은 `shippingAddress=null`이다.

- 개인정보가 포함되므로 응답은 `Cache-Control: no-store`로 반환한다.

#### 2.7.2 관리자 주문 검색

```http
GET /api/v1/admin/orders/search?status=PAID_APPROVAL_PENDING&dateFrom=2026-03-20&dateTo=2026-03-24&keyword=홍길동&page=0&size=20
Authorization: Bearer {token}
```

```json
{
  "content": [
    {
      "orderId": 123,
      "orderNumber": "ORD-00000123",
      "status": "PAID_APPROVAL_PENDING",
      "totalAmount": 118000,
      "buyerName": "홍길동",
      "buyerPhone": "01012345678",
      "paidAt": "2026-03-24T11:32:10",
      "approvalDeadlineAt": "2026-03-25T11:32:10",
      "createdAt": "2026-03-24T02:32:10Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalCount": 37,
  "totalPages": 2
}
```

- 성공: `200 OK`
- 정책:
  - `status`, `dateFrom`, `dateTo`, `keyword`는 모두 선택 필터다.
  - `page`는 0 미만이면 0으로, `size`는 1~100 범위로 보정하며 표현 가능한 OFFSET을 넘으면 `400 INVALID_INPUT`으로 거절한다.
  - `keyword`가 `ORD-{숫자}` 형식의 주문번호이면 해당 주문 ID와 정확 일치로 검색한다. 국내 휴대폰 형식이면 정규화한 전화 HMAC, 그 외 문자열은 회원·비회원 이름 HMAC 정확 일치로 검색하며 개인정보 부분 검색과 주문 ID 부분 검색은 제공하지 않는다.
  - `dateFrom`~`dateTo`는 KST 기준 주문 생성일 범위를 의미한다.
  - 결과는 `createdAt DESC` 기준 OFFSET 페이지로 반환한다.
  - `createdAt`은 UTC 오프셋(`Z`)을 포함해 브라우저가 서울 시각으로 정확히 변환할 수 있게 한다.
  - 관리자 화면의 검색 결과는 `orderId`와 `status`를 사용해 주문 탭의 기존 운영 패널로 이동하고 대상 주문을 강조한다.

#### 2.7.3 주문 운영 엔드포인트

- `POST /api/v1/admin/orders/{id}/approve`
  - 응답: `200 OK` 본문 없음
  - 정책:
    - `PAID_APPROVAL_PENDING`만 승인 가능
    - `MADE_TO_ORDER` 상품이 있으면 `IN_PRODUCTION`, 아니면 `APPROVED_FULFILLMENT_PENDING`으로 전이
    - 이력의 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.
- `POST /api/v1/admin/orders/{id}/reject`
  - 응답: `200 OK`
    ```json
    {
      "orderId": 5,
      "orderStatus": "REJECTED",
      "refund": {
        "refundId": 42,
        "amount": 118000,
        "status": "REQUESTED",
        "attemptCount": 0,
        "failReason": null
      }
    }
    ```
  - 정책:
    - 승인 대기 주문만 거절 가능
    - 재고 복구 + 환불 실행 + `REJECTED` 전이
    - 응답의 `REQUESTED`는 로컬 거절과 환불 요청 접수 완료를 뜻하며 PG 환불 완료를 뜻하지 않는다.
- `PATCH /api/v1/admin/orders/{id}/expected-ship-date`
  - 요청: `{ "expectedShipDate": "2026-04-15" }`
  - 응답: `{ "orderId": 5, "status": "IN_PRODUCTION", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - `IN_PRODUCTION`, `DELAY_CONSENT_PENDING`, `DELAY_ACCEPTED`, `SHIPPING_PREPARING` 상태에서만 설정 가능
    - `SHIPPING` 타입 fulfillment에서만 설정 가능 (`PICKUP` 타입은 400)
    - 설정·갱신마다 `SHIP_DATE_UPDATED` 이력을 추가한다. adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이고, `reason`은 `예상 출고일: {변경 전} -> {변경 후}` 형식이다. 날짜가 없으면 `미설정`으로 기록한다.
- `POST /api/v1/admin/orders/{id}/delay`
  - 응답: `{ "orderId": 5, "status": "DELAY_CONSENT_PENDING", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - 기성품의 승인 전 재고 부족은 `PAID_APPROVAL_PENDING`, 주문제작의 제작 일정 변경은 `IN_PRODUCTION`에서 지연 제안 가능
    - `DELAY` 이력을 추가하며 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.
    - 고객 동의가 끝난 것으로 간주하지 않는다. 회원·비회원 고객 응답 API가 수락하면 `DELAY_ACCEPTED`, 거절하면 `DELAY_REJECTED_CANCELED`로 전이한다.
    - 지연 동의 요청 알림 이벤트명은 사건 의미를 나타내는 `ORDER_DELAY_REQUESTED`를 유지한다.
- `POST /api/v1/admin/orders/{id}/cancel-for-delay-rejection`
  - 응답:
    ```json
    {
      "orderId": 5,
      "orderStatus": "DELAY_REJECTED_CANCELED",
      "expectedShipDate": "2026-04-15",
      "refund": {
        "refundId": 43,
        "amount": 118000,
        "status": "REQUESTED",
        "attemptCount": 0,
        "failReason": null
      }
    }
    ```
  - 정책:
    - 고객이 제안된 지연을 수락하기 전에 거절한 경우 사용한다.
    - `DELAY_CONSENT_PENDING`에서만 지연 거절 취소 가능
    - 재고 복구 + 환불 실행 + `DELAY_REJECTED_CANCELED` 전이
    - 응답의 `REQUESTED`는 로컬 취소와 환불 요청 접수 완료를 뜻하며 PG 환불 완료를 뜻하지 않는다.
    - 이력은 `DELAY_CANCEL`로 기록하고 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.
    - `DELAY_ACCEPTED`는 이미 지연을 수락한 상태이므로 400으로 거절한다.
- `POST /api/v1/admin/orders/{id}/resume-after-delay`
  - 기성품 응답: `{ "orderId": 5, "status": "APPROVED_FULFILLMENT_PENDING", "expectedShipDate": null }`
  - 주문제작 응답: `{ "orderId": 6, "status": "IN_PRODUCTION", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - `DELAY_ACCEPTED`에서만 주문 처리 재개 가능
    - 기성품은 배송·픽업 이행 대기인 `APPROVED_FULFILLMENT_PENDING`, 주문제작은 `IN_PRODUCTION`으로 복귀한다.
    - 이력의 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.
- `POST /api/v1/admin/orders/{id}/prepare-pickup`
  - 요청: `{ "pickupDeadlineAt": "2026-04-16T18:00:00" }`
  - 응답: `{ "orderId": 5, "status": "PICKUP_READY", "pickupDeadlineAt": "2026-04-16T18:00:00" }`
  - 정책:
    - 결제 시 고객이 `PICKUP`을 선택한 주문만 처리한다.
    - `pickupDeadlineAt`은 서버 `Clock` 기준 현재보다 이후여야 한다.
    - 이력은 `PICKUP_READY`로 기록하고 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.
- `POST /api/v1/admin/orders/{id}/complete-pickup`
  - 응답: `{ "orderId": 5, "status": "PICKED_UP", "pickupDeadlineAt": "2026-04-16T18:00:00" }`
  - 정책:
    - 이력은 `PICKUP_COMPLETE`로 기록하고 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.
- `POST /api/v1/admin/orders/{id}/refund-missed-pickup`
  - 응답: `200 OK`
    ```json
    {
      "orderId": 5,
      "orderStatus": "PICKUP_EXPIRED",
      "refund": {
        "refundId": 44,
        "amount": 118000,
        "status": "REQUESTED",
        "attemptCount": 0,
        "failReason": null
      }
    }
    ```
  - 정책:
    - `PICKUP_FORFEITED` 주문만 관리자가 예외적으로 전액 환불할 수 있다.
    - 기성품 재고는 만료 처리 때 이미 복구됐고 주문제작 재고는 판매 재고가 아니므로, 예외 환불은 재고를 다시 조정하지 않는다.
    - 환불 요청과 `PICKUP_EXPIRED` 전이, `PICKUP_EXPIRED` 이력을 한 트랜잭션에 기록한다. 이력의 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.
    - 응답의 `REQUESTED`는 환불 요청 접수 완료를 뜻하며 PG 환불 완료를 뜻하지 않는다.

공통 에러:
- `401 UNAUTHORIZED` — 관리자 인증 실패
- `404 NOT_FOUND` — orderId 또는 관련 fulfillment 미존재
- `400 INVALID_INPUT` — 허용되지 않은 상태 전이
- `409 ALREADY_REFUNDED` — 이미 환불된 주문에 대한 승인·거절 재시도

#### 2.7.4 픽업 만료 배치 수동 트리거

```http
POST /api/v1/admin/orders/expire-pickups
Authorization: Bearer {token}
```

```json
{
  "successCount": 2,
  "failureCount": 1,
  "failureReasons": {
    "NOT_FOUND": 1
  }
}
```

- 성공: `200 OK`
- 정책:
  - `pickup_deadline_at < now` 인 `PICKUP_READY` 주문만 처리한다.
  - 기성품 주문은 재고만 복구하고 환불 요청 없이 `PICKUP_FORFEITED`로 전이한다.
  - 주문제작 상품이 하나라도 포함된 주문은 제작 완료 상품으로 보아 환불 요청과 재고 복구 없이 `PICKUP_FORFEITED`로 전이한다.
  - 이력은 `PICKUP_FORFEITED`로 기록하며 자동 처리이므로 adminId는 `null`이다.
  - `successCount`는 미수령 종료 상태 전이에 성공한 건수다.
  - `failureReasons`는 내부 예외명을 그대로 노출하지 않고 `CONFLICT`, `NOT_FOUND`, `ALREADY_PROCESSED`, `BUSINESS_ERROR`, `INTERNAL_ERROR`로 정규화한다.

#### 2.7.5 제작 완료

```http
POST /api/v1/admin/orders/{id}/complete-production
Authorization: Bearer {token}
```

```json
{
  "orderId": 5,
  "status": "APPROVED_FULFILLMENT_PENDING",
  "expectedShipDate": "2026-04-15"
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — orderId 미존재
  - `400 INVALID_INPUT` — `IN_PRODUCTION` 또는 `DELAY_ACCEPTED` 상태가 아닌 주문
- 정책:
  - `IN_PRODUCTION` 또는 `DELAY_ACCEPTED` → `APPROVED_FULFILLMENT_PENDING`
  - 이력의 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.

#### 2.7.6 배송 흐름

```http
POST /api/v1/admin/orders/{id}/prepare-shipping
POST /api/v1/admin/orders/{id}/mark-delivered
Authorization: Bearer {token}
```

배송 출발은 택배사와 운송장 번호를 함께 받는다.

```http
POST /api/v1/admin/orders/{id}/mark-shipped
Authorization: Bearer {token}
Content-Type: application/json

{ "carrier": "CJ대한통운", "carrierCode": "CJ_LOGISTICS", "trackingNumber": "123456789012" }
```

```json
{ "orderId": 5, "status": "SHIPPING_PREPARING", "expectedShipDate": "2026-04-15" }
```

```json
{ "orderId": 5, "status": "SHIPPED", "expectedShipDate": "2026-04-15", "carrier": "CJ대한통운", "carrierCode": "CJ_LOGISTICS", "trackingNumber": "123456789012", "trackingRegistrationStatus": "PENDING", "trackingStatus": "PENDING", "trackingStatusText": "배송조회 등록 대기", "trackingUpdatedAt": null }
```

```json
{ "orderId": 5, "status": "DELIVERED", "expectedShipDate": "2026-04-15" }
```

- 정책:
  - `APPROVED_FULFILLMENT_PENDING` → `SHIPPING_PREPARING` → `SHIPPED` → `DELIVERED` 순서만 허용한다.
  - 결제 시 고객이 `SHIPPING`을 선택한 주문만 배송 흐름을 시작할 수 있다. 픽업 주문은 상태 변경 전에 거절한다.
  - `mark-shipped`의 `carrier`와 `trackingNumber`는 공백일 수 없고 각각 최대 50자, 100자다. `carrierCode`는 선택값으로 기존 클라이언트와 호환하며 새 관리자 화면은 지원 택배사 enum을 반드시 선택한다.
  - `carrierCode`가 있는 신규 출고는 배송조회 등록 대기 상태가 된다. 매분 25초 배치가 외부 서비스에 등록하고 일시 실패는 최대 10회 재시도한다. 연동 설정이 꺼져 있으면 출고 자체는 정상 처리하고 등록 대기를 유지한다.
  - 택배사 배송 완료 웹훅은 배송조회 상태와 이력만 갱신한다. 주문 `DELIVERED` 전이는 적립금과 후기 요청을 발생시키므로 관리자가 별도 API로 확정한다.
  - 각 전이는 `order_approvals` 이력에 `PREPARE_SHIPPING`, `SHIP`, `DELIVER`로 기록한다.
  - 이력의 adminId는 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`이다.

#### 2.7.6.1 택배 배송현황 웹훅

```http
POST /api/v1/webhooks/delivery-tracking
Content-Type: application/json
X-Webhook-Timestamp: {unix-seconds 또는 ISO-8601}
X-Webhook-Signature: {HMAC-SHA256 hex}
```

- 성공: `200 OK`
- 인증 실패: `401 UNAUTHORIZED`
- 정책:
  - 서명 입력은 `{timestamp}.{원문 JSON 바이트}`이며 `DELIVERY_WEBHOOK_SECRET`으로 HMAC-SHA256을 계산한다.
  - 서버 현재 시각과 5분 넘게 차이 나는 요청은 재전송 공격으로 보고 거절한다.
  - `clientId=order-{orderId}`이고 현재 fulfillment의 택배사 코드·운송장 번호와 일치하는 항목만 반영한다.
  - 웹훅 경로는 세션·CSRF 인증 대신 위 HMAC 서명을 신뢰 경계로 사용한다.
  - 최초 설정은 연동을 끈 상태에서 운영 서버에 `DELIVERY_WEBHOOK_SECRET`을 먼저 배포하고 같은 secret으로 웹훅 URL을 등록한다. 발급된 `endpointId`와 API 키를 설정한 뒤 `DELIVERY_TRACKING_ENABLED=true`로 전환한다.

#### 2.7.7 주문 처리 이력 조회

```http
GET /api/v1/admin/orders/{id}/history
Authorization: Bearer {token}
```

```json
[
  {
    "id": 1,
    "decision": "APPROVE",
    "decidedByAdminId": 1,
    "reason": null,
    "decidedAt": "2026-03-15T10:00:00"
  }
]
```

- 성공: `200 OK`
- 정책:
  - 처리 시간 순으로 정렬된 전체 이력을 반환한다.
  - `decision`: `APPROVE`, `REJECT`, `CUSTOMER_CANCEL`, `SHIP_DATE_UPDATED`, `DELAY`, `DELAY_ACCEPT`, `DELAY_REJECT`, `DELAY_CANCEL`, `AUTO_REFUND`, `PRODUCTION_COMPLETE`, `RESUME_PRODUCTION`, `PICKUP_READY`, `PICKUP_COMPLETE`, `PICKUP_EXPIRED`, `PICKUP_FORFEITED`, `PREPARE_SHIPPING`, `SHIP`, `DELIVER`

#### 2.7.8 주문 클레임 운영

```http
GET /api/v1/admin/order-claims?status=REQUESTED&cursor={cursor}&size=50
Authorization: Bearer {token}
```

```json
{
  "content": [
    {
      "id": 17,
      "orderId": 42,
      "type": "DEFECT",
      "requestedResolution": "REFUND",
      "status": "REQUESTED",
      "customerReason": "수령한 작품에 흠집이 있습니다.",
      "adminNote": null,
      "resolvedByAdminId": null,
      "completedByAdminId": null,
      "replacementCarrier": null,
      "replacementTrackingNumber": null,
      "maximumRefundAmount": 43000,
      "refundAmount": null,
      "refundStatus": null,
      "requestedAt": "2026-07-25T11:20:00",
      "resolvedAt": null,
      "completedAt": null,
      "items": [
        {
          "orderItemId": 51,
          "productId": 8,
          "productName": "빈티지 가죽 카드지갑",
          "quantity": 1,
          "unitPrice": 43000
        }
      ]
    }
  ],
  "nextCursor": "base64-cursor",
  "hasMore": true
}
```

```http
POST /api/v1/admin/order-claims/{claimId}/resolve
Authorization: Bearer {token}
Content-Type: application/json

{
  "approved": true,
  "refundAmount": 43000,
  "restoreInventory": true,
  "note": "반품 확인 후 환불"
}
```

```http
POST /api/v1/admin/order-claims/{claimId}/complete-exchange
Authorization: Bearer {token}
Content-Type: application/json

{
  "carrier": "CJ대한통운",
  "trackingNumber": "123456789012",
  "note": "교환품 발송 완료"
}
```

- 목록은 `(requestedAt, id)` 내림차순 커서를 사용한다. `size`는 1~100으로 보정하고 `hasMore=true`이면 `nextCursor`로 다음 페이지를 조회한다. 상태 필터를 바꾸면 커서를 초기화한다.
- 거절은 `approved=false`와 거절 메모를 보내며 즉시 `REJECTED`로 종결한다.
- 환불 승인은 1원 이상 최대 환불액 이하의 `refundAmount`를 요구하고 클레임별 비동기 환불을 요청한다. PG 성공 시 `COMPLETED`로 전이한다.
- `restoreInventory`는 승인·거절 요청 모두 명시하며, 반품품이 다시 판매 가능한 경우에만 `true`로 보낸다.
- 교환 승인은 반품 복구 여부와 무관하게 같은 품목·수량의 교환품 재고를 차감한다. 재고 부족이면 승인하지 않으며, 승인 뒤 관리자가 필수 택배사·운송장 번호를 남겨 교환 배송을 완료한다.

### 2.8 공지사항 API

#### 2.8.1 공개 공지 목록 조회

```http
GET /api/v1/notices
```

```json
[
  {
    "id": 3,
    "title": "4월 클래스 일정 공지",
    "pinned": true,
    "viewCount": 18,
    "version": 0,
    "createdAt": "2026-03-24T09:00:00"
  }
]
```

- 성공: `200 OK`
- 정책:
  - pinned 우선, 같은 pinned 그룹 안에서는 `createdAt DESC`로 정렬한다.
  - 홈 위젯은 이 목록에서 최근 5건만 노출한다.
  - `200 OK` 응답에는 `ETag` 헤더를 포함한다.
  - `If-None-Match`가 현재 ETag와 같으면 `304 Not Modified`를 반환한다.

#### 2.8.2 공개 공지 상세 조회

```http
GET /api/v1/notices/{id}
```

```json
{
  "id": 3,
  "title": "4월 클래스 일정 공지",
  "content": "4월 예약 오픈 일정입니다.",
  "pinned": true,
  "viewCount": 19,
  "version": 0,
  "createdAt": "2026-03-24T09:00:00"
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — noticeId 미존재
- 정책:
  - 상세 조회 시 DB 원자 갱신으로 `viewCount`를 1 증가시킨 뒤 최신 값을 반환한다.
    동시 조회의 증가분은 서로 유실되지 않고, 관리자 제목·본문 수정도 이미 반영된 조회수를 덮지 않는다.
  - 매 상세 조회를 서버에 전달하기 위해 `Cache-Control: no-store`를 반환하며 `ETag`와 `304 Not Modified`는 사용하지 않는다.

#### 2.8.3 관리자 공지 CRUD

- `GET /api/v1/admin/notices`
  - 응답: 공개 목록 조회와 동일한 배열
- `GET /api/v1/admin/notices/{id}`
  - 응답: 공지 상세 응답
  - 편집 폼을 열거나 동시 수정 충돌을 복구할 때 본문과 최신 `version`을 조회하며, 공개 상세 조회와 달리 조회수를 증가시키지 않는다.
- `POST /api/v1/admin/notices`
  - 요청: `{ "title": "점검 공지", "content": "3/28 점검 예정", "pinned": true }`
  - `title`은 공백이 아닌 200자 이하, `content`는 공백이 아닌 16,000자 이하
  - 응답: `201 Created` + 공지 상세 응답
- `PUT /api/v1/admin/notices/{id}`
  - 요청: `{ "expectedVersion": 0, "title": "수정 공지", "content": "본문 수정", "pinned": false }`
  - `title`과 `content` 길이 제한은 생성 요청과 동일
  - 응답: `200 OK` + 공지 상세 응답
- `DELETE /api/v1/admin/notices/{id}?expectedVersion={version}`
  - 관리자 목록에서 받은 현재 `version`을 필수 query parameter로 전달
  - 응답: `204 No Content`

공통 에러:
- `401 UNAUTHORIZED` — 관리자 인증 실패
- `404 NOT_FOUND` — noticeId 미존재
- `409 CONFLICT` — 조회 응답의 `version`과 수정·삭제 요청의 `expectedVersion`이 다르거나,
  비교 직후 다른 관리자 변경이 먼저 반영된 동시 처리 충돌
- 관리자 화면은 `409 CONFLICT`가 발생하면 상세를 다시 조회한다. 사용자의 편집 초안은 유지한 채
  최신 `version`으로 다시 시도하거나 서버의 최신 내용으로 교체할 수 있어야 한다.
- 관리자 공지 생성·수정·삭제가 성공하면 프론트는 관리자 목록뿐 아니라 `notices` 공개 목록·상세 query key도 함께 무효화해 같은 탭의 홈·공지 화면에 이전 내용을 남기지 않는다.

### 2.9 관리자 예약 목록 API

#### 2.9.1 관리자 예약 목록 조회

```http
GET /api/v1/admin/bookings?date=2026-03-20&status=BOOKED
Authorization: Bearer {token}
```

```json
[
  {
    "bookingId": 1,
    "bookingNumber": "BK-00000001",
    "customerSummary": {
      "type": "GUEST",
      "name": "홍길동",
      "phone": "010****5678"
    },
    "className": "향수 클래스",
    "startAt": "2026-03-20T10:00:00",
    "endAt": "2026-03-20T12:00:00",
    "status": "BOOKED",
    "source": "PHONE",
    "participantCount": 3,
    "depositAmount": 15000,
    "depositPaidAt": "2026-03-18T14:00:00",
    "balanceAmount": 135000,
    "balanceStatus": "UNPAID",
    "balancePaidAt": null,
    "arrears": false,
    "passBooking": false
  }
]
```

- 성공: `200 OK`
- 정책:
  - `customerSummary.type`은 `GUEST` 또는 `MEMBER`로 구분한다.
  - `source`는 `WEB`, `PHONE`, `NAVER_TALK`, `KAKAO`, `VISIT`이며 `participantCount`는 예약 인원이다.
  - 비회원 이력 가져오기(claim) 이후 `userId`가 설정된 예약은 `MEMBER`로 표시한다.
  - 탈퇴 회원의 종결 예약도 `MEMBER` 이력으로 유지하며 익명화된 이름과 `customerSummary.phone=null`을 반환한다.
  - 선택 귀속 요청의 주문 ID와 예약 ID는 각각 최대 100건이며 모든 ID는 양수여야 한다.
  - User 정보는 탈퇴 회원을 포함하는 관리자 이력 전용 batch fetch
    (`UserReaderPort.findAllByIdForAdminHistory`)로 조합한다.
  - `date` 필수, `status`는 선택(미입력 시 전체).

#### 2.9.1.1 관리자 수기 예약 등록

```http
POST /api/v1/admin/bookings
Authorization: Bearer {token}
Content-Type: application/json

{
  "slotId": 42,
  "name": "홍길동",
  "phone": "010-1234-5678",
  "participantCount": 3,
  "source": "PHONE",
  "depositPaid": true
}
```

- 성공: `201 Created` — 2.9.1과 같은 예약 응답
- 정책:
  - `source`는 운영자 접수 경로인 `PHONE`, `NAVER_TALK`, `KAKAO`, `VISIT`만 허용하며 `WEB`은 받지 않는다.
  - 휴대폰은 표준 숫자 형식으로 저장하고 같은 고객의 같은 슬롯 활성 예약을 중복 생성하지 않는다.
  - 인원은 1명 이상이며 남은 슬롯 정원을 초과할 수 없다.
  - 금액은 클라이언트가 보내지 않는다. 서버가 `클래스 1인 가격 × 인원`으로 계산한다.
  - `depositPaid=true`이면 10% 예약금과 입금 시각을 기록하고, 취소 시 같은 금액의 수동 반환 작업을 만든다.
  - `depositPaid=false`이면 예약금은 0원이고 전체 금액을 현장 잔금으로 남기며 취소 시 예약금 환불 작업을 만들지 않는다.

#### 2.9.2 관리자 예약 검색

```http
GET /api/v1/admin/bookings/search?status=BOOKED&dateFrom=2026-03-20&dateTo=2026-03-24&keyword=홍길동&page=0&size=20
Authorization: Bearer {token}
```

```json
{
  "content": [
    {
      "bookingId": 15,
      "bookingNumber": "BK-00000015",
      "bookerType": "MEMBER",
      "bookerName": "홍길동",
      "bookerPhone": "01012345678",
      "className": "향수 클래스",
      "startAt": "2026-03-24T10:00:00",
      "endAt": "2026-03-24T12:00:00",
      "status": "BOOKED",
      "source": "WEB",
      "participantCount": 1,
      "depositAmount": 5000,
      "depositPaidAt": "2026-03-20T09:15:00",
      "balanceAmount": 45000,
      "balanceStatus": "UNPAID",
      "balancePaidAt": null,
      "arrears": false,
      "passBooking": false,
      "createdAt": "2026-03-20T00:15:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalCount": 18,
  "totalPages": 1
}
```

- 성공: `200 OK`
- 정책:
  - `status`, `dateFrom`, `dateTo`, `keyword`는 모두 선택 필터다.
  - 관리자 화면의 검색 결과는 `bookingId`, `status`, `startAt`을 사용해 예약 탭의 기존 날짜별 운영 패널로 이동하고 대상 예약을 강조한다.
  - `page`는 0 미만이면 0으로, `size`는 1~100 범위로 보정하며 표현 가능한 OFFSET을 넘으면 `400 INVALID_INPUT`으로 거절한다.
  - `keyword`가 `BK-{숫자}` 형식의 예약번호이면 해당 예약 ID와 정확 일치로 검색한다. 국내 휴대폰 형식이면 정규화한 전화 HMAC, 그 외 문자열은 회원·비회원 이름 HMAC 정확 일치로 검색하며 개인정보 부분 검색과 예약 ID 부분 검색은 제공하지 않는다.
  - 날짜 필터는 슬롯 시작 시간(`slotStart`) 기준 KST 범위를 사용한다.
  - 결과는 `createdAt DESC` 기준 OFFSET 페이지로 반환한다.
  - DB 생성 시각인 `createdAt`은 UTC 오프셋(`Z`)을 포함한다. 슬롯·예약금·잔금 시각은 서울 현지시각이다.

#### 2.9.3 관리자 예약 잔금 결제

```http
POST /api/v1/admin/bookings/{bookingId}/balance-payment
Authorization: Bearer {token}
```

```json
{
  "bookingId": 1,
  "status": "COMPLETED",
  "balanceStatus": "PAID",
  "participantCount": 3,
  "balancePaidAt": "2026-03-20T12:10:00",
  "arrears": false
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — bookingId 미존재
  - `400 INVALID_INPUT` — 취소 또는 결석 예약
- 정책:
  - `BOOKED`와 `COMPLETED` 예약의 현장 잔금 결제를 기록한다.
  - 이미 결제된 예약에 다시 요청하면 최초 `balancePaidAt`을 유지한다.
  - 결제 완료 시 미수 표시는 자동 해제한다.

#### 2.9.4 관리자 예약 미수 설정

```http
PUT /api/v1/admin/bookings/{bookingId}/arrears
Authorization: Bearer {token}
Content-Type: application/json

{ "arrears": true }
```

- 성공: `200 OK` + 2.9.3과 같은 정산 응답
- 에러:
  - `400 INVALID_INPUT` — 결제 완료 잔금을 미수로 설정하거나 취소·결석 예약을 변경
  - `404 NOT_FOUND` — bookingId 미존재
- 정책:
  - `true`와 `false`를 모두 명시적으로 설정할 수 있다.
  - 미결제 예약을 완료하려면 먼저 `arrears=true`로 미수를 확인해야 한다.

#### 2.9.5 관리자 예약 완료

```http
POST /api/v1/admin/bookings/{bookingId}/complete
Authorization: Bearer {token}
```

- 성공: `200 OK` + 2.9.3과 같은 정산 응답 (`status=COMPLETED`)
- 에러:
  - `400 INVALID_INPUT` — BOOKED가 아니거나, 수업 종료 전이거나, 미결제 잔금을 미수로 표시하지 않음
  - `404 NOT_FOUND` — bookingId 미존재
  - `409 BOOKING_CONFLICT` — 동시에 같은 예약을 변경해 낙관적 락 충돌
- 정책:
  - `booking_history`에 `COMPLETED`를 기록한다.
  - 이미 진행된 수업을 닫는 상태 전이이므로 슬롯 정원과 버퍼 차단 수는 변경하지 않는다.

#### 2.9.6 관리자 예약 취소

```http
POST /api/v1/admin/bookings/{bookingId}/cancel
Authorization: Bearer {token}
Content-Type: application/json

{ "reason": "강사 사정으로 수업 취소" }
```

```json
{
  "bookingId": 15,
  "status": "CANCELED",
  "participantCount": 3,
  "passCreditRestored": false,
  "depositRefundAmount": 15000,
  "depositRefundStatus": "REQUESTED",
  "balanceSettlementRequired": true,
  "manualCompensationRequired": false
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 사유가 비었거나 200자를 초과함, 또는 `BOOKED` 상태가 아님
  - `401 UNAUTHORIZED` — 관리자 인증 실패
  - `404 NOT_FOUND` — bookingId 미존재
  - `409 BOOKING_CONFLICT` — 동시 변경 충돌
- 정책:
  - 고객 취소 마감과 무관하게 슬롯 정원과 버퍼를 반납하고 예약을 `CANCELED`로 전이한다.
  - PG로 결제한 일반 예약은 예약금 전액의 비동기 PG 환불 요청을 생성한다. `depositRefundStatus=REQUESTED`는 접수 완료이며 PG 환불 완료가 아니다.
  - 전화·메신저·방문 접수에서 받은 오프라인 예약금은 PG 환불을 호출하지 않고 실제 반환액이 있는 `MANUAL_COMPENSATION` 작업을 생성한다. 입금 전 수기 예약은 예약금이 0원이므로 환불 작업이 없다.
  - 8회권 예약은 유효한 이용권이면 크레딧을 복구한다. 만료되어 복구할 수 없으면 `passCreditRestored=false`, `manualCompensationRequired=true`로 운영자 수동 보상을 알린다.
  - 현장 잔금이 이미 결제된 일반 예약은 `balanceSettlementRequired=true`이며 서버가 예약금 외 잔금을 자동 환불하지 않는다.
  - `booking_history`에 `actor=ADMIN`, 입력 사유와 Bearer 세션이면 관리자 ID, 로컬 API key면 `null`인 행위자를 저장하고 취소 알림 outbox를 같은 트랜잭션에서 생성한다.

#### 2.9.7 관리자 수업 회차 취소

```http
POST /api/v1/admin/slots/{slotId}/cancel-session
Authorization: Bearer {token}
Content-Type: application/json

{ "reason": "강사 사정으로 해당 회차 폐강" }
```

```json
{
  "canceledBookings": 4,
  "passCreditsRestored": 2,
  "depositRefundsRequested": 2,
  "balanceSettlementsRequired": 1,
  "manualCompensationsRequired": 0
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 사유가 비었거나 200자를 초과함, 또는 슬롯이 관리자 비활성 상태가 아님
  - `401 UNAUTHORIZED` — 관리자 인증 실패
  - `404 NOT_FOUND` — slotId 미존재
  - `409 BOOKING_CONFLICT` — 잠금 전 대상 확인 뒤 예약·8회권 연결이 동시에 변경됨
- 정책:
  - 운영자는 먼저 슬롯을 비활성화해 신규 접수를 중단해야 한다. 버퍼 때문에 일시적으로 예약 불가능할 뿐 `adminActive=true`인 슬롯은 회차 취소 대상이 아니다.
  - 해당 슬롯의 현재 `BOOKED` 예약만 개별 관리자 취소와 같은 정책으로 한 트랜잭션에서 처리한다. 대상이 없으면 모든 집계가 0인 성공 응답을 반환한다.
  - `depositRefundsRequested`는 PG 환불 요청 생성 건수이지 완료 건수가 아니다. `balanceSettlementsRequired`와 `manualCompensationsRequired`는 각각 현장 잔금과 복구 불가 8회권의 후속 수동 처리 건수다.
  - 여러 8회권을 변경할 때 이용권 ID 오름차순으로 먼저 잠근 뒤 클래스와 슬롯을 잠그고, 취소 대상 예약은 마지막에 `FOR UPDATE`로 다시 조회한다.

#### 2.9.8 예약 취소 후속 작업

```http
GET /api/v1/admin/bookings/cancellation-tasks
Authorization: Bearer {token}
```

```http
POST /api/v1/admin/bookings/cancellation-tasks/{taskId}/complete
Authorization: Bearer {token}
```

GET 응답 항목:

```json
{
  "taskId": 501,
  "bookingId": 15,
  "bookingNumber": "BK-00000015",
  "type": "MANUAL_COMPENSATION",
  "status": "PENDING",
  "className": "가죽공예 원데이",
  "startAt": "2026-08-10T14:00:00",
  "balanceAmount": 0,
  "compensationAmount": 15000,
  "reason": "공방 일정 변경",
  "createdAt": "2026-08-01T10:00:00",
  "completedByAdminId": null,
  "completedAt": null
}
```

- 운영자 취소 때 이미 수납한 현장 잔금은 `BALANCE_SETTLEMENT`, 만료로 복구할 수 없는 8회권은 `MANUAL_COMPENSATION` 작업으로 생성한다.
- 오프라인에서 받은 예약금도 `MANUAL_COMPENSATION`으로 생성한다. 작업 응답의 `compensationAmount`는 반환할 예약금이며, 만료 8회권 보상은 금액으로 확정할 수 없어 0원이다. `balanceAmount`는 잔금 정산 작업의 금액이다.
- GET은 오래된 순으로 미완료 작업을 최대 100건 반환한다.
- 완료는 작업 행을 잠근 뒤 처리 관리자와 완료 시각을 저장한다. 이미 완료된 작업을 다시 요청하면 `changed=false`와 기존 결과를 반환한다.

### 2.10 관리자 대시보드 API

관리자 인증은 `Authorization: Bearer {token}` 기준이며, 모든 응답은 `200 OK`를 반환한다.

#### 2.10.1 개요/매출 집계

- `GET /api/v1/admin/dashboard/overview?from=2026-03-01&to=2026-03-31`
  - 응답:

```json
{
  "todayRevenue": 118000,
  "todayOrderCount": 3,
  "pendingApprovalCount": 2,
  "todayBookingCount": 4,
  "monthRevenue": 2140000,
  "monthOrderCount": 41
}
```

- `GET /api/v1/admin/dashboard/sales-summary?from=2026-03-01&to=2026-03-31&granularity=DAILY`
  - 응답: `[{"periodLabel":"2026-03-24","totalRevenue":118000,"orderCount":3,"avgOrderValue":39333}]`
- `GET /api/v1/admin/dashboard/revenue-breakdown?from=2026-03-01&to=2026-03-31`
  - 응답: `{"orderRevenue":1500000,"bookingDepositRevenue":120000,"bookingBalanceRevenue":320000,"passPurchaseRevenue":200000,"totalRevenue":2140000}`
- `GET /api/v1/admin/dashboard/refunds?from=2026-03-01&to=2026-03-31`
  - 응답: `{"totalRefundCount":4,"totalRefundedAmount":180000,"refundRate":0.08}`

정책:
- `from`, `to`는 KST 기준 집계 기간이다.
- 호환 필드명인 `overview.monthRevenue`, `overview.monthOrderCount`는 달력 월 고정값이 아니라 요청한 `from`~`to` 선택 기간의 값이다.
- `sales-summary.granularity` 기본값은 `DAILY`이며 `DAILY`, `WEEKLY`, `MONTHLY`를 지원한다.
- `overview`, `revenue-breakdown`, `daily-revenue`는 주문·예약금·예약 잔금·8회권 결제를 결제 시점에 더하고, 도메인 환불이 `SUCCEEDED`가 된 시점에 차감한 순매출이다. 미완료·실패 환불과 `payment_attempt_id`만 가진 보상환불은 차감하지 않는다.
- `sales-summary`와 `top-products`는 상품 주문 분석 전용이다. 주문 결제를 더하고 성공한 주문 환불을 성공 시점에 차감하며, 예약·8회권 매출은 포함하지 않는다.
- `refunds.refundRate`는 조회 기간의 성공 환불액을 같은 기간의 네 결제 원천 총 유입액으로 나눈 운영 현금흐름 비율이다. 이전 기간 결제가 현재 기간에 환불되면 1을 넘을 수 있다.

#### 2.10.2 상태/상품/예약 분포

- `GET /api/v1/admin/dashboard/order-status`
  - 응답: `[{"status":"PAID_APPROVAL_PENDING","count":2},{"status":"IN_PRODUCTION","count":5}]`
- `GET /api/v1/admin/dashboard/top-products?from=2026-03-01&to=2026-03-31&limit=10&sort=REVENUE`
  - 응답: `[{"productId":1,"productName":"시그니처 캔들","productType":"READY_STOCK","totalRevenue":530000,"totalQuantity":14}]`
- `GET /api/v1/admin/dashboard/daily-revenue?from=2026-03-01&to=2026-03-31`
  - 응답: `[{"date":"2026-03-24","revenue":118000}]`
- `GET /api/v1/admin/dashboard/slot-utilization?from=2026-03-01&to=2026-03-31`
  - 응답: `[{"date":"2026-03-24","className":"향수 클래스","totalCapacity":24,"totalBooked":18,"utilizationRate":0.75}]`

정책:
- `top-products.limit` 기본값은 `10`이다.
- `top-products.sort` 기본값은 `REVENUE`이며 `REVENUE`, `QUANTITY`를 지원한다.
- `order-status`는 전체 운영 상태 분포를 반환하고, 나머지 API는 `from`, `to`를 필수로 받는다.

### 2.11 운영 조치 관리 Admin API

#### 2.11.1 조치 필요 환불 목록 조회

```http
GET /api/v1/admin/refunds/failed?cursor={cursor}&size=20
Authorization: Bearer {token}
```

```json
{
  "content": [
    {
      "refundId": 42,
      "bookingId": 15,
      "orderId": null,
      "orderClaimId": null,
      "passPurchaseId": null,
      "paymentAttemptId": null,
      "amount": 5000,
      "pgRefundAmount": 4000,
      "rewardRestoreAmount": 1000,
      "rewardRevokeAmount": 50,
      "restoreCoupon": false,
      "status": "RECONCILIATION_REQUIRED",
      "attemptCount": 1,
      "failReason": "PG 응답 지연으로 환불 상태 확인이 필요합니다.",
      "createdAt": "2026-03-01T14:30:00"
    }
  ],
  "nextCursor": "MjAyNi0wMy0wMVQxNDozMDowMHw0Mg",
  "hasMore": true
}
```

- 성공: `200 OK`
- `FAILED`, `RETRYABLE`, `RECONCILIATION_REQUIRED` 상태를 `(createdAt, id)` 최신순 커서 페이지로 반환한다.
- 클레임 환불은 `orderId`와 `orderClaimId`를 함께 반환해 한 주문의 여러 클레임을 구분한다.
- `size` 기본값은 20이고 범위는 1~100이다. `hasMore=true`이면 `nextCursor`로 다음 페이지를 조회한다.

#### 2.11.2 환불 상태 조회

```http
GET /api/v1/admin/refunds/{refundId}
Authorization: Bearer {token}
```

```json
{
  "refundId": 42,
  "amount": 5000,
  "pgRefundAmount": 4000,
  "rewardRestoreAmount": 1000,
  "rewardRevokeAmount": 50,
  "restoreCoupon": false,
  "status": "SUCCEEDED",
  "attemptCount": 1,
  "failReason": null
}
```

- 성공: `200 OK`
- 에러: `404 NOT_FOUND` — refundId 미존재
- 주문 거절·지연 거절 취소·8회권 환불 시작 응답의 `refundId`로 실제 처리 상태를 조회한다. `amount`는 고객 반환 총액이고, `pgRefundAmount`와 `rewardRestoreAmount`의 합과 일치한다. `rewardRevokeAmount`는 환불 상품에서 이미 지급된 적립금 회수액이다.

#### 2.11.3 환불 재시도

```http
POST /api/v1/admin/refunds/{refundId}/retry
Authorization: Bearer {token}
```

```json
{
  "refundId": 42,
  "amount": 5000,
  "pgRefundAmount": 4000,
  "rewardRestoreAmount": 1000,
  "rewardRevokeAmount": 50,
  "restoreCoupon": false,
  "status": "SUCCEEDED",
  "attemptCount": 2,
  "failReason": null
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — refundId 미존재
  - `400 INVALID_INPUT` — 조치 필요 상태가 아닌 환불 재시도
- 정책:
  - `FAILED`, `RETRYABLE`, `RECONCILIATION_REQUIRED` 상태를 재처리할 수 있다.
  - 성공 시 `SUCCEEDED`, 명시적 거절 시 `FAILED`, 실행 전 일시 실패 시 `RETRYABLE`, 결과 불명 시 `RECONCILIATION_REQUIRED`가 된다.
  - `RECONCILIATION_REQUIRED`는 Toss cancel을 바로 다시 호출하지 않는다. `paymentKey`로 결제와 취소 내역을 조회해 취소 사유에 포함한 최초 멱등키, 요청 금액, `DONE` 상태, `transactionKey`, 결제 상태 `CANCELED|PARTIAL_CANCELED`가 모두 일치하면 `SUCCEEDED`로 화해한다.
  - 해당 멱등키의 취소 내역이 없고 결제 상태가 미취소로 명확하면 `RETRYABLE`로 바꾼다. 다음 실행부터 최초 멱등키로 cancel을 호출한다. 식별자·금액·상태 모순이나 조회 실패는 `RECONCILIATION_REQUIRED`를 유지해 중복 취소를 피한다.
  - PG 조회 응답의 `paymentKey`, 취소 사유 멱등키, 취소 금액, `transactionKey`가 저장 요청과 일치하는지 결과 저장 전에 다시 확인한다.
  - PG 호출 전 선점과 호출 후 결과 저장은 부모 주문/예약 트랜잭션 및 PG 네트워크 구간과 분리된 짧은 `REQUIRES_NEW` 트랜잭션으로 처리한다.
  - `paymentAttemptId`가 있으면 PG 승인 후 주문·예약·8회권 생성에 실패한 결제의 보상 환불이다.
  - 최초 환불과 자동·수동 재처리는 같은 `refunds.idempotency_key`를 Toss `Idempotency-Key` 헤더로 사용한다.

#### 2.11.4 실패 알림 목록과 재처리

- `GET /api/v1/admin/notifications/failed`
  - 자동 재시도를 모두 소진한 `FAILED` outbox를 오래된 순서로 최대 100건 반환한다.
  - 현재 도메인 상태와 맞지 않아 발송 없이 종결된 `OBSOLETE` 리마인드는 실패 목록에 포함하지 않는다.
  - `createdAt`은 UTC 오프셋을 포함한 ISO-8601 시각으로 반환한다.
- `POST /api/v1/admin/notifications/{outboxId}/retry`
  - 성공: `200 OK`, 기존 outbox를 `PENDING`으로 다시 연 결과를 반환한다.
  - 에러: `404 NOT_FOUND`, `400 INVALID_INPUT`(최종 실패가 아닌 outbox)

재처리는 새 알림 요청을 만들지 않고 기존 outbox와 멱등키를 그대로 사용한다. 다음 scheduler 주기 또는 dispatcher가 발송을 다시 시도한다.
`OBSOLETE`는 재처리할 수 없고 기존과 동일하게 `400 INVALID_INPUT`으로 거절한다.
다만 일정 재변경·마감 연장으로 같은 시간 의존 리마인드가 미래 유효 구간에 다시 들어온 경우에는 정기 배치가
같은 outbox와 멱등키를 자동으로 `PENDING`으로 재활성화하며, 이는 관리자 API 동작이 아니다.

#### 2.11.5 결제 대사 목록과 PG 조회

```http
GET /api/v1/admin/payment-attempts/reconciliation-required
Authorization: Bearer {token}
```

- 성공: `200 OK`
- 응답은 `attemptId`, `context`, `amount`, `status`, `reason`, `createdAt`을 포함하며 paymentKey와 저장 payload는 노출하지 않는다. `createdAt`은 UTC 오프셋을 포함한 ISO-8601 시각이다.

```http
POST /api/v1/admin/payment-attempts/{attemptId}/reconcile
Authorization: Bearer {token}
```

- 성공: `200 OK`, `attemptId`, 최종 또는 유지 상태, 생성된 `domainId`, 처리 메시지를 반환한다.
- 에러: `404 NOT_FOUND`, `400 INVALID_INPUT`(대사 대상이 아님), `409 CONFLICT`(동시 상태 변경), PG 조회 결과와 저장 요청의 식별자·금액 불일치
- 정책:
  - 저장된 `orderId`로 PG 상태를 조회하며 조회 네트워크 구간에는 DB 트랜잭션을 열지 않는다.
  - `DONE`이면 저장된 paymentKey·orderId·amount를 모두 확인한 뒤 기존 주문·예약·8회권 처리를 재개한다.
  - Toss가 `NOT_FOUND_PAYMENT`로 승인 없음(결제 미존재)을 명시한 경우에만 `FAILED`로 확정한다. 다른 404를 포함해 결과를 자동 판정할 수 없으면 `RECONCILIATION_REQUIRED`를 유지한다.

#### 2.11.6 PG 정산 불일치 목록

```http
GET /api/v1/admin/payment-settlements/issues
Authorization: Bearer {token}
```

- 성공: `200 OK`
- 응답은 최근 Toss 정산 내역 중 로컬 승인·환불과 일치하지 않은 건을 `id`, `transactionKey`, `paymentKey`, `orderId`, `amount`, `payOutAmount`, `soldDate`, `cancelTransaction`, `status`, `reason`, `fetchedAt`으로 반환한다.
- `status`는 `LOCAL_PAYMENT_NOT_FOUND`, `LOCAL_REFUND_NOT_FOUND`, `IDENTIFIER_MISMATCH`, `AMOUNT_MISMATCH` 중 하나다. 일치한 `MATCHED` 건은 운영자 작업 목록에 노출하지 않는다.
- 서버는 매시 40분에 Toss 정산 API에서 최근 7일 내역을 다시 읽어 `transactionKey` 기준으로 갱신한다. 정산 조회는 승인·취소용 HTTP 연결 풀과 분리해 긴 응답이 고객 결제 요청을 점유하지 않게 한다.

#### 2.11.7 스마트스토어 정산 불일치 목록

```http
GET /api/v1/admin/smartstore-settlements/issues
Authorization: Bearer {token}
```

- 성공: `200 OK` — `ORDER_NOT_FOUND`, `EXPECTED_AMOUNT_MISSING`, `AMOUNT_MISMATCH`인 최근 정산 원장을 최대 100건 반환한다.
- 응답은 상품 주문·주문 번호, 정산 유형, 결제 정산액·수수료·혜택 정산액·정산 예정액, 기준일·예정일·완료일·지급일, 사유와 조회 시각을 포함한다.
- 서버는 매시 50분 저장된 다음 지급일부터 오늘까지 한 실행당 최대 31일을 순차 처리하고, 오늘까지 완료한 뒤에는 당일을 다시 조회한다. 처리 날짜는 성공한 뒤에만 전진하므로 장기 장애 뒤에도 누락 기간을 자동 복구한다. `productOrderId + 정산 유형 + 정산일` 조합으로 멱등 갱신하며 부가 정산 유형은 `NOT_APPLICABLE`, 일치한 원거래는 `MATCHED`로 저장하되 작업 목록에는 표시하지 않는다.
- 관리자는 `POST /api/v1/admin/smartstore-settlements/synchronize`에 `{ "from": "2026-08-01", "to": "2026-08-07" }`을 보내 31일 이내 기간을 다시 조회할 수 있다. 응답은 정상 건수, 확인 필요 건수와 사유별 건수를 반환한다.
- `GET /api/v1/admin/smartstore-settlements/accounting?from=2026-07-01&to=2026-07-31`은 31일 이내 일별 정산·수수료 상세·일별 부가세 자료를 반환한다. 부가세는 전월 말까지만 조회하며 응답의 `vatAvailableThrough`로 제공 가능 종료일을 알린다. 구매자명·예금주·계좌번호는 반환하지 않고 CSV는 관리자 화면에서 내려받는다.

### 2.12 회원 API (`/api/v1/me`)

회원 인증은 `HG_SESSION` HttpOnly 쿠키 기반이며, Spring Security의 회원 principal로 검증한다.

#### 2.12.0.0 현재 정책 버전 조회

```http
GET /api/v1/policies/current
```

```json
{
  "terms": { "version": "2026-08-08-v1", "documentPath": "/terms/2026-08-08-v1" },
  "privacy": { "version": "2026-08-11-v2", "documentPath": "/privacy/2026-08-11-v2" }
}
```

- 인증 없이 현재 이용약관·개인정보처리방침 버전과 버전별 불변 문서 화면 경로를 반환한다.
- 이메일·소셜 최초 가입과 비회원 주문·예약 prepare는 이 버전과 명시적 동의를 제출한다.
- 서버는 현재 버전을 다시 검증하고 클라이언트가 보낸 시각이 아니라 서버 수락 시각을 이력으로 저장한다.

#### 2.12.0 회원 인증 정책

- 회원 세션은 `HG_SESSION` HttpOnly 쿠키로 유지한다.
- 로그인·회원가입·소셜 로그인 성공 시 기존 세션 ID를 회전하고 새 ID로 회원 세션을 유지한다.
- 로그인·회원가입·소셜 로그인 성공과 명시적 재인증 성공은 현재 회원 ID·자격 버전에 결합된 최근 본인 확인을 해당 세션에 10분간 기록한다. 다른 세션이나 다른 자격 버전에는 재사용할 수 없다.
- 상태를 변경하는 요청은 1.3의 SPA CSRF 계약에 따라 `X-XSRF-TOKEN` 헤더를 함께 보낸다.
- 회원 로그인은 이메일/비밀번호(local)와 Google, Naver, Kakao OAuth2를 함께 지원한다.
- 소셜 계정은 `user_social_accounts`에 `(provider, provider_id_hmac)`로 저장한다. 한 회원은 Google, Naver, Kakao 계정을 각각 하나씩 연결할 수 있다.
- Google은 `email_verified=true`인 이메일만 기준 이메일 후보로 수용한다. 처음 보는 Google provider ID의 검증 이메일이 기존 회원과 겹치면 자동 연결하지 않고 `SOCIAL_ACCOUNT_LINK_REQUIRED`를 반환한다.
- Naver 프로필 이메일은 검증된 기준 이메일로 간주하지 않아 충돌 조회와 신규 회원 저장에 사용하지 않는다. 신규 Naver 회원은 provider ID와 이름으로 생성하며 기준 이메일은 `null`이다. 이메일이 없는 로그인 회원은 2.12.0.5.2의 별도 메일함 소유 확인을 마친 뒤 기준 이메일을 한 번 등록할 수 있다.
- Kakao는 `is_email_valid=true`, `is_email_verified=true`인 카카오계정 이메일과 닉네임을 모두 요구한다. 처음 보는 Kakao provider ID의 검증 이메일이 기존 회원과 겹치면 자동 연결하지 않고 `SOCIAL_ACCOUNT_LINK_REQUIRED`를 반환한다.
- 소셜 로그인으로 새로 생성된 회원은 `password_hash`가 비어 있을 수 있다.
- 이메일 로그인은 존재하지 않는 계정과 로컬 비밀번호가 없는 소셜 전용 계정에도 고정 dummy BCrypt 해시를 한 번 비교하고 모두 `401 INVALID_CREDENTIALS`로 응답한다. 정규화 이메일별 시도는 10회/10분으로 제한하며 Redis 장애 시 fail-closed한다.
- 회원·관리자 비밀번호 필드는 UTF-8 72바이트 이하로 제한한다. 문자 수가 72 이하여도 UTF-8 바이트 수가 이를 넘으면 `400 INVALID_INPUT`이다.
- `CustomerUserResponse.email`은 nullable이다. `null`은 검증된 기준 이메일이 없다는 뜻이며, `localPasswordEnabled`는 이메일 로그인 비밀번호가 설정되어 있는지를 나타낸다.
- 기준 이메일이 있으면 앞뒤 공백을 제거한 소문자, 전화번호는 공백·하이픈을 제거한 숫자 형식으로 통일한다. 응답에는 저장된 값을 복호화해 반환한다.

#### 2.12.0.1 이메일 회원가입

```http
POST /api/v1/auth/signup

{
  "email": "member@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "01012345678",
  "verificationCode": "483921",
  "policyAcceptance": {
    "termsVersion": "2026-08-08-v1",
    "termsAccepted": true,
    "privacyVersion": "2026-08-11-v2",
    "privacyAccepted": true
  }
}
```

```json
{
  "id": 7,
  "email": "member@example.com",
  "name": "홍길동",
  "phone": "01012345678",
  "phoneVerified": true,
  "localPasswordEnabled": true
}
```

- 성공: `201 Created`, `HG_SESSION` HttpOnly 쿠키 발급
- 에러:
  - `400 INVALID_INPUT` — 요청 형식 불일치
  - `400 PHONE_VERIFICATION_FAILED` — 같은 전화번호의 미소모·유효 인증 코드가 아님
  - `409 EMAIL_ALREADY_EXISTS` — 이미 가입한 이메일
  - `422 POLICY_CONSENT_REQUIRED` — 현재 정책 버전과 동의가 없거나 일치하지 않음
  - `429 TOO_MANY_REQUESTS` — 회원가입 처리율 제한 초과
- 정책:
  - 인증 코드는 2.4.1에서 `purpose=SIGNUP`으로 발급한다.
  - 비밀번호는 8~72자이면서 UTF-8 72바이트 이하다.
  - 정책 동의를 확인한 뒤 휴대폰 인증 코드부터 검증하고 이메일·전화번호 중복을 조회한다. 따라서 유효한 인증 코드가 없는 요청은 이메일 존재 여부와 관계없이 같은 `PHONE_VERIFICATION_FAILED`를 반환한다.
  - 회원 저장과 인증 코드 1회 소모를 같은 트랜잭션에서 처리하며 성공한 회원은 `phoneVerified=true`다.
  - 같은 전화번호의 회원가입 인증 코드 시도는 5회/10분으로 제한한다.
  - 로그인 성공과 동일하게 세션 ID를 회전한다.

#### 2.12.0.2 소셜 로그인 시작

일반 로그인:

```http
GET /api/v1/auth/social/authorization/{provider}
```

- `{provider}`: `google`, `naver`, `kakao`
- 성공: `302 Found`, `Location`은 해당 제공자의 authorization endpoint
- 에러:
  - `429 TOO_MANY_REQUESTS` — 로그인 시작 IP 버킷 분당 10회 초과
- 정책:
  - 브라우저는 JSON URL 발급 API를 먼저 호출하지 않고 이 경로로 직접 이동한다.
  - Spring Security OAuth2 Client가 `state`를 포함한 authorization request를 만들고 callback 전까지만 현재 Redis HTTP 세션에 저장한다.
  - 일반 로그인 GET은 정책 동의를 받지 않는다. 이미 연결된 회원 로그인은 가입 의도 없이 처리하고,
    처음 보는 계정이면 callback에서 `POLICY_CONSENT_REQUIRED`로 종료한다.
  - callback URI는 provider별 `GOOGLE_OAUTH_REDIRECT_URI`, `NAVER_OAUTH_REDIRECT_URI`, `KAKAO_OAUTH_REDIRECT_URI` 설정에 고정하며 브라우저 요청값으로 받지 않는다.
  - Google은 `openid`, `profile`, `email` 범위의 OIDC 로그인을 사용한다. 로그인만을 위해 refresh token을 요청하거나 저장하지 않는다.
  - Kakao는 `profile_nickname`, `account_email` 동의 항목의 REST OAuth2 로그인을 사용한다. access token은 UserInfo 조회 뒤 저장하지 않는다.

신규 가입 시작:

```http
POST /api/v1/auth/social/signup-intents/{provider}
Cookie: HG_SESSION={anonymousSession}
X-XSRF-TOKEN: {csrfToken}

{
  "termsVersion": "2026-08-08-v1",
  "termsAccepted": true,
  "privacyVersion": "2026-08-11-v2",
  "privacyAccepted": true
}
```

```json
{
  "authorizationUrl": "/api/v1/auth/social/authorization/google?signupAttempt={oneTimeAttemptId}"
}
```

- `operationId`: `startSocialSignup`
- 성공: `200 OK`
- 에러:
  - `403 FORBIDDEN` — CSRF 토큰 누락 또는 불일치
  - `422 POLICY_CONSENT_REQUIRED` — 현재 정책 버전·명시적 동의가 아님
  - `429 TOO_MANY_REQUESTS` — 로그인 시작 IP 버킷 초과
- 정책:
  - 서버는 현재 정책을 검증한 뒤 provider·동의·불투명 시도 ID·5분 만료를 현재 HTTP 세션에 저장한다.
  - 브라우저는 응답 URL로 이동하고 서버는 `signupAttempt`가 같은 세션의 미사용 의도와 일치할 때만
    새 OAuth authorization request의 `state`를 한 번 결합한다.
  - callback은 provider·state·만료를 다시 확인한 뒤 동의를 한 번 소비한다. 다른 provider, 다른 세션,
    재사용·만료 시도와 공개 GET에 임의로 붙인 정책 값은 신규 가입 동의가 될 수 없다.

#### 2.12.0.3 소셜 로그인 callback

```http
GET /api/v1/auth/social/callback/{provider}?code=...&state=...
```

- 이 경로는 Google/Naver/Kakao가 호출하는 backend callback이며 프런트가 직접 호출하지 않는다.
- 성공: `302 Found` → `/auth/callback?newUser=true|false`
- 실패: `302 Found` → `/auth/callback?error=SOCIAL_LOGIN_FAILED`
- 신규 회원 동의 누락·버전 불일치: `302 Found` → `/auth/callback?error=POLICY_CONSENT_REQUIRED`
- Google/Kakao 검증 이메일이 기존 기준 이메일과 충돌: `302 Found` → `/auth/callback?error=SOCIAL_ACCOUNT_LINK_REQUIRED`
- 명시적 계정 연결 성공: `302 Found` → `/auth/callback?linked=GOOGLE|NAVER|KAKAO`
- 소셜 재인증 성공: `302 Found` → `/auth/callback?reauthenticated=GOOGLE|NAVER|KAKAO`
- 처리율 제한 초과: `429 TOO_MANY_REQUESTS`
- 정책:
  - Spring Security가 callback의 `state`와 세션의 authorization request를 비교하고 한 번 사용한 authorization request를 제거한 뒤 code를 토큰으로 교환한다.
  - 신규 가입 callback은 OAuth `state`에 결합된 미사용 가입 의도의 provider·만료를 추가로 검증하고,
    검증한 정책 동의만 회원 생성 트랜잭션에 전달한다.
  - 연결 callback은 Google ID Token 또는 Naver/Kakao UserInfo에서 provider와 provider ID를 먼저 확인한 뒤 application의 계정 연결 트랜잭션을 시작한다. 재인증 callback은 같은 provider ID가 현재 회원에게 이미 연결되어 있는지도 확인한다. 일반 로그인·신규 가입은 Google의 검증 이메일과 이름, Naver의 이름, Kakao의 유효·검증 이메일과 닉네임을 요구하며 Naver 프로필 이메일은 버린다.
  - 성공 시 세션 ID를 한 번 회전하고 `customerUserId`, `customerCredentialVersion`, `userId:credentialVersion` 형식의 principal 인덱스를 장기 인증 상태로 저장한다.
  - OAuth `SecurityContext`, access token, refresh token은 세션에 저장하지 않는다. 다음 요청은 기존 `CustomerAuthenticationFilter`가 `customerUserId`로 회원 principal을 다시 구성한다.
  - 성공 후 기존 CSRF 토큰이 폐기되므로 클라이언트는 새 CSRF 토큰을 발급받는다.
  - `newUser=true`이면 프런트는 마이페이지의 최초 전화번호 등록 온보딩을 연다. callback 상태를 잃고 마이페이지에 직접 진입해도 현재 회원의 `phone=null`이면 같은 온보딩을 연다.
  - 소셜 로그인 시작과 callback은 서로 분리된 IP 버킷으로 각각 분당 10회 제한한다.

#### 2.12.0.4 소셜 계정 연결 관리

```http
GET /api/v1/me/social-accounts
Cookie: HG_SESSION={sessionToken}
```

```json
{
  "linkedProviders": ["GOOGLE", "NAVER", "KAKAO"]
}
```

```http
POST /api/v1/me/social-accounts/{provider}/authorization
Cookie: HG_SESSION={sessionToken}
X-XSRF-TOKEN: {csrfToken}
```

```json
{
  "authorizationUrl": "/api/v1/auth/social/authorization/google?linkAttempt={oneTimeAttemptId}"
}
```

```http
POST /api/v1/me/social-accounts/{provider}/reauthentication
Cookie: HG_SESSION={sessionToken}
X-XSRF-TOKEN: {csrfToken}
```

```json
{
  "authorizationUrl": "/api/v1/auth/social/authorization/google?linkAttempt={oneTimeAttemptId}"
}
```

```http
DELETE /api/v1/me/social-accounts/{provider}
Cookie: HG_SESSION={sessionToken}
X-XSRF-TOKEN: {csrfToken}
```

- `{provider}`: `google` 또는 `naver`
- 조회 성공: `200 OK`
- 소셜 재인증 시작 성공: `200 OK`, 현재 회원에게 이미 연결된 provider만 허용
- 연결 시작 성공: `200 OK`, 브라우저가 응답의 일회성 `linkAttempt`가 포함된 same-origin `authorizationUrl`로 이동
- 연결 해제 성공: `204 No Content`, 현재 세션을 포함한 기존 회원 세션 폐기
- 에러:
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `403 FORBIDDEN` — 재인증하려는 provider가 현재 회원에게 연결되어 있지 않음
  - `403 REAUTHENTICATION_REQUIRED` — 최근 본인 확인이 없거나 만료됨
  - `409 LAST_LOGIN_METHOD_REQUIRED` — 해제하면 로컬 비밀번호와 소셜 계정이 모두 사라짐
- 정책:
  - 비밀번호 재인증은 `POST /api/v1/me/reauthentication/password`, 소셜 재인증은 위 provider별 재인증 시작을 사용한다. 두 방식 모두 성공 시 현재 세션·회원 ID·자격 버전에 결합된 증명을 10분간 유지한다.
  - 연결 시작과 해제는 최근 본인 확인과 SPA CSRF 검증을 모두 통과해야 한다. 연결 시작은 5분짜리 연결 의도와 일회성 `linkAttempt`를 만들며 연결 의도에는 회원 ID, 자격 버전, provider를 저장한다.
  - `linkAttempt`가 일치하는 authorization request가 처음 생성될 때 Spring Security가 만든 OAuth `state`를 연결 의도에 결합한다. 일반 소셜 로그인 시작이나 다른 연결 시도는 이 의도를 이어받지 않는다.
  - 이어지는 OAuth callback은 결합된 `state`, 연결 의도의 provider·만료·자격 버전과 현재 HTTP 세션의 회원 ID·자격 버전을 모두 확인한다.
  - provider ID만 외부 계정 식별과 연결에 사용한다. provider 이메일·이름은 연결 대상 회원을 찾거나 기존 계정 소유권을 증명하는 데 사용하지 않는다.
  - 같은 외부 계정의 재연결은 멱등 처리하지만, 다른 회원의 외부 계정이나 같은 provider의 다른 계정을 자동 이전·교체하지 않는다.
  - 해제 뒤에도 로컬 비밀번호 또는 다른 소셜 계정이 하나 이상 남아야 한다. 새 외부 계정 연결 또는 실제 연결 해제로 로그인 수단 집합이 바뀌면 `credential_version`을 증가시키고 모든 기존 회원 세션을 폐기해 변경 뒤 남은 로그인 수단으로 다시 로그인하게 한다. 이미 같은 외부 계정이 연결된 멱등 요청은 버전과 세션을 바꾸지 않는다.
  - 연결 callback에서 외부 계정이 다른 회원에게 이미 연결되어 있으면 `/auth/callback?error=SOCIAL_ACCOUNT_ALREADY_LINKED`, 현재 회원에게 같은 provider의 다른 계정이 있으면 `/auth/callback?error=SOCIAL_PROVIDER_ALREADY_LINKED`로 이동한다. 연결 의도가 만료됐거나 현재 자격 버전과 다르면 `SOCIAL_LOGIN_FAILED`로 종료한다.

#### 2.12.0.5 회원 휴대폰 등록·변경

```http
PATCH /api/v1/me/phone
Cookie: HG_SESSION={sessionToken}

{
  "phone": "01012345678",
  "verificationCode": "483921"
}
```

```json
{
  "id": 7,
  "email": null,
  "name": "홍길동",
  "phone": "01012345678",
  "phoneVerified": true,
  "localPasswordEnabled": false
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 요청 형식 또는 전화번호 형식 불일치
  - `400 PHONE_VERIFICATION_FAILED` — 같은 전화번호의 미소모·유효 인증 코드가 아님
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `403 REAUTHENTICATION_REQUIRED` — 최근 본인 확인이 없거나 현재 자격 버전과 다름
  - `409 PHONE_ALREADY_IN_USE` — 다른 회원이 이미 사용하는 전화번호
  - `409 DUPLICATE_BOOKING` — 변경할 번호의 비회원 예약과 회원의 활성 예약이 같은 슬롯에서 충돌
  - `429 TOO_MANY_REQUESTS` — 같은 전화번호의 인증 코드 확인 시도 초과
- 정책:
  - 비밀번호 또는 현재 연결된 소셜 계정으로 최근 본인 확인을 먼저 완료한다.
  - 최초 등록은 2.4.1에서 `purpose=MEMBER_PHONE_REGISTRATION`, 기존 번호 변경은 `purpose=MEMBER_PHONE_CHANGE`로 인증 코드를 발급한다.
  - 회원 행 잠금 아래 새 번호의 인증 코드를 한 번 소비하고 `phone_enc`, `phone_hmac`, `phone_verified=true`와
    해당 회원의 `BOOKED` 예약 `owner_phone_hmac`을 같은 트랜잭션에서 저장한다.
    활성 예약 중복 제약과 충돌하면 전화번호와 예약 식별자 변경을 모두 롤백한다.
  - 전화번호가 없는 소셜 회원의 최초 등록과 기존 회원의 번호 변경에 같은 API를 사용한다. `users.phone_hmac`은 null 외 값에 UNIQUE 제약을 적용한다.
  - 비회원 이력 가져오기는 `/api/v1/me/guest-claims/**` 계약을 사용하며 번호 변경만으로 자동 이관하지 않는다.
  - `GET /api/v1/me`의 `phone`은 최초 등록 전 `null`일 수 있다.

#### 2.12.0.5.1 비밀번호 최근 본인 확인

```http
POST /api/v1/me/reauthentication/password
Cookie: HG_SESSION={sessionToken}
X-XSRF-TOKEN: {csrfToken}

{
  "currentPassword": "password123"
}
```

- 성공: `204 No Content`, 현재 세션·회원 ID·자격 버전에 결합된 최근 본인 확인을 10분간 기록
- 에러:
  - `400 INVALID_INPUT` — 빈 값 또는 UTF-8 72바이트 초과
  - `401 INVALID_CREDENTIALS` — 현재 비밀번호 불일치 또는 로컬 비밀번호가 없음
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `429 TOO_MANY_REQUESTS` — IP 또는 회원 ID별 시도 초과
- 정책:
  - 비밀번호 불일치는 회원 세션 만료가 아니므로 프런트가 자동 로그아웃하지 않는다.
  - IP와 회원 ID 제한은 Redis 장애 시 fail-closed한다.

#### 2.12.0.5.2 기준 이메일 소유 확인과 최초 등록

인증 코드 발송:

```http
POST /api/v1/me/email-verifications
Cookie: HG_SESSION={sessionToken}
X-XSRF-TOKEN: {csrfToken}
Content-Type: application/json

{
  "email": "naver-member@example.com"
}
```

인증한 이메일 등록:

```http
PATCH /api/v1/me/email
Cookie: HG_SESSION={sessionToken}
X-XSRF-TOKEN: {csrfToken}
Content-Type: application/json

{
  "email": "naver-member@example.com",
  "verificationCode": "483921"
}
```

- 두 요청의 성공: `204 No Content`
- 에러:
  - `400 INVALID_INPUT` — 이메일 형식·254자 상한 또는 6자리 숫자 코드 형식 불일치
  - `400 EMAIL_VERIFICATION_FAILED` — 발송 성공이 확인된 미소모·유효 코드가 아님
  - `401 UNAUTHORIZED` — 회원 세션이 없거나 세션의 자격 버전이 현재 회원과 다름
  - `403 REAUTHENTICATION_REQUIRED` — 최근 본인 확인이 없거나 만료됨
  - `409 EMAIL_ALREADY_EXISTS` — 현재 회원에게 기준 이메일이 이미 있거나 다른 회원이 사용하는 이메일
  - `429 TOO_MANY_REQUESTS` — IP, 회원 ID 또는 정규화 이메일별 발송·확인 시도 초과
  - `503 SERVICE_UNAVAILABLE` — fail-closed 처리율 제한 저장소 장애, SMTP 호출 차단·대기열 포화·timeout 또는 발송 실패
- 정책:
  - 주 대상은 기준 이메일이 없는 Naver 전용 회원이지만, 계약은 로그인한 회원 중 `email=null`인 계정에 적용한다. Naver가 제공한 프로필 이메일을 자동 채우거나 같은 이메일의 기존 회원과 병합하지 않는다.
  - 두 요청 모두 현재 회원 ID·`credential_version`에 결합된 최근 10분의 비밀번호 또는 연결된 소셜 계정 재인증과 SPA CSRF를 요구한다.
  - 이메일은 앞뒤 공백을 제거하고 소문자로 통일한다. 6자리 코드는 5분 유효하며 회원 ID·자격 버전·정규화 이메일에 결합한다.
  - 두 경로는 하나의 IP 버킷으로 합산해 5회/1분으로 제한한다. 발송은 회원 ID와 정규화 이메일별 각각 3회/10분, 등록 시도는 각각 5회/10분으로 제한하며 Redis 장애 시 fail-closed한다.
  - 서버는 인증 행을 먼저 커밋하고 DB 트랜잭션 밖에서 전용 SMTP를 호출한다. 발송 성공이 기록된 같은 회원의 가장 최근 코드만 사용할 수 있고, 실패한 새 발송은 이전 정상 코드를 무효화하지 않는다. 일반 알림 outbox와 이메일 fallback은 사용하지 않으며 자동 재발송 대신 사용자가 다시 요청한다.
  - DB에는 이메일·코드 HMAC과 보호된 행 복원 및 local/dev 조회에 필요한 코드 AES-GCM 암호문만 저장한다. 검증에 성공한 코드는 한 번 소비한다.
  - 등록은 회원 행을 잠근 뒤 이메일 유일 제약을 `saveAndFlush`로 최종 확인한다. 성공하면 `credential_version`을 증가시키고 현재 요청 세션을 포함한 기존 회원 세션을 폐기해 새 이메일로 다시 로그인하게 한다.
  - 이메일 등록만으로 로컬 비밀번호를 자동 생성하지 않는다. 검증된 휴대폰도 등록한 회원은 2.12.0.7의 SMS 비밀번호 재설정으로 최초 로컬 비밀번호를 설정할 수 있다.

#### 2.12.0.6 로그인 비밀번호 변경

```http
PATCH /api/v1/me/password
Cookie: HG_SESSION={sessionToken}

{
  "currentPassword": "password123",
  "newPassword": "newPassword456"
}
```

- 성공: `204 No Content`
- 에러:
  - `400 INVALID_INPUT` — 비밀번호 길이 또는 요청 형식 불일치
  - `401 INVALID_CREDENTIALS` — 현재 비밀번호 불일치
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `409 LOCAL_PASSWORD_NOT_SET` — 소셜 로그인만 사용하는 회원
  - `422 PASSWORD_UNCHANGED` — 현재 비밀번호와 새 비밀번호가 같음
- 정책:
  - 현재 비밀번호는 `PasswordEncoder.matches(...)`로 확인하고 새 비밀번호는 BCrypt로 다시 해시한다. 두 값 모두 UTF-8 72바이트 이하여야 한다. 롤백 호환 기간에는 식별자 없는 형식과 `{bcrypt}` 형식을 모두 읽고 식별자 없는 형식으로 쓴다.
  - 성공하면 `credential_version`을 증가시키고 현재 요청을 포함한 모든 회원 세션을 무효화한다.
  - 검증된 기준 이메일이 있는 소셜 전용 회원은 이 API 대신 2.12.0.7의 SMS 재설정으로 최초 로컬 비밀번호를 설정한다. 기준 이메일이 없는 회원은 먼저 2.12.0.5.2에서 이메일을 등록한다.

#### 2.12.0.7 검증된 휴대폰으로 비밀번호 재설정

```http
POST /api/v1/auth/password/reset

{
  "email": "member@example.com",
  "phone": "01012345678",
  "verificationCode": "483921",
  "newPassword": "newPassword456"
}
```

- 성공: `204 No Content`
- 에러:
  - `400 INVALID_INPUT` — 이메일·비밀번호·인증코드 요청 형식 불일치
  - `400 PASSWORD_RESET_FAILED` — 이메일, 저장된 검증 전화번호 또는 인증코드 불일치
  - `422 PASSWORD_UNCHANGED` — 기존 로컬 비밀번호와 새 비밀번호가 같음
  - `429 TOO_MANY_REQUESTS` — IP 또는 전화번호별 확인 시도 초과
  - `503 SERVICE_UNAVAILABLE` — fail-closed 처리율 제한 저장소 장애
- 정책:
  - 검증된 기준 이메일이 저장된 회원만 사용할 수 있다. 이메일과 회원에게 저장된 `phoneVerified=true` 전화번호가 일치하고, `purpose=PASSWORD_RESET`으로 발급한 같은 번호의 미소모·유효 SMS 코드를 한 번 소비해야 한다.
  - 새 비밀번호는 8~72자이면서 UTF-8 72바이트 이하다.
  - 계정·전화번호·인증코드 중 어느 값이 틀렸는지는 `PASSWORD_RESET_FAILED` 하나로 응답해 계정 존재 여부를 구분하지 못하게 한다.
  - `password_hash=null`인 Google 소셜 전용 회원과 2.12.0.5.2에서 기준 이메일을 직접 등록한 Naver 소셜 전용 회원도 성공할 수 있으며, 성공 후 이메일 로그인이 활성화된다.
  - 성공하면 `credential_version`을 증가시키고 해당 회원의 모든 세션을 무효화한다.

#### 2.12.0.8 회원 탈퇴

```http
DELETE /api/v1/me
Cookie: HG_SESSION={sessionToken}
```

- 성공: `204 No Content`, 현재 세션을 포함한 기존 회원 세션 폐기
- 에러:
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `403 REAUTHENTICATION_REQUIRED` — 최근 본인 확인이 없거나 현재 자격 버전과 다름
  - `422 ACCOUNT_WITHDRAWAL_BLOCKED` — 미종결 결제 시도·주문·클레임·예약, 미완료 예약 취소 후속 작업·환불 또는 사용 가능한 미만료 8회권이 있음
- 정책:
  - 비밀번호 또는 현재 연결된 소셜 계정으로 최근 본인 확인을 먼저 완료한다. 화면의 `탈퇴` 확인 문자열은 의사 확인이며 이 소유권 증명을 대신하지 않는다.
  - 회원 행을 잠그고 차단 활동을 다시 확인해 탈퇴와 새 거래 생성을 직렬화한다.
  - 잠근 회원 행의 현재 `credential_version`이 세션 재인증 증명의 예상 버전과 다르면 탈퇴하지 않는다.
  - 이메일·이름은 재사용 가능한 탈퇴 식별값으로 바꾸고 전화번호·비밀번호·소셜 연결을 제거한다. `withdrawnAt`과 새 자격 버전을 저장하며 주문·예약·정산 이력은 보존한다.
  - 탈퇴 회원은 로그인과 일반 회원 조회에서 제외한다. 커밋 뒤 이전 자격 버전의 Redis 세션을 폐기한다.

#### ~~2.12.1 회원 예약 생성~~ (2026-04-22 제거)

> 회원 예약 생성도 `POST /api/v1/payments/prepare` (`context=BOOKING`, `payload.userId` 지정) → `POST /api/v1/payments/confirm`으로 단일화됨. 8회권 사용 예약은 `payload.passId`를 채워 amount=0 → confirm 직접 호출 경로를 탄다. 2.15 결제 API 참조.

#### ~~2.12.2 회원 주문 생성~~ (2026-04-22 제거)

> 회원 주문 생성도 `POST /api/v1/payments/prepare` (`context=ORDER`, `payload.userId` 지정) → `POST /api/v1/payments/confirm`으로 단일화됨. 2.15 결제 API 참조.

#### 2.12.3 회원 목록/상세 조회

- `GET /api/v1/me/bookings` — 회원 예약 목록
- `GET /api/v1/me/bookings/page?cursor={cursor}&size=20` — 회원 예약 커서 페이지
- `GET /api/v1/me/bookings/{id}` — 회원 예약 상세
- `PATCH /api/v1/me/bookings/{id}/participants` — 예약 인원 부분취소
- `GET /api/v1/me/vacancy-alerts` — 현재 대기 중인 회원 빈자리 알림 신청 목록
- `GET /api/v1/me/orders` — 회원 주문 목록
- `GET /api/v1/me/orders/page?cursor={cursor}&size=20` — 회원 주문 커서 페이지
- `GET /api/v1/me/orders/{id}` — 회원 주문 상세
- `GET /api/v1/me/passes` — 회원 8회권 목록
- `GET /api/v1/me/passes/page?cursor={cursor}&size=20` — 회원 8회권 커서 페이지
- `GET /api/v1/me/passes/{id}` — 회원 8회권 상세
- `POST /api/v1/me/passes/{id}/refund` — 소유한 8회권 잔여 횟수 정산 환불
- `DELETE /api/v1/me/orders/{id}` — 승인 대기 주문 취소
- `POST /api/v1/me/orders/{id}/delay-response` — 제작 지연 제안 수락/거절

회원 주문 액션은 세션 소유권을 검증한다. 취소는 `PAID_APPROVAL_PENDING`, 지연 응답은 `DELAY_CONSENT_PENDING`에서만 허용하며 응답의 환불 상태는 실제 PG 완료와 분리한다.

회원 주문 상세·예약 상세와 8회권 목록·페이지·상세에는 필수 nullable 문자열 `receiptUrl`을 포함한다. 현재 거래 소유권을 확인한 뒤 기존 결제 이력의 유료 `CONFIRMED` 영수증 URL을 조회하며 0원·과거 미기록·URL이 없는 결제는 `null`이다. 주문 상세 DTO를 공유하는 비회원 주문 조회도 같은 필드를 반환한다. 회원에게 가져온 비회원 주문·예약은 현재 거래 소유자가 조회할 수 있다. 8회권 목록은 영수증을 일괄 조회한다.

회원 예약 상세 응답은 `passBooking`과 `cancelPolicy`를 포함한다.

```json
{
  "bookingId": 1,
  "classId": 1,
  "slotId": 42,
  "status": "BOOKED",
  "className": "향수 클래스",
  "startAt": "2026-03-01T10:00:00",
  "endAt": "2026-03-01T12:00:00",
  "depositAmount": 0,
  "balanceAmount": 0,
  "balanceStatus": "PAID",
  "passBooking": true,
  "cancelPolicy": {
    "cancellable": true,
    "refundable": false,
    "deadlineAt": "2026-03-01T00:00:00",
    "passCreditRestorable": false,
    "warningCode": "PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE"
  },
  "refund": null
}
```

회원 8회권 목록의 각 항목과 상세 응답은 같은 형태를 사용하며, 환불 요청이 있으면 공개 가능한 진행 상태만 포함한다.

```json
{
  "passId": 300,
  "planCode": "REGULAR_CRAFT_8",
  "planName": "정규 공예 8회권",
  "purchasedAt": "2026-07-01T10:00:00",
  "expiresAt": "2026-09-29T00:00:00",
  "totalCredits": 8,
  "remainingCredits": 0,
  "totalPrice": 320000,
  "refund": {
    "amount": 320000,
    "status": "PROCESSING"
  }
}
```

공통 정책:
- 인증 실패 시 `401 UNAUTHORIZED`
- 다른 회원의 리소스 접근 시 `404 NOT_FOUND`
- 기존 배열 목록 경로는 `/api/v1` 응답 호환을 위해 유지하되 최신 100건까지만 반환한다. 신규 화면은 `/page`를 사용하며 응답은 `{content,nextCursor,hasMore}`다. `size`는 1~100이고 `(createdAt,id)` 또는 해당 이력의 생성 시각과 ID 내림차순 커서로 다음 페이지를 잇는다.
- 8회권 예약에서 `cancelPolicy.warningCode=PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE`이면 취소해도 크레딧이 복구되지 않는다. 취소 확인창과 완료 알림은 이 사실을 한국어로 명확히 알린다.
- 신규 `REGULAR_CRAFT_8`은 `passEligible=true`이고 카테고리가 `PERFUME`가 아닌 클래스에만 사용할 수 있다.
- 회원 예약·주문 상세와 8회권 목록·상세의 `refund`는 `{amount,status}` 또는 `null` 계약을 사용한다. 본인 소유권 검증 후 조회하며 내부 환불 ID와 실패 사유는 노출하지 않는다.
- 환불 상태는 `REQUESTED`, `PROCESSING`, `RETRYABLE`, `RECONCILIATION_REQUIRED`, `SUCCEEDED`, `FAILED` 중 하나다. 고객 화면은 비종결 상태만 제한된 간격으로 다시 조회한다.

#### 2.12.4 회원 상품 Q&A 작성

```http
POST /api/v1/me/products/{productId}/qna
Cookie: HG_SESSION={sessionToken}

{
  "title": "재입고 예정이 있나요?",
  "content": "다음 달에도 구매 가능한지 궁금합니다.",
  "secret": true
}
```

- 성공: `201 Created`
- 에러:
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `404 NOT_FOUND` — 상품 미존재
- 정책:
  - 작성 주체는 회원(User)만 허용한다.
  - `title`은 공백이 아닌 200자 이하, `content`는 공백이 아닌 16,000자 이하로 제한한다.
  - `secret=true`이면 공개 목록에서 제목을 숨기고 공개 상세 조회를 거절한다.
  - 응답에는 작성 결과 요약만 반환한다.

작성자 목록:

```http
GET /api/v1/me/products/{productId}/qna
Cookie: HG_SESSION={sessionToken}
```

신규 화면은 `GET /api/v1/me/products/{productId}/qna/page?cursor={cursor}&size=20`의
`{content,nextCursor,hasMore}` 응답으로 다음 작성글을 이어서 조회한다. 기존 배열 경로는 최신 100건으로 제한한다.

- 성공: `200 OK`
- 응답 항목: `id`, `title`, `secret`, `hasReply`, `createdAt`
- 정책:
  - 현재 회원이 해당 상품에 작성한 Q&A만 최신순으로 반환한다.
  - 본문·답변 전문은 목록에 포함하지 않는다.
  - 프론트는 공개 목록의 ID와 이 목록을 대조해 작성자에게만 비밀글 열기 동작을 표시한다.

작성자 상세:

```http
GET /api/v1/me/products/{productId}/qna/{id}
Cookie: HG_SESSION={sessionToken}
```

- 성공: `200 OK`
- 에러:
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `404 NOT_FOUND` — Q&A가 없거나 URL의 상품·현재 회원 소유와 일치하지 않음
- 정책:
  - 일반글과 비밀글 모두 로그인한 작성자 본인만 제목·본문·답변을 조회한다.
  - 비밀글 접근 권한을 별도 공유 비밀번호로 위임하지 않는다.

#### 2.12.5 회원 1:1 문의 작성/조회

- `POST /api/v1/me/inquiries` — 회원 문의 생성
- `GET /api/v1/me/inquiries` — 내 문의 목록
- `GET /api/v1/me/inquiries/page?cursor={cursor}&size=20` — 내 문의 커서 페이지
- `GET /api/v1/me/inquiries/{id}` — 내 문의 상세

```http
POST /api/v1/me/inquiries
Cookie: HG_SESSION={sessionToken}

{
  "title": "배송 일정 문의",
  "content": "이번 주 안에 수령 가능한지 확인 부탁드립니다."
}
```

- 성공: 생성 `201 Created`, 조회 `200 OK`
- 에러:
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `404 NOT_FOUND` — 다른 회원 문의 또는 미존재
- 정책:
  - 문의 작성/조회는 본인 리소스로만 제한한다.
  - 생성 요청의 `title`은 공백이 아닌 200자 이하, `content`는 공백이 아닌 16,000자 이하로 제한한다.
  - 응답에는 `hasReply`, `replyContent`, `repliedAt`를 포함한다.
  - 기존 배열 목록은 최신 100건으로 제한하고 신규 화면은 `(createdAt,id)` 내림차순의 `{content,nextCursor,hasMore}` 페이지를 사용한다.

#### 2.12.6 회원 장바구니

- `GET /api/v1/me/cart` — 내 장바구니 조회
  - 응답:

```json
{
  "items": [
    {
      "cartItemId": 31,
      "productId": 1,
      "productVariantId": null,
      "productName": "시그니처 캔들",
      "productType": "READY_STOCK",
      "basePrice": 39000,
      "variantPriceAdjustment": 0,
      "textOptionPriceAdjustment": 0,
      "price": 39000,
      "options": [],
      "qty": 2,
      "subtotal": 78000,
      "available": true
    }
  ],
  "totalAmount": 78000,
  "cartVersion": "f3029b77e4e6080fcb48f1ac6f15fa76c27d2c44f748c8f772b705bf7fe79c76"
}
```

- `POST /api/v1/me/cart/items`
  - 요청: `{ "productId": 1, "productVariantId": null, "textInputs": [], "qty": 2 }`
  - `productId`, `qty`는 필수다. 주문제작은 `productVariantId`와 직접입력 그룹의 `{groupKey,value}`를 함께 보내며 직접입력이 없으면 `textInputs`를 생략하거나 빈 배열로 보낸다.
  - 응답: `201 Created`
- `POST /api/v1/me/cart/merge`
  - 요청: `{ "expectedCustomerId": 1, "idempotencyKey": "UUID", "items": [{ "productId": 1, "productVariantId": null, "textInputs": [], "qty": 2 }] }`
  - `expectedCustomerId`, 각 항목의 `productId`, `qty`는 필수다. 직접입력이 없으면 `textInputs`를 생략하거나 빈 배열로 보낸다.
  - `items`는 1~100건이다.
  - 응답: `204 No Content`
  - 로그인 직전의 비회원 장바구니를 한 번에 합친다. 같은 회원과 멱등키의 재요청은 수량을 다시 더하지 않는다.
  - `expectedCustomerId`가 현재 인증된 회원과 다르면 세션 전환 경합으로 보고 `409 CONFLICT`로 거절한다. 클라이언트는 요청 전·후의 세션 스냅샷도 함께 확인해 이전 회원의 병합 결과를 현재 화면에 적용하지 않는다.
  - 같은 회원과 멱등키로 다른 상품·수량을 보내면 `409 CONFLICT`로 거절한다.
- `PUT /api/v1/me/cart/items/{cartItemId}`
  - 요청: `{ "qty": 3 }`
  - `qty`는 필수다.
  - 응답: `200 OK` 본문 없음
- `DELETE /api/v1/me/cart/items/{cartItemId}`
  - 응답: `204 No Content`
- 장바구니 결제는 별도 checkout API를 두지 않는다. `POST /api/v1/payments/prepare`에 `context=ORDER`, `payload.userId`, `payload.cartCheckout=true`, `payload.items=[]`를 보내 시작한다.

공통 정책:
- 인증 실패 시 `401 UNAUTHORIZED`
- 장바구니는 회원 전용이며 `상품 + variant + 정규화된 직접입력값` 단위로 중복 없이 관리한다. 같은 SKU라도 각인 문구가 다르면 별도 `cartItemId`를 가진다.
- 같은 회원의 모든 장바구니 변경은 소유자 잠금을 먼저 획득한다. 최초 추가가 동시에 실행돼 기존 항목 행이 아직 없어도 한 행에 수량을 합산한다.
- 추가·수정·병합 수량은 SKU별 1~99개다. 직접입력값이 달라 행이 여러 개여도 같은 SKU 수량을 합산하며, 변경 후 합계가 현재 재고를 넘으면 `409 INVENTORY_NOT_ENOUGH`로 거절한다. 이 검증은 재고를 예약하지 않으므로 결제 prepare·confirm에서도 재검증한다.
- 비회원 장바구니 병합의 멱등키 기록과 회원 장바구니 수량 변경은 같은 DB 트랜잭션으로 처리한다.
- 장바구니 병합 멱등 응답은 요청 생성 후 7일간 보장한다. 클라이언트는 이 기간을 넘겨 같은 키를 재사용하지 않으며 서버는 보존 배치에서 오래된 기록을 정리한다.
- 비회원 장바구니 화면은 로컬 항목의 `productId`·variant·직접입력값과 최신 공개 상품 상세를 조립해 상품명·옵션·단가·소계·구매 가능 여부를 표시한다. 서로 다른 직접입력 문구는 각각 수량 변경·삭제하고, 주문은 로그인 후 병합이 완료된 회원 장바구니에서 진행한다.
- 비회원 추가·수량 증가는 공개 상품의 `stockQuantity` 또는 선택한 `variants[].quantity`와 같은 SKU 합산 99개 한도를 저장 직전에 확인한다. 재고 감소로 초과한 항목은 수량 조정을 안내하며 감소·삭제는 허용한다.
- 회원 수량 감소·유지는 소유권과 수량 범위만 확인하며 현재 판매 옵션·재고 검증은 증가에 적용한다. 관리자가 옵션 표시 순서를 바꿔도 동일 각인은 기존 행에 합산한다. V163 이전 중복 행은 ID·수량을 보존하고 이후 추가·병합은 입력이 같은 최소 ID 행을 사용한다.
- 상품 상세의 회원 다건 담기도 `/api/v1/me/cart/merge`를 사용한다. 선택한 모든 항목을 한 요청으로 전송하고 결과가 불명확한 재시도에는 같은 멱등키를 사용한다. 비회원은 한 번의 로컬 장바구니 잠금·저장으로 반영한다.
- 클라이언트는 병합 응답을 확인할 때까지 회원·멱등키·상품 스냅샷을 바꾸지 않는다. 로컬 항목은 요청 당시 계보를 함께 보존하고 성공 후 같은 계보의 스냅샷 수량만 차감한다. 도중에 추가된 수량은 새 멱등키로 이어서 병합하며, 로그아웃 뒤 상품을 삭제하고 다시 담아 새 계보가 된 수량은 이전 계정의 늦은 성공 응답이 차감하지 않는다. 계보 식별자는 브라우저 내부 값이며 API 요청에는 보내지 않는다.
- 같은 브라우저의 여러 탭은 비회원 장바구니 추가·수량 변경·삭제와 병합의 최신 로컬 조회부터 성공분 제거까지를 같은 탭 간 잠금으로 직렬화한다. 한 탭의 로컬 변경은 다른 탭에도 반영하며, 병합 응답 뒤 보류 요청이 이미 정리됐더라도 응답을 받은 탭은 자신이 전송한 계보 스냅샷을 제거한다.
- 상품이 `ACTIVE`가 아니거나 같은 SKU의 장바구니 합산 수량보다 재고가 적으면 관련 항목을 모두 `available=false`로 표시하며, checkout 시 구매 가능한 항목만 주문으로 전환한다.
- `cartVersion`은 항목 순서·표시 정보·수량·구매 가능 여부를 SHA-256으로 만든 불투명 스냅샷 식별자다. 클라이언트는 값을 해석하거나 직접 만들지 않고 결제 준비의 `expectedCartVersion`으로 그대로 돌려보낸다.
- 장바구니 prepare는 같은 회원의 장바구니 변경과 직렬화한 뒤 `expectedCartVersion`을 현재 스냅샷과 비교한다. 다르면 `409 CART_SNAPSHOT_CHANGED`로 최신 장바구니 확인을 요구하고 결제 시도를 만들지 않는다. 일치하면 구매 가능한 항목만 서버에서 선택하고, confirm 성공 시 prepare에서 확정한 수량만 차감한다. 결제 진행 중 추가한 같은 상품 수량과 다른 상품은 유지한다.

#### 2.12.7 회원 알림함

- `GET /api/v1/me/notifications?page=0&size=20`
  - 응답:

```json
[
  {
    "id": 101,
    "eventType": "ORDER_PAID",
    "aggregateType": "ORDER",
    "aggregateId": 42,
    "deliveredAt": "2026-03-28T09:15:00",
    "readAt": null,
    "read": false
  }
]
```

- `GET /api/v1/me/notifications/unread-count`
  - 응답: `{ "count": 3 }`
- `PATCH /api/v1/me/notifications/{id}/read`
  - 응답: `200 OK` 본문 없음
- `PATCH /api/v1/me/notifications/read-all`
  - 응답: `200 OK` 본문 없음

공통 정책:
- 인증 실패 시 `401 UNAUTHORIZED`
- `page`는 0 이상, `size`는 1~100이어야 하며 표현 가능한 OFFSET 범위를 넘으면 `400 INVALID_INPUT`으로 거절한다.
- 본인 알림만 조회/읽음 처리할 수 있고, 타인 알림 ID는 찾을 수 없는 것처럼 거절한다.
- 목록은 발송 완료된 논리 알림을 `deliveredAt DESC` 기준으로 페이지네이션하며, 카카오톡 실패 후 SMS 성공처럼 채널 로그가 여러 건이어도 한 건만 반환한다.
- `readAt != null`이면 `read=true`로 본다.
- 알림 목록의 최초 로딩과 실패를 빈 목록으로 표시하지 않는다. 실패 시 재시도를 제공하고, 재조회 실패 때는 이미 받은 목록을 유지한다.
- 알림 팝오버는 모바일 화면 폭을 넘지 않으며 trigger의 펼침 상태·연결 대상을 노출하고 Escape로 닫은 뒤 trigger로 포커스를 돌린다.
- 발송 완료, 현재 의미가 사라져 발송 없이 종결된 `OBSOLETE` 리마인드와 최종 실패 outbox는 각각 `processed_at`부터 180일 뒤 채널 감사 로그와 함께 보존 배치에서 삭제한다. 재시도 가능한 `PENDING`과 실행 중인 `PROCESSING` outbox는 이 정책으로 삭제하지 않는다.

### 2.13 공개 Product Q&A API
#### 2.13.1 상품 Q&A 목록 조회

```http
GET /api/v1/products/{productId}/qna
```

신규 화면은 `GET /api/v1/products/{productId}/qna/page?cursor={cursor}&size=20`의
`{content,nextCursor,hasMore}` 응답으로 다음 글을 조회한다. 기존 배열 경로는 최신 100건으로 제한한다.

- 성공: `200 OK`
- 정책:
  - 작성자 이름은 마스킹해 반환한다.
  - `secret=true`인 글은 제목을 `[비밀글입니다]`로 가려서 반환한다.
  - 공개 목록에는 본문/답변 전문을 포함하지 않는다.
  - 목록 쿼리는 제목·비밀 여부·답변 존재 여부·작성 시각만 projection으로 읽고 `TEXT` 본문과 답변 전문을 로드하지 않는다.

#### 2.13.2 상품 Q&A 상세 조회

일반글:

```http
GET /api/v1/products/{productId}/qna/{id}
```

- 성공: `200 OK`
- 에러:
  - `403 FORBIDDEN` — 비밀글 공개 상세 조회
  - `404 NOT_FOUND` — Q&A 미존재 또는 URL의 상품에 속하지 않음
- 정책:
  - `secret=false`인 일반글의 제목·본문·답변을 비밀번호 없이 반환한다.
  - `secret=true`인 비밀글은 이 경로에서 상세를 반환하지 않는다. 작성자는 회원 상세 API를 사용한다.

### 2.14 관리자 Q&A / 문의 API

#### 2.14.1 관리자 상품 Q&A 조회/답변

- `GET /api/v1/admin/qna?productId={productId}` — 특정 상품의 Q&A 목록 조회
- `GET /api/v1/admin/qna/page?productId={productId}&cursor={cursor}&size=20` — 특정 상품의 Q&A 커서 페이지 조회
- `GET /api/v1/admin/qna/unanswered?cursor={cursor}&size=20` — 전체 미답변 Q&A 최신순 커서 조회
- `POST /api/v1/admin/qna/{id}/reply` — Q&A 답변 등록

정책:
- 인증: `Authorization: Bearer {token}`
- 기존 상품별 배열 목록은 최신 100건으로 제한한다. 상품별 페이지와 미답변 목록 응답은
  `{content, nextCursor, hasMore}`이고 `(createdAt, id)` 내림차순으로 조회한다. `size` 범위는 1~100이다.
- 답변 작성 시 `replyContent`, `repliedAt`, `repliedBy`를 기록한다.
- `replyContent`는 공백이 아닌 16,000자 이하로 제한한다.
- 이미 답변이 있는 글에 재답변을 시도하면 `409 CONFLICT`로 거절하고 기존 답변과 알림 outbox를 변경하지 않는다.
- 답변 저장과 `PRODUCT_QNA_ANSWERED` 회원 알림 outbox insert를 같은 트랜잭션으로 처리한다. 멱등키는 회원·이벤트·`PRODUCT_QNA`·Q&A ID 조합이다.
- 관리자 화면은 답변 성공 뒤 관리자 목록뿐 아니라 같은 상품의 공개·회원 Q&A 목록과 상세 캐시를 상품 접두사로 함께 무효화한다.

#### 2.14.2 관리자 1:1 문의 조회/답변

- `GET /api/v1/admin/inquiries?cursor={cursor}&size=20` — 최신 문의 커서 페이지 조회
- `GET /api/v1/admin/inquiries/{id}` — 문의 상세 조회
- `POST /api/v1/admin/inquiries/{id}/reply` — 문의 답변 등록

정책:
- 인증: `Authorization: Bearer {token}`
- 회원 이름을 함께 반환한다.
- 목록 응답은 `{content, nextCursor, hasMore}`이고 `size` 범위는 1~100이다.
- `replyContent`는 공백이 아닌 16,000자 이하로 제한한다.
- 이미 답변이 있는 문의에 재답변을 시도하면 `409 CONFLICT`로 거절하고 기존 답변과 알림 outbox를 변경하지 않는다.
- 답변 저장과 `INQUIRY_ANSWERED` 회원 알림 outbox insert를 같은 트랜잭션으로 처리한다. 외부 Alimtalk/SMS 발송은 커밋 뒤 실행하며 같은 문의의 중복 발송 요청은 멱등키로 합친다.

#### 2.14.3 상품·클래스 후기 API

공개 조회:

- `GET /api/v1/products/{productId}/reviews?rating={1..5}&sort={LATEST|RATING_HIGH|RATING_LOW}&cursor={cursor}&size=20`
- `GET /api/v1/classes/{classId}/reviews?rating={1..5}&sort={LATEST|RATING_HIGH|RATING_LOW}&cursor={cursor}&size=20`

응답:

```json
{
  "summary": {
    "reviewCount": 2,
    "averageRating": 4.5,
    "histogram": {
      "rating1": 0,
      "rating2": 0,
      "rating3": 0,
      "rating4": 1,
      "rating5": 1
    }
  },
  "filteredCount": 2,
  "content": [
    {
      "id": 31,
      "rating": 5,
      "content": "마감이 깔끔하고 선물하기 좋았습니다.",
      "authorName": "홍**",
      "sourceType": "ORDER_ITEM",
      "verifiedTransaction": true,
      "createdAt": "2026-08-08T15:30:00",
      "updatedAt": "2026-08-08T15:30:00",
      "edited": false,
      "editedAt": null,
      "officialReply": {
        "content": "정성스러운 후기 고맙습니다.",
        "createdAt": "2026-08-08T16:00:00",
        "edited": false,
        "editedAt": null
      },
      "helpfulCount": 3,
      "images": [
        {
          "id": 11,
          "imageUrl": "/api/v1/media/images/00000000-0000-0000-0000-000000000011.jpg",
          "sortOrder": 0,
          "createdAt": "2026-08-08T15:31:00"
        }
      ]
    }
  ],
  "nextCursor": null,
  "hasMore": false
}
```

- 공개 목록과 요약은 삭제되지 않은 `PUBLISHED`만 포함한다. 작성자 이름은 마스킹한다.
- `summary`의 수·평균·별점 분포는 현재 `rating` 필터와 무관한 전체 공개 후기 기준이고, `filteredCount`는 현재 필터 결과 수다.
- 최신순은 `(createdAt,id)`, 별점순은 `(rating,createdAt,id)`를 사용한다. `cursor`는 정렬 종류를 포함하는 opaque 값이며 다른 정렬에 재사용하면 `400 INVALID_INPUT`이다.
- 공개 응답은 검증된 거래·수정 표식, 공방 답글, 도움돼요 수와 사진만 포함한다. 현재 회원의 반응 여부는 포함하지 않는다.
- 공개 후기가 없으면 `reviewCount=0`, `averageRating=0.0`, 모든 분포 `0`, `filteredCount=0`, `content=[]`를 반환한다.
- 상품이나 클래스가 없으면 `404 NOT_FOUND`다. 비활성 클래스의 후기 목록은 후기 대상 검증 정책에 따라 `422 CLASS_INACTIVE`로 거절한다.

회원 작성·조회·수정·삭제:

- `POST /api/v1/me/reviews/products` — `{orderItemId,rating,content}`로 상품 후기 작성
- `POST /api/v1/me/reviews/classes` — `{bookingId,rating,content}`로 클래스 후기 작성
- `GET /api/v1/me/reviews/products/{orderItemId}/creation-state` — 주문 품목의 현재 작성 상태
- `GET /api/v1/me/reviews/classes/{bookingId}/creation-state` — 예약의 현재 작성 상태
- `GET /api/v1/me/reviews?cursor={cursor}&size=20` — 내 후기 커서 페이지
- `GET /api/v1/me/reviews/opportunities?cursor={cursor}&size=20` — 작성 가능한 완료 주문 품목·예약 커서 페이지
- `GET /api/v1/me/reviews/reactions?reviewIds=31,32` — 공개 후기 최대 100건의 도움돼요·신고·본인 소유·상호작용 가능 여부
- `GET /api/v1/me/reviews/orders/{orderId}` — 주문 품목별 내 후기 배열
- `GET /api/v1/me/reviews/bookings/{bookingId}` — 예약의 내 후기 배열(0~1건)
- `PATCH /api/v1/me/reviews/{reviewId}` — `{expectedContentRevision,rating,content}` 수정
- `DELETE /api/v1/me/reviews/{reviewId}` — `204 No Content` 삭제
- `PUT /api/v1/me/reviews/{reviewId}/helpful` — 도움돼요 멱등 등록
- `DELETE /api/v1/me/reviews/{reviewId}/helpful` — 도움돼요 멱등 해제
- `POST /api/v1/me/reviews/{reviewId}/reports` — `{reason,detail}` 신고
- `POST /api/v1/me/reviews/{reviewId}/images` — multipart `file` 사진 첨부
- `GET /api/v1/me/reviews/{reviewId}/images/{imageId}` — 회원 소유 숨김 후기 사진 보호 조회
- `DELETE /api/v1/me/reviews/{reviewId}/images/{imageId}` — 사진 삭제

회원 후기 응답은 `id`, `targetType(PRODUCT|CLASS)`, `targetId`, `targetName`,
`sourceType(ORDER_ITEM|BOOKING)`, `sourceId`, `rating`, `content`,
`status(PUBLISHED|HIDDEN)`, `contentRevision`, nullable 숨김 사유, 작성·수정 시각, `edited`,
`verifiedTransaction`, nullable `officialReply`, `helpfulCount`, 최대 5건의 `images`를 포함한다.

- 상품 후기는 현재 회원 소유 주문 품목의 주문이 `DELIVERED`, `PICKED_UP`, `COMPLETED`일 때만 `201 Created`다.
- 클래스 후기는 현재 회원 소유 예약이 `COMPLETED`일 때만 `201 Created`다.
- 다른 회원의 작성 근거나 후기는 `404 NOT_FOUND`, 미완료 근거는 `422 REVIEW_NOT_ALLOWED`다.
- 주문·예약별 내 후기 조회도 먼저 현재 회원 소유를 확인한다. 본인 원천에 후기가 없을 때만 빈 배열을 반환하고, 타인·미존재 원천은 `404 NOT_FOUND`다.
- 작성 상태는 `AVAILABLE`, `REVIEW_EXISTS`, `RECREATION_BLOCKED`, `NOT_REVIEWABLE`이다. 화면은 이 서버 값으로 작성 폼 노출을 결정한다.
- 작성 기회는 실제 배송·픽업·예약 완료 시각의 역순으로 정렬하고, `(completedAt,targetType,sourceId)`를 포함한 opaque cursor와 `content`, `nextCursor`, `hasMore`를 반환한다. 예전 이용 내역도 페이지를 계속 열어 누락 없이 조회할 수 있다.
- 같은 주문 품목·예약에 활성 후기가 있으면 `409 REVIEW_ALREADY_EXISTS`다. 한 번이라도 숨겨졌던 후기를 삭제한 원천은 `409 REVIEW_RECREATION_BLOCKED`다.
- `rating`은 1~5 정수, `content`는 공백이 아닌 16,000자 이하다.
- 삭제는 사용자 내용과 사진 참조, 현재 행의 숨김 사유·처리자를 지우는 soft-delete다. 재작성 차단 표식과 3년 감사 사건은 별도로 보존한다. 숨김 이력이 없을 때만 원천을 해제해 재작성을 허용하며, 신고·운영 조치·판단 증거가 없는 비차단 tombstone은 `deletedAt`부터 30일 뒤 개인정보 보존 배치가 최대 100건씩 파기한다. 재작성 차단 tombstone은 이 배치 대상이 아니다.
- 회원 수정은 화면이 읽은 `expectedContentRevision`과 현재 값이 다르면 `409 REVIEW_CONTENT_CHANGED`로 거절해 다른 탭의 최신 후기를 덮어쓰지 않는다.
- 후기 사진은 JPEG·PNG, 원본 5MB 이하와 가로·세로 4,096px·총 1,600만 픽셀 이하만 허용한다. 표준 디코더로 실제 형식을 판별하므로 빈 MIME과 `application/octet-stream`은 허용하지만 명시한 JPEG/PNG MIME이 실제 형식과 다르면 거절한다. EXIF 방향을 적용한 뒤 메타데이터 없는 새 파일로 재인코딩하며 후기당 5장까지다. 검증 가능한 서버 디코더가 없는 WebP는 관리자 자산 업로드와 달리 회원 후기에서는 받지 않는다. 업로드는 IP와 회원별 10분에 20회로 제한하고 동시 디코딩 기본 상한 2건이 차면 `429 TOO_MANY_REQUESTS`다. 저장 뒤 DB 연결 실패와 사진 참조 해제는 파일 참조를 재확인해 보상 삭제하며 실패 시 기존 7일 고아 정리를 사용한다.
- 회원은 자기 후기이나 숨김·삭제 후기에 도움돼요·신고를 할 수 없다. 신고 사유는 `SPAM|ABUSIVE|PRIVACY|FALSE_INFORMATION|OTHER`이며 후기·회원당 한 번, IP와 회원별 10분에 10회다.
- 후기 작성·수정·삭제는 회원별 10분에 20회, 도움돼요 등록·해제는 회원별 1분에 60회로 제한한다. 한도 초과는 `429 TOO_MANY_REQUESTS`, 제한 저장소 장애는 fail-closed `503 SERVICE_UNAVAILABLE`다.
- 작성자는 숨김 후기도 수정·사진 관리·삭제할 수 있지만 수정만으로 공개 상태가 바뀌지 않는다. 숨김 후기 사진은 현재 회원 소유와 이미지 연결을 확인하는 `CustomerSession` 전용 조회에서만 `Cache-Control: no-store`로 제공한다.

관리자 운영:

- `GET /api/v1/admin/reviews?targetType={PRODUCT|CLASS}&status={PUBLISHED|HIDDEN}&cursor={cursor}&size=20`
- `GET /api/v1/admin/reviews/{reviewId}` — 신고 화면에서도 사용할 수 있는 현재 후기 단건
- `PATCH /api/v1/admin/reviews/{reviewId}/status` — `{status,reason,expectedContentRevision,expectedVersion}`
- `GET /api/v1/admin/reviews/{reviewId}/moderation-actions` — 숨김·재공개 감사 이력
- `PUT /api/v1/admin/reviews/{reviewId}/reply` — `{expectedVersion,content}` 공방 공식 답글 작성·수정
- `DELETE /api/v1/admin/reviews/{reviewId}/reply?expectedVersion={version}` — 공방 공식 답글 삭제
- `GET /api/v1/admin/reviews/{reviewId}/images/{imageId}` — Bearer 관리자 전용 숨김 후기 현재 사진
- `GET /api/v1/admin/review-evidence/{evidenceId}/images/{sortOrder}` — Bearer 관리자 전용 증거 사진
- `GET /api/v1/admin/review-reports?status={PENDING|ACCEPTED|REJECTED}&cursor={cursor}&size=20`
- `GET /api/v1/admin/review-reports/{reportId}` — 신고자·상세·판단 증거를 포함한 Bearer 관리자 전용 단건
- `PATCH /api/v1/admin/review-reports/{reportId}` — `{decision: ACCEPTED|REJECTED,note}`

- 관리자 목록과 단건은 작성자 회원 ID·이름, 대상·원천, 상태, 숨김 메타데이터와 `contentRevision`, JPA `version`을 포함한다.
- 신고 목록 항목은 `id`, `reviewId`, `reason`, `snapshotStatus`, `status`, `createdAt`만 포함한다. 신고자 ID·신고 상세·판단 정보와 전체 evidence는 선택한 신고의 단건 API를 호출한 뒤에만 반환하고, 관리자 화면도 상세를 펼칠 때 해당 API를 요청한다.
- 상태 변경, 운영 조치 감사 이력, 공식 답글 작성·수정·삭제, 신고 단건 조회·판단과 숨김 후기 현재 사진·증거 사진 조회는 계정 기반 `Authorization: Bearer` 관리자 세션만 허용한다. local API key는 `403 FORBIDDEN`이다.
- `HIDDEN` 전환에는 공백이 아닌 사유가 필요하고 관리자 ID·시각을 기록한다. `PUBLISHED` 재전환은 숨김 메타데이터를 제거한다.
- 상태 변경은 관리자가 읽은 `expectedContentRevision`과 잠근 후기의 현재 revision이 다르면 `409 REVIEW_CONTENT_CHANGED`다. 상태가 실제로 바뀐 때만 당시 별점·본문·수정 시각·정렬된 사진 URL 증거, append-only moderation action과 회원 알림 outbox를 같은 트랜잭션으로 저장한다. 중간 전환이 더 최신 전환으로 대체되면 발송 직전 재검증에서 `OBSOLETE`로 종료한다.
- `expectedVersion`도 잠긴 후기의 JPA `version`과 비교해 다른 관리자의 상태 왕복 전환(ABA)과 공식 답글 덮어쓰기·삭제를 `409 CONFLICT`로 차단한다.
- `contentRevision`은 본문·평점·사진의 의미 변경만 추적하고 JPA `version`은 상태·답글을 포함한 후기 행의 모든 쓰기를 추적한다. 상태 변경에는 두 토큰이 필요하고 답글 변경에는 `expectedVersion`이 필요하다.
- 공식 답글은 후기당 하나를 작성·수정·삭제하며 첫 작성에만 회원 알림을 요청한다. 본문은 공백이 아닌 16,000자 이하다.
- 신고는 신고 시점의 별점·본문·공개 상태·본문 수정 시각·정렬된 사진 URL을 공통 불변 evidence로 포함한다. 과거 신고 이관본은 사진을 복원할 수 없어 `LEGACY_REPORT`, `imagesComplete=false`로 구분한다. 관리자는 `PENDING`을 `ACCEPTED` 또는 `REJECTED`로 한 번만 판단하며, 신고 수만으로 후기를 자동 숨김하지 않는다. 미결 evidence는 결정까지 보존하고 종결 신고·운영 조치와 evidence는 결정 또는 조치 뒤 3년간 보존한다. 신고·운영 조치·evidence의 후기 FK는 `ON DELETE RESTRICT`로 두어 자식 보존기간보다 부모가 먼저 파기되지 않게 한다.
- 증거 전용이거나 숨김·삭제 후기에서만 참조되는 사진은 공개 미디어 조회에서 `404`로 숨긴다. 숨김 후기 현재 사진은 작성자 `CustomerSession` 또는 Bearer 관리자 보호 경로, 판단 증거 사진은 Bearer 관리자 전용 경로에서만 `Cache-Control: no-store`로 제공한다. evidence 만료 시 DB 참조 삭제 후 물리 파일도 커밋 이후 즉시 참조를 재확인해 파기한다.
- 상태·답글·신고 변경 후 관리자, 같은 공개 대상과 회원 후기 cache를 함께 무효화한다.

### 2.15 결제 API (`/api/v1/payments`)

주문/예약/8회권의 표준 결제 생성 경로는 `POST /api/v1/payments/prepare` → `POST /api/v1/payments/confirm`이다.
서버가 `prepare` 단계에서 `orderId(UUID)`와 `amount`를 확정해 `payment_attempt` 레코드(`PENDING`)로 저장하고,
프론트가 Toss 결제창을 통과한 뒤 `confirm`이 동일 `amount` 일치를 강제한 뒤 도메인 저장(주문/예약/8회권)을 수행한다.
회원 장바구니도 같은 prepare/confirm 경로를 사용한다.

회원/비회원 구분은 요청 본문이 아니라 인증 컨텍스트(`HG_SESSION` 쿠키 유무)로 결정한다.
회원 경로는 현재 회원의 `phone`이 존재하고 `phoneVerified=true`여야 하며, 미등록 상태에서는 `422 PHONE_VERIFICATION_REQUIRED`를 반환한다.
8회권 사용 예약과 쿠폰·적립금 전액 결제처럼 amount가 0이면 프론트가 현재 고객 세션에 확정 요청을 저장하고 공통 `/payments/success` 화면에서 PG 없이 `confirm`한다. 응답 유실 시 기존 `orderId`로 상태를 조회·재확인하고 미확인 0원 요청이 있으면 새 prepare를 만들지 않는다. 저장소에 기록하지 못하면 승인 전에 기존 결제 종료 API를 호출한다. 서버 요청·응답 계약은 유료 결제와 같다.

#### 2.15.1 결제 준비 (prepare)

```http
POST /api/v1/payments/prepare
Cookie: HG_SESSION={sessionToken}   # 회원 경로일 때만
Content-Type: application/json

{
  "context": "ORDER",
  "payload": {
    "type": "ORDER",
    "userId": 7,
    "items": [
      { "productId": 1, "productVariantId": null, "textInputs": [], "qty": 2 }
    ],
    "cartCheckout": false,
    "issuedCouponId": 81,
    "rewardAmount": 5000,
    "fulfillmentType": "PICKUP",
    "shippingAddress": null,
    "madeToOrderConsent": false
  }
}
```

```json
{
  "orderId": "f2d3a1b4-9d24-4f0a-8a8a-7c8b06f5b1a2",
  "amount": 78000,
  "context": "ORDER",
  "statusToken": null
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — payload context와 `context` 필드 불일치, 항목 누락, 상품별 수량 1~99 범위 위반, 주문 금액 안전 정수 범위 초과 또는 overflow, 회원/비회원 정보 불일치 등
  - `400 PHONE_VERIFICATION_FAILED` — 비회원 전화번호와 인증 코드가 일치하지 않거나, 코드가 만료·소모됨
  - `404 NOT_FOUND` — 상품/슬롯 미존재
  - `409 CART_SNAPSHOT_CHANGED` — 장바구니 결제의 `expectedCartVersion`이 현재 장바구니와 다름
  - `409 CONFLICT` — 이미 예약·사용된 쿠폰, 중복 쿠폰 발급 또는 동시 혜택 변경 충돌
  - `422 CHANGE_NOT_ALLOWED` — 만료·비활성·최소 주문 금액 미달 등 현재 주문에 적용할 수 없는 쿠폰
  - `422 REWARD_BALANCE_INSUFFICIENT` — 사용할 수 있는 적립금보다 큰 금액 요청
  - `422 PAYMENT_METHOD_NOT_ALLOWED` — `BookingPayload.paymentMethod=BANK_TRANSFER`
  - `422 POLICY_CONSENT_REQUIRED` — 비회원 주문·예약의 현재 정책 동의가 없거나 버전이 일치하지 않음
- 정책:
  - `payload.type`은 `ORDER` / `BOOKING` / `PASS` 중 하나로, 상위 `context`와 일치해야 한다.
  - 금액은 서버가 산출한다. 클라이언트가 `amount`를 보내도 무시되며, `payment_attempt.amount`는 서버 계산값이다.
  - 모든 컨텍스트의 최종 `amount`는 0원 이상 `9,007,199,254,740,991원` 이하의 웹 안전 정수여야 한다. 0원은 유효한 8회권 예약 또는 픽업 상품 금액을 적립금으로 모두 지불한 주문처럼 외부 PG 호출이 없는 내부 승인에 사용한다.
    - `ORDER`: `items`는 0~100건이며 장바구니 결제일 때만 빈 목록을 허용한다. 동일한 `productId + productVariantId + 직접입력값`의 수량을 먼저 합쳐 SKU별 1~99개 제한을 적용한다. 서버가 상품과 옵션을 일괄 조회해 `기본가 + 조합 추가금 + 직접입력 추가금`에 수량을 곱하고, 같은 variant 재고 요구량을 다시 합산한다. `SHIPPING`이면 `app.order.shipping-fee`의 고정액을 더하고 `PICKUP`이면 0원을 더한다. 총액은 `9,007,199,254,740,991원` 이하로 제한한다.
    - `BOOKING`: `passId`가 있으면 0 (8회권 사용 예약, `participantCount=1`), 없으면 `slot.bookingClass.price * participantCount * 10%`이며 결과는 1원 이상
    - `PASS`: `app.pass.total-price`(기본 `PASS_TOTAL_PRICE=240000`)
  - 서버는 prepare 시점의 `ORDER` 상품명·기본가·옵션 추가금·항목 단가·variant ID·선택 옵션·직접입력 문구·상품 유형·고정 사양·관리 방법·예상 제작 기간·배송비·쿠폰 할인·적립금 사용·품목별 배분, `BOOKING` 예약금·잔금·인원, `PASS` 총 가격과 계획을 공개 요청 모델과 분리된 내부 payload로 저장한다. 비회원 주문·예약은 같은 prepare 트랜잭션에서 인증 코드를 잠금 후 한 번 소비하고 `context + orderId + 정규화 전화번호 + nonce`에 HMAC 서명한 결제 귀속 증거로 교체한다. 내부 payload 전체는 `payment_attempt.payload_enc`에 AES-GCM 암호문으로 저장하며 인증 코드 원문은 포함하지 않는다. confirm은 현재 가격을 다시 계산하지 않고 이 스냅샷으로 도메인을 생성하며, 저장된 결제 금액과 `payment_attempt.amount`가 다르면 PG 호출 전에 거절한다.
  - 클라이언트의 `ORDER` payload에는 단가를 받지 않는다.
  - `cartCheckout`은 항상 명시한다. 직접 주문은 `false`, 회원 장바구니 주문은 `true`다.
  - `ORDER` payload는 `fulfillmentType=SHIPPING|PICKUP`을 반드시 포함한다. `SHIPPING`은 구조화된 `shippingAddress`가 필수이고 `PICKUP`은 `shippingAddress=null`이어야 한다.
  - 주문제작 상품이 하나라도 포함되면 `madeToOrderConsentVersion`이 현재 정책 버전과 일치하고 `madeToOrderConsent=true`여야 한다. 서버는 현재 동의 문구 버전·전문·서버 동의 시각을 내부 payload에 확정하고 confirm 시 `orders`로 옮긴다. 기성품만 포함되면 이 값과 무관하게 동의 스냅샷을 만들지 않는다.
  - V97 이전 구형 prepare 중 상품 유형이 없고 주문제작 동의도 없는 항목은 당시 기성품으로 해석해 `READY_STOCK` 주문 항목으로 확정한다. 상품 유형은 없지만 주문제작 동의가 남은 prepare는 구매조건을 재현할 수 없으므로 PG 호출 전에 `400 INVALID_INPUT`으로 거절하고 새 prepare를 요구한다. 이미 PG 승인이 저장된 구형 주문제작 시도가 자동 복구되면 주문을 만들지 않고 기존 보상 환불 경계로 격리한다.
  - 클라이언트는 `GET /api/v1/orders/policy`의 `shippingFee`를 사전 표시용으로 사용하되 요청 금액으로 보내지 않는다. prepare가 현재 설정을 다시 읽어 확정하고 주문에 스냅샷으로 저장한다.
  - 직접 주문과 장바구니 주문 모두 `ACTIVE` 상품만 확정한다. 판매 중지 상품은 재고가 남아 있어도 `400 INVALID_INPUT`으로 거절한다.
  - 회원 장바구니는 `cartCheckout=true`를 지정하고 직전 `GET /api/v1/me/cart`의 `cartVersion`을 `expectedCartVersion`으로 보낸다. 서버는 버전이 일치할 때만 클라이언트의 `items` 대신 장바구니에서 구매 가능한 항목을 확정한다. 기존 `/api/v1` 클라이언트 호환을 위해 필드는 선택형이지만 현재 웹 클라이언트는 항상 전송한다.
  - `issuedCouponId`와 `rewardAmount`는 회원 `ORDER`에서만 사용할 수 있다. 쿠폰은 공개 발급으로 회원이 보유한 미사용 쿠폰 1장만 허용하고, 상품 합계가 최소 주문 금액 이상일 때 배송비를 제외한 상품 금액에서 할인한다. 적립금은 쿠폰 적용 뒤 상품 금액까지만 1P=1원으로 사용할 수 있어 배송비에는 적용되지 않는다.
  - prepare는 결제 시도와 같은 트랜잭션에서 발급 쿠폰 행을 배타 잠그고 쿠폰 정의 행을 공유 잠금 조회한 뒤 쿠폰과 적립금을 30분 동안 예약한다. 관리자 비활성화가 먼저 커밋되면 과거 조회 스냅샷이 있더라도 결제 견적을 `422 CHANGE_NOT_ALLOWED`로 거절하고, 서로 다른 회원의 같은 정의 결제 견적은 병렬로 처리한다. confirm 성공 시 쿠폰을 사용 완료하고 적립금을 차감하며, prepare 만료·PG 최종 거절·보상 환불 완료처럼 결제가 최종적으로 성립하지 않은 경우 예약을 멱등 해제한다. 결과가 불명확한 재시도·대사 상태에서는 중복 사용을 막기 위해 예약을 유지한다.
  - 비회원 경로(`HG_SESSION` 없음)는 payload에 `phone/verificationCode/name`이 모두 채워져 있어야 한다 (`PASS` 제외 — 8회권은 회원 전용).
  - 비회원 `ORDER`, `BOOKING` payload는 `policyAcceptance`에 현재 이용약관·개인정보처리방침 버전과 두 동의 여부를 함께 보낸다. 서버는 결제 시도와 같은 트랜잭션에서 유형·목적·서버 수락 시각을 저장한다. 회원 거래에는 이 필드를 요구하지 않는다.
  - 공개 `payload.type` 계약에는 `ORDER`, `BOOKING`, `PASS`만 존재한다. 서버 암호화 스냅샷의
    `PREPARED_ORDER`, `PREPARED_BOOKING`, `PREPARED_PASS` 식별자는 저장 JSON 호환을 위해 내부에서만 유지하며
    OpenAPI 요청 schema에는 노출하지 않는다.
  - OpenAPI의 `PreparePaymentRequest.payload`는 `OrderPayload`, `BookingPayload`, `PassPayload`를 구분하는 `oneOf`다. 공통 `PaymentPayload`는 `type` discriminator mapping만 가지며 subtype `allOf`와 순환하지 않는다.
  - `OrderPayload`의 필수 필드는 `type`, `items`, `cartCheckout`, `fulfillmentType`, `madeToOrderConsent`다. 각 `items` 항목의 `productId`, `qty`가 필수이며 직접입력이 없으면 `textInputs`를 생략하거나 빈 배열로 보낸다.
  - `BookingPayload`의 필수 필드는 `type`, `slotId`, `participantCount`다. `paymentMethod`는 일반 결제에서 사용하고 `passId`는 8회권 사용 예약에서 사용한다.
  - `PassPayload`의 필수 필드는 `type`, `userId`다.
  - `userId`, 비회원 인증 정보, `shippingAddress`, 주문제작 동의 버전, 정책 동의, `expectedCartVersion`, `issuedCouponId`, `rewardAmount`는 인증 주체와 결제 종류에 따라 조건부로 사용하므로 schema에서는 nullable 또는 optional로 유지하고 위 정책으로 검증한다.
  - prepare 응답의 `orderId`는 Toss 결제창에 그대로 전달한다.
  - 회원 응답의 `statusToken`은 `null`이다. 비회원 응답에는 30일 만료 HMAC 서명 토큰을 반환하며 프론트는 URL이 아닌 session storage에 보관한다. DB에는 서명 토큰 전체의 SHA-256 해시만 저장한다.

##### Payload 구조

```jsonc
// ORDER
{
  "type": "ORDER",
  "userId": 7,                  // 회원 경로
  "phone": "01012345678",      // 비회원 경로
  "verificationCode": "483921",
  "name": "홍길동",
  "items": [{ "productId": 1, "qty": 2 }],
  "cartCheckout": false,
  "issuedCouponId": 81,
  "rewardAmount": 5000,
  "madeToOrderConsentVersion": "2026-07-21-v1",
  "madeToOrderConsent": true,
  "policyAcceptance": {
    "termsVersion": "2026-08-08-v1",
    "termsAccepted": true,
    "privacyVersion": "2026-08-11-v2",
    "privacyAccepted": true
  },
  "fulfillmentType": "SHIPPING",
  "shippingAddress": {
    "recipientName": "홍길동",
    "phone": "01012345678",
    "postalCode": "06236",
    "addressLine1": "서울시 강남구 테헤란로 1",
    "addressLine2": "2층"
  }
}

// ORDER (회원 장바구니)
{
  "type": "ORDER",
  "userId": 7,
  "items": [],
  "cartCheckout": true,
  "expectedCartVersion": "f3029b77e4e6080fcb48f1ac6f15fa76c27d2c44f748c8f772b705bf7fe79c76",
  "madeToOrderConsentVersion": null,
  "madeToOrderConsent": false,
  "fulfillmentType": "PICKUP",
  "shippingAddress": null
}

// BOOKING (예약금 결제)
{
  "type": "BOOKING",
  "userId": 7,                  // 회원 경로
  "phone": "01012345678",      // 비회원 경로
  "verificationCode": "483921",
  "name": "홍길동",
  "slotId": 42,
  "paymentMethod": "CARD",     // CARD | EASY_PAY (BANK_TRANSFER 거절)
  "participantCount": 3,
  "policyAcceptance": {
    "termsVersion": "2026-08-08-v1",
    "termsAccepted": true,
    "privacyVersion": "2026-08-11-v2",
    "privacyAccepted": true
  }
}

// BOOKING (8회권 사용 예약 — 회원 전용, amount=0)
{
  "type": "BOOKING",
  "userId": 7,
  "slotId": 42,
  "passId": 9,
  "participantCount": 1
}

// PASS (8회권 구매 — 회원 전용)
{
  "type": "PASS",
  "userId": 7
}
```

- 8회권 사용 예약은 회원이 예약 가능 슬롯을 직접 선택해 한 회차씩 생성하며, 성공할 때마다 크레딧 1회를 차감한다.
- 일반 예약의 `participantCount`는 1명부터 선택한 슬롯의 남은 정원까지이며 슬롯 점유와 예약금·잔금에 함께 반영한다. 8회권 예약은 1만 허용한다. prepare는 결제 시도를 만들기 전에 슬롯·클래스 활성 상태, 시작 시각, 현재 정원과 양방향 수업·정리 구간의 예약 충돌을 확인하고, confirm 시에는 같은 범위를 잠근 뒤 최신 상태를 다시 확인한다.
- 신규 8회권 구매는 `REGULAR_CRAFT_8` 계획으로 확정한다. 8회권 예약 prepare는 현재 회원 소유권·만료·잔여 횟수를 확인하고, 클래스의 `passEligible=true`와 비향수 카테고리를 모두 충족할 때만 0원 결제 시도를 만든다. confirm에서는 이용권 행을 잠근 뒤 같은 조건을 다시 확인하고 크레딧을 차감한다.
- 운영자가 8회 일정을 일괄 배정하는 별도 API는 제공하지 않는다.

#### 2.15.2 결제 확정 (confirm)

```http
POST /api/v1/payments/confirm
Cookie: HG_SESSION={sessionToken}   # 회원 경로일 때만
X-Payment-Status-Token: {prepareStatusToken}  # 비회원 경로일 때만
Content-Type: application/json

{
  "paymentKey": "tviva20260422123456ABCDEF",
  "orderId": "f2d3a1b4-9d24-4f0a-8a8a-7c8b06f5b1a2",
  "amount": 78000
}
```

```json
{
  "context": "ORDER",
  "domainId": 12,
  "accessToken": "eyJ...signed-token",
  "accessRecoveryRequired": false,
  "receiptUrl": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=..."
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — prepare 시점 amount와 불일치, payload 인증 정보 불일치
  - `404 NOT_FOUND` — `payment_attempt` 미존재 또는 prepare 소유권 불일치
  - `409 INVENTORY_NOT_ENOUGH` — 결제 직전 재고 부족
  - `409 CAPACITY_EXCEEDED` — 결제 직전 슬롯 정원 초과
  - `409 DUPLICATE_BOOKING` — 동일 전화번호 + 동일 슬롯에 활성 예약 중복
  - `409 SLOT_NOT_AVAILABLE` — 결제 직전 비활성 슬롯
  - `409 PAYMENT_CONFIRM_IN_PROGRESS` — 동일 결제 confirm 처리 중
  - `409 PAYMENT_RECONCILIATION_REQUIRED` — PG 승인 여부가 불명확해 운영자 대사가 필요함. 새 결제를 시작하지 않음
  - `500 INTERNAL_ERROR` — 저장된 결제 payload 직렬화/역직렬화 실패
  - `502 PAYMENT_FAILED` — PG가 결제 확정을 최종 거절
  - `503 PAYMENT_CONFIRM_RETRYABLE` — 타임아웃·서킷 오픈·호출 대기열 포화처럼 같은 결제 정보로 재확인이 가능한 일시 실패
  - `410 PAYMENT_ATTEMPT_EXPIRED` — prepare 후 30분 동안 confirm을 시작하지 않아 만료됨
- 정책:
  - `paymentKey`는 amount > 0 결제만 필수다. 8회권 사용 예약처럼 `payment_attempt.amount=0`인 경우 `paymentKey`는 비워서 보내고 PG 호출은 생략된다.
  - `orderId`, `amount`는 모든 confirm 요청에서 필수다. 0원 결제도 `amount=0`을 명시한다.
  - 서버는 `payment_attempt.amount`와 요청 `amount`가 일치하지 않으면 `400 INVALID_INPUT`으로 거절한다.
  - 서버는 `PENDING/RETRYABLE -> PROCESSING`을 새 processing token과 함께 짧은 트랜잭션으로 선점한 뒤 DB 트랜잭션 밖에서 PG `confirm`을 호출한다. stale 재선점 뒤 이전 token의 실패 결과는 상태에 반영하지 않지만, 늦게 도착한 PG 성공은 같은 요청임을 재검증한 뒤 `APPROVED`로 화해한다.
  - Toss `Idempotency-Key`는 prepare에서 생성한 `orderId`를 사용하며 같은 결제 재시도에서 변경하지 않는다.
  - 브라우저 기본 결제는 Toss `CARD` 통합창이며, 네이버페이·카카오페이를 선택하면 같은 SDK의 해당 간편결제 자체창을 연다. 이 선택은 prepare/confirm 요청 계약을 바꾸지 않는다. 예약의 최종 `paymentMethod`는 prepare 화면값이 아니라 PG 승인·조회 응답의 실제 `method`로 저장한다.
  - Toss 승인 응답의 `paymentKey`, `orderId`는 confirm 요청값과 모두 같아야 한다. 다르면 성공으로 저장하지 않고 같은 멱등키로 재확인 가능한 실패로 처리한다.
  - PG 성공은 별도 트랜잭션으로 `APPROVED`에 저장하고, 이후 도메인 저장과 `CONFIRMED` 전이는 한 트랜잭션으로 처리한다.
  - 비회원 주문·예약 fulfillment는 내부 proof의 HMAC을 현재 또는 이전 게스트 토큰 키로 검증하고, proof의 context·orderId·정규화 전화번호가 현재 `PaymentAttempt` 및 저장 payload와 모두 일치할 때만 Guest와 도메인을 생성한다. 원 인증 코드가 prepare 뒤 만료되어도 이미 소비된 결제 귀속 증거는 해당 결제 시도에서 유효하다.
  - 이미 `CONFIRMED`인 결제를 같은 인증 주체·금액·paymentKey로 재호출하면 PG와 도메인 생성을 반복하지 않고 최초 `context`, `domainId`, `accessToken`을 그대로 반환한다.
  - 성공 화면은 URL의 동일한 `paymentKey`, `orderId`, `amount`를 유지하고 `PAYMENT_CONFIRM_IN_PROGRESS`, `PAYMENT_CONFIRM_RETRYABLE`, 네트워크 오류 또는 필수 인프라 일시 장애에만 명시적 재확인을 제공한다. `PAYMENT_FAILED`와 `PAYMENT_RECONCILIATION_REQUIRED`처럼 최종 또는 운영자 확인이 필요한 상태에는 재확인을 제공하지 않는다.
  - 결제 실패 화면은 provider query의 원문 `message`를 표시하지 않는다. 허용 목록에 있는 `code`만 고정된 한국어 안내로 변환하고, 화면 진입 직후 query를 브라우저 주소에서 제거한다.
  - 결제창 인증 취소·실패 후 구매 화면 복귀는 고객 세션 귀속 `hg_payment_return_hint`의 `returnPath`, prepare에서 받은 `orderId`를 사용한다. 콜백에 주문번호가 없어도 저장된 ID로 결제 종료 후 복귀한다. 종료 실패 시 화면에 머물러 현재 상태를 조회하며 조회 자격은 보존한다. SDK가 오류를 반환해도 같은 종료를 시도한다. 로그인 복귀와 같은 내부 주소 확인을 사용하며 고객 세션이 다르거나 저장 정보가 없으면 복귀 버튼을 표시하지 않는다. 결제 준비·승인 API는 자동 호출하지 않는다.
  - PG 최종 거절은 `FAILED`, 타임아웃·서킷 오픈 같은 일시 실패는 `RETRYABLE`로 저장한다. `FAILED`로 종결된 결제의 동일 confirm 재호출은 PG를 다시 호출하지 않고 저장된 실패 사유의 `502 PAYMENT_FAILED`를 반환한다.
  - PG 승인 후 도메인 저장이 실패하면 `paymentAttemptId` 기반 보상 환불을 요청하고 기존 환불 자동·수동 복구 경로로 처리한다. amount=0 내부 승인 실패는 외부 결제가 없으므로 보상 환불을 만들지 않는다.
  - PG 승인 상태 또는 보상 환불 요청 저장까지 실패해 `PROCESSING`·`RETRYABLE`·`APPROVED`가 1분 이상 남으면, 서버 배치가 매분 최대 10건을 자동 재개한다. `PROCESSING/RETRYABLE`은 저장된 요청과 같은 `orderId` 멱등키로 PG confirm을 재확인하고, `APPROVED`는 PG 호출 없이 fulfillment를 재개한다. 마지막 복구 시각을 저장해 건별 1분 backoff와 후보 순환을 적용한다. 생성 후 14일이 지난 유료 미확정 PG 호출은 자동·사용자 재승인 모두 막고 `RECONCILIATION_REQUIRED`로 격리하며, PG를 호출하지 않는 0원 결제는 기간과 무관하게 내부 처리를 재개한다. 내부 복구는 저장 payload의 결제 주체를 사용하는 전용 명령으로만 인증 검증을 우회한다. 공개 confirm은 회원 세션 소유자 또는 비회원 `X-Payment-Status-Token`이 prepare 소유권과 일치해야 한다.
  - confirm 요청 `paymentKey`는 `payment_attempt.payment_key`, PG 승인 응답의 `paymentKey`는 `payment_attempt.confirmed_payment_key`와 생성된 도메인 레코드의 `payment_key`에 저장한다. 이후 환불은 승인 응답의 `paymentKey`를 PG cancel 호출의 원결제 식별자로 사용한다.
  - 환불 이력은 원결제 식별자인 Toss `paymentKey`를 `refunds.payment_key`, 환불 거래 식별자인 Toss cancel `transactionKey`를 `refunds.refund_transaction_key`에 분리해 저장한다. PG port의 성공 결과와 도메인의 `SUCCEEDED` 전이는 공백이 아닌 `transactionKey`를 필수로 검증하고, 잘못된 성공 응답은 성공으로 저장하지 않고 대사 대상으로 격리한다. 자동·수동 재처리는 `refunds.payment_key`와 최초 `idempotency_key`를 다시 사용한다.
  - 비회원 경로의 `accessToken`은 HMAC-SHA256 서명과 기본 30일 만료 시각을 포함한다. 주문·예약에는 서명 토큰 전체의 SHA-256 해시만 저장하며, 서명 없는 토큰은 허용하지 않는다. 응답 유실 뒤 동일 confirm 재호출을 위해 원문 토큰은 `payment_attempt`에 AES-GCM 암호문으로 저장한다. 재호출 시에도 토큰 서명·만료와 현재 주문·예약의 비회원 소유권·저장 해시를 다시 확인한다. 이미 회원에게 귀속됐거나 토큰이 교체·만료된 경우 `accessToken=null`, `accessRecoveryRequired=true`를 반환한다. 회원 경로는 두 값이 각각 `null`, `false`다.
  - `domainId`는 context에 따라 `orderId`(`ORDER`), `bookingId`(`BOOKING`), `passId`(`PASS`)다.
  - 유료 결제가 완료되면 Toss 승인 응답의 `receipt.url`을 `receiptUrl`로 반환한다. 0원 결제이거나 PG가 영수증 URL을 제공하지 않으면 `null`이다.
  - 내부 결제 귀속 증거가 없거나 위조·재사용되어 현재 결제 시도와 일치하지 않으면 fulfillment를 중단하고 `400 INVALID_INPUT`으로 처리한다. PG가 이미 승인됐다면 기존 보상 환불 경계를 따른다.
  - confirm은 행 잠금 아래 30분 유효시간을 다시 확인하고, 경계를 넘긴 `PENDING`을 `CANCELED`로 전이한 뒤 `payload_enc`를 제거하고 `410 PAYMENT_ATTEMPT_EXPIRED`를 반환한다. 매분 배치도 같은 기준으로 confirm을 시작하지 않은 결제를 일괄 정리한다. 만료된 orderId는 새 prepare부터 다시 시작해야 한다.
  - prepare·confirm 성공 응답과 고객 상태 조회 응답은 `Cache-Control: no-store`로 반환한다.
  - 최종 상태인 결제 시도는 생성 30일 뒤 `payload_enc`, `fulfilled_access_token_enc`, `owner_phone_hmac`과 `status_access_token_hash`를 제거한다. 이후 같은 orderId로 confirm 결과를 재요청하면 `410 PAYMENT_RESULT_RETENTION_EXPIRED`를 반환한다. 결제 감사 필드와 복구·대사·보상 진행 상태는 유지한다.

#### 2.15.3 고객 결제 상태 조회

```http
GET /api/v1/payments/{orderId}
Cookie: HG_SESSION={sessionToken}                 # 회원
X-Payment-Status-Token: {prepareStatusToken}      # 비회원
```

```json
{
  "context": "ORDER",
  "amount": 78000,
  "status": "REFUNDING",
  "domainId": null,
  "accessToken": null,
  "accessRecoveryRequired": false,
  "receiptUrl": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=..."
}
```

- 회원은 prepare 당시 저장한 `owner_user_id`와 현재 세션 사용자 ID가 같아야 한다.
- 비회원은 prepare 응답 또는 SMS 결제 상태 복구에서 받은 서명 토큰을 헤더로 보내며, 서명·만료 검증 뒤 토큰 전체 해시가 저장값과 같아야 한다. `orderId`만으로는 조회하지 않는다.
- 결제 미존재와 소유권 불일치는 모두 `404 NOT_FOUND`로 응답해 결제 존재 여부를 노출하지 않는다.
- 고객 상태는 `READY`, `CONFIRMING`, `RETRYABLE`, `COMPLETED`, `FAILED`, `REVIEW_REQUIRED`, `REFUNDING`, `REFUNDED`, `SUPPORT_REQUIRED`, `EXPIRED`다.
- `COMPLETED`만 `domainId`를 반환한다. 비회원 완료 결제는 응답 유실 복구를 위해 현재 주문·예약의 비회원 소유권과 저장 해시까지 일치하는 유효한 `accessToken`만 반환한다. 토큰을 안전하게 복원할 수 없으면 `accessRecoveryRequired=true`로 휴대폰 인증 복구가 필요함을 알린다.
- PG 승인 시 저장한 영수증 URL이 있으면 `receiptUrl`을 반환한다. 결제 소유권 검증에 성공한 고객만 조회할 수 있으며 URL을 서버에서 재구성하지 않는다.
- 실패 사유, `refundId`, PG 식별자, 재시도 횟수는 고객 응답에 포함하지 않는다.
- 모든 응답은 `Cache-Control: no-store`로 반환한다.

#### 승인 전 결제 종료

```http
POST /api/v1/payments/{orderId}/abandon
Cookie: HG_SESSION={sessionToken}                 # 회원
X-Payment-Status-Token: {prepareStatusToken}      # 비회원
```

- `operationId`: `abandonPayment`, 요청 본문 없음, 성공 `204 No Content`.
- 결제 상태 조회와 같은 소유권 검증을 적용한다. 미존재·다른 소유자는 `404 NOT_FOUND`다.
- confirm과 같은 결제 행 잠금 아래 `PENDING`만 `CANCELED`로 종료하고 payload 제거와 쿠폰·적립금 예약 해제를 한 트랜잭션으로 처리한다. PG 승인·환불은 호출하지 않는다.
- 이미 `CANCELED`이면 `204`를 재응답한다. 나머지 상태는 `409 CONFLICT`로 변경 없이 거절한다.
- 종료한 결제의 confirm은 기존 `410 PAYMENT_ATTEMPT_EXPIRED`, 상태 조회는 `EXPIRED`를 사용한다. 고객 화면은 만료·직접 종료를 함께 뜻하는 ‘결제 준비가 종료되었습니다’로 안내한다.
- 브라우저 자체 종료·요청 실패 시 기존 30분 만료 배치를 유지한다. 비회원 조회 토큰은 보존하며 CSRF와 `Cache-Control: no-store`를 적용한다.

#### 2.15.4 비회원 결제 상태 조회 권한 복구

```http
POST /api/v1/guest-records/payment-status-recovery
Content-Type: application/json

{
  "phone": "01012345678",
  "verificationCode": "123456"
}
```

```json
{
  "statusToken": "signed-payment-status-token",
  "expiresAt": "2026-08-20T00:00:00Z",
  "payments": [
    {
      "orderId": "f2d3a1b4-9d24-4f0a-8a8a-7c8b06f5b1a2",
      "context": "ORDER",
      "amount": 78000,
      "status": "REFUNDING"
    }
  ]
}
```

- `operationId`: `recoverGuestPaymentStatuses`
- SMS 인증 코드를 검증·소모한 뒤 같은 휴대폰 소유의 생성 30일 이내 최종 결제와 미종결 결제를 함께 복구한다. `orderId`를 요청에 요구하지 않으므로 브라우저 탭 저장소 전체 유실도 복구할 수 있다.
- 서버는 대상 결제를 ID 순서로 잠그고 공통 새 `statusToken` 해시로 교체한다. 응답 뒤에는 이전 prepare·복구 토큰이 모두 즉시 무효다.
- `expiresAt`은 공통 토큰 자체의 만료 시각이다. 개별 최종 결제는 생성 30일 보존 경계가 먼저 오면 해당 행의 조회 토큰 해시가 제거되어 더 일찍 조회가 끝날 수 있다.
- 결제 미존재와 휴대폰 소유 불일치는 모두 `404 NOT_FOUND`로 응답하며, SMS 코드는 두 경우 모두 먼저 소모한다.
- `payments[].status`는 고객 결제 상태 조회와 같은 enum을 사용한다. 실패 원인, PG 식별자와 환불 내부 ID는 반환하지 않는다.
- IP·휴대폰별 복구 처리율 제한을 적용하고 성공 응답은 `Cache-Control: no-store`로 반환한다.

#### 2.15.5 8회권 결제 정책 조회

```http
GET /api/v1/payments/pass-policy
```

```json
{ "totalPrice": 240000, "totalCredits": 8, "validityDays": 90 }
```

- 프론트는 결제 전에 서버 설정 가격, 이용 횟수와 기간을 표시한다.
- 표시값을 결제 요청 금액으로 신뢰하지 않는다. 최종 금액은 prepare가 현재 서버 설정으로 다시 확정한다.

#### 2.15.6 Toss 결제 상태 웹훅

```http
POST /api/v1/webhooks/toss-payments
tosspayments-webhook-transmission-id: {전송 식별자}
Content-Type: application/json

{
  "eventType": "PAYMENT_STATUS_CHANGED",
  "data": { "orderId": "pay_20260501_0001" }
}
```

- `operationId`: `receiveTossPaymentWebhook`
- 성공과 이미 수신한 전송: `200 OK`
- `PAYMENT_STATUS_CHANGED`인 알려진 `orderId`만 전송 식별자 유일키로 저장한다.
- 웹훅 본문만으로 결제를 확정하지 않는다. 배치가 저장된 결제 시도 ID로 기존 PG 조회 대사를 실행한다.
- Toss가 같은 전송 식별자를 재전송해도 대사 요청은 한 건만 유지한다.

---

### 2.16 클라이언트 모니터링 API

프론트 전환 퍼널과 비회원 -> 회원 전환 CTA를 best-effort 로그로 남기는 API다.

```http
POST /api/v1/monitoring/client-events
Content-Type: application/json

{
  "event": "GUEST_LOOKUP_ENTRY",
  "path": "/guest",
  "source": "home_lookup_panel",
  "target": "guest_orders"
}
```

- 성공: `204 No Content`
- 정책:
  - 인증은 선택이다. `HG_SESSION`이 있으면 `userId`를 함께 기록한다.
  - `path`는 필수이며 최대 120자다.
  - `source`, `target`은 선택이며 최대 80자다.
  - 클라이언트가 보내는 `path`, `source`, `target` 원문은 로그에 남기지 않는다. 로그와 메트릭에는 서버가 정의한 `event`, 인증 여부와 내부 ID만 사용한다.
  - 모니터링 실패는 사용자 핵심 플로우를 막지 않는 best-effort 성격으로 다룬다.

### 2.17 local/dev 전용 Dev API

인증 코드 조회는 `local`, `dev`, 환불 실패 재현은 `local` 프로필에서만 등록되는 관리자 dev API다. 운영 프로필에서는 빈이 등록되지 않는다.

#### 2.17.1 환불 실패 재현 훅

- `POST /api/v1/admin/dev/payment/refunds/fail-next`
  - 요청: `{ "reason": "로컬 smoke 강제 환불 실패" }` (본문 생략 가능)
  - 응답: `{ "status": "ARMED", "reason": "..." }`
- `DELETE /api/v1/admin/dev/payment/refunds/fail-next`
  - 응답: `204 No Content`

정책:
- 관리자 Bearer 인증을 통과해야 한다.
- 다음 PG 환불 1건만 실패시키고, 실패 사유는 재시도 검증에 사용한다.

#### 2.17.2 최근 이메일 인증 코드 조회

```http
GET /api/v1/admin/dev/email-verifications/latest?userId=7&email=naver-member@example.com
Authorization: Bearer {token}
```

```json
{ "code": "483921" }
```

- 성공: `200 OK`
- 발송 성공이 기록된 미소모 코드가 없음: `404 Not Found`
- 정책:
  - `local`, `dev` 프로필에서만 등록되며 운영에는 노출하지 않는다. 관리자 Bearer 또는 해당 환경에서 명시적으로 활성화한 local API key 인증을 요구한다.
  - 회원 ID와 정규화 이메일이 모두 일치하는 가장 최근 코드를 반환한다.
  - 가짜 발송기는 이메일·코드 원문을 로그에 남기지 않는다.

### 2.18 비회원 조회 정보 복구

```http
POST /api/v1/guest-records/recovery
Content-Type: application/json

{ "phone": "01012345678", "verificationCode": "483921" }
```

```json
{
  "accessToken": "signed-token",
  "expiresAt": "2026-07-22T10:00:00Z",
  "orders": [
    { "orderId": 12, "status": "PAID_APPROVAL_PENDING", "totalAmount": 39000, "createdAt": "2026-07-20T01:00:00Z" }
  ],
  "bookings": [
    { "bookingId": 21, "status": "BOOKED", "className": "도자기 정규반", "startAt": "2026-07-25T10:00:00", "endAt": "2026-07-25T12:00:00" }
  ]
}
```

- 기존 인증 코드 발송 API로 SMS 소유 확인을 시작한다. 성공 시 인증 코드를 한 번 소비하고 같은 비회원의 모든 주문·예약에 새 복구 토큰 해시를 저장한다.
- 복구 토큰 기본 수명은 24시간이다. 응답에 포함된 모든 대상에 같은 `X-Access-Token`을 사용하며 교체 직후 이전 토큰은 무효다.
- 응답의 `accessToken`, `expiresAt`, `orders`, `bookings`와 각 주문·예약 요약 필드는 항상 존재한다. 대상이 없으면 목록을 생략하지 않고 빈 배열로 반환하며, 기존 응답 배열은 유형별 최신 100건으로 제한한다.
- 전체 복구 이력은 같은 토큰을 `X-Access-Token`으로 보내 `GET /api/v1/guest-records/recovery/orders?cursor={cursor}&size=20`와 `GET /api/v1/guest-records/recovery/bookings?cursor={cursor}&size=20`에서 `{content,nextCursor,hasMore}`로 조회한다. `size`는 1~100이고 `(createdAt,id)` 내림차순 커서를 사용한다.
- 토큰 교체는 주문·예약 엔티티를 전부 로드하지 않고 guest ID 기준 bulk update로 수행하며 낙관적 락 버전도 함께 증가시킨다. 하나의 복구 토큰은 같은 비회원의 여러 이력에 공유되므로 DB는 토큰을 UNIQUE로 제한하지 않고 `(access_token,created_at,id)` 조회 인덱스를 둔다.
- 프론트는 복구 결과와 토큰을 만료 시각까지만 현재 브라우저 탭의 `sessionStorage`에 보관하고 현재 고객 세션 경계에 결합한다. 저장 도중 로그인·로그아웃·계정 전환으로 경계가 바뀌면 방금 저장한 값과 일치할 때만 compare-and-delete로 제거하며 메모리의 복구 화면도 폐기한다. 주문·예약 ID는 URL 쿼리로 전달하고 토큰은 URL에 넣지 않아, 같은 고객 세션의 목록 이동과 새로고침 뒤에만 복구 세션을 이어간다.
- 같은 전화번호의 비회원이 없어도 존재 여부 오류 대신 새 토큰과 빈 목록을 반환한다.
- IP와 전화번호별 처리율 제한은 Redis 장애 시 fail-closed로 동작한다.

### 2.19 공방 프로필·이미지 미디어

#### 2.19.1 공방 프로필

- `GET /api/v1/workshop` — 인증 없이 공방 안내 조회
- `GET /api/v1/admin/workshop` — 관리자 조회
- `PUT /api/v1/admin/workshop` — 관리자 전체 갱신

```json
{
  "name": "해피갤러리",
  "phone": "010-9635-5608",
  "postalCode": null,
  "addressLine1": "충북 충주시 계명대로 161",
  "addressLine2": "1층",
  "businessHours": null,
  "mapUrl": "https://m.place.naver.com/place/21668321",
  "parkingInfo": null,
  "businessRegistrationNumber": "303-11-87052",
  "representativeName": "홍지현",
  "email": "ssi1972@naver.com",
  "mailOrderRegistrationNumber": "2011-충북 충주-127",
  "introduction": "해피갤러리는 빈티지 가죽공예, 레진아트, 플루이드아트, 톨페인팅, 냅킨아트, 양말목공예, 하바리움, 위빙, POP 원데이클래스부터 자격증반, 창업반을 운영합니다.",
  "kakaoTalkId": "ssim1972",
  "naverTalkUrl": "https://talk.naver.com/w4xufy",
  "naverBlogUrl": "https://blog.naver.com/ssim1972",
  "instagramUrl": "https://www.instagram.com/happygallery_by/",
  "smartStoreUrl": "https://smartstore.naver.com/happygallery",
  "updatedAt": "2026-07-21T10:00:00",
  "version": 0
}
```

- `name`은 필수이고 나머지 안내 필드는 선택값이다. `businessRegistrationNumber`는 값이 있으면 `000-00-00000` 형식이고, `email`은 표준 이메일 형식과 254자 상한을 적용해 소문자로 저장한다. `mapUrl`, `naverTalkUrl`, `naverBlogUrl`, `instagramUrl`, `smartStoreUrl`은 값이 있으면 500자 이하의 HTTP(S) 주소여야 한다. 공개·관리자 응답은 같은 구조를 사용한다.
- 관리자 전체 갱신 요청은 조회 응답의 `version`을 필수 `expectedVersion`으로 함께 보낸다.
  서비스가 현재 버전과 먼저 비교하고, 비교 직후의 경쟁은 JPA 낙관적 잠금이 최종 차단한다.
  두 충돌 모두 `409 CONFLICT`를 반환한다.
- 기존 네이버톡톡 사용 여부 불리언 필드는 제거하고 `naverTalkUrl`로 대체한다. 네이버톡톡 문의 제공 여부는 `naverTalkUrl` 값의 존재로 판단하며, 클라이언트는 응답 URL을 그대로 링크에 사용한다.
- 기준 프로필은 제공된 대표자명, 전자우편주소와 통신판매업 신고번호를 저장한다. `prod`에서는 이 값들과 연락처·주소·사업자등록번호가 모두 입력되기 전 결제 prepare를 `503 SERVICE_UNAVAILABLE`로 차단한다.
- 관리자 공방 주소와 주문 배송지는 아래 도로명주소 검색 결과의 `postalCode`, `roadAddress`를 적용하거나 직접 입력할 수 있다.

```http
GET /api/v1/addresses/search?keyword=계명대로%20161
```

```json
[
  {
    "postalCode": "27360",
    "roadAddress": "충청북도 충주시 계명대로 161",
    "jibunAddress": "충청북도 충주시 연수동 1615",
    "buildingName": "해피갤러리"
  }
]
```

- `operationId`: `searchRoadAddresses`
- 인증 없이 조회하며 `keyword`는 2~100자다. 최대 10건을 반환한다.
- 연동 비활성·외부 장애: `503 SERVICE_UNAVAILABLE`. 프런트는 기존 직접 입력을 유지한다.
- 승인키는 백엔드에만 저장하고 브라우저가 공식 주소 API를 직접 호출하지 않는다.

#### 2.19.2 이미지 업로드·조회

```http
POST /api/v1/admin/media/images
Authorization: Bearer {token}
Content-Type: multipart/form-data

file={JPEG|PNG|WebP binary}
```

```json
{
  "fileName": "21ad89d4-73ca-43af-a11e-d7953851acb0.jpg",
  "url": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg"
}
```

- 비어 있지 않은 JPEG, PNG, WebP 파일만 허용하며 요청 MIME과 파일 시그니처를 함께 확인한다. 파일 상한은 5MiB다.
- UUID 파일명으로 원자 저장하고 상품·클래스 `imageUrl`에는 반환된 경로를 사용한다.
- `GET /api/v1/admin/media/images/{fileName}`은 계정 기반 Bearer 관리자 인증 뒤 업로드 직후 아직 참조되지 않은 파일과 비활성·미게시 자산의 파일을 미리보기 위해 실제 이미지 MIME으로 반환한다. local API key는 후기 사진 보호 경계를 우회하지 못하게 `403 FORBIDDEN`이고, 응답은 `Cache-Control: no-store`다.
- `GET /api/v1/media/images/{fileName}`은 현재 `ACTIVE` 상품·클래스, 게시 상태이고 종료 전인 이벤트 또는 공개 후기에서 참조하는 파일만 인증 없이 실제 이미지 MIME으로 반환한다. 기존 로컬 저장값에 query나 fragment가 남아 있어도 URI path가 같으면 같은 공개 참조로 인정한다. 참조가 없거나 비활성 상품·클래스, 미게시·종료 이벤트, 숨김·삭제 후기, 보존 증거에만 남은 파일은 `404 NOT_FOUND`이며 응답은 `Cache-Control: no-store`다.
- 허용된 UUID 파일명 형식이 아니거나 파일이 없으면 `404 NOT_FOUND`다.

### 2.20 이벤트·쿠폰·적립금 API

#### 2.20.1 공개 이벤트

- `GET /api/v1/events` — 현재 게시된 이벤트 목록
- `GET /api/v1/events/{id}` — 현재 게시된 이벤트 상세

```json
{
  "id": 1,
  "title": "여름 공방전",
  "summary": "여름 작품과 회원 혜택을 만나는 행사",
  "content": "행사 기간과 관련 작품을 확인해 주세요.",
  "imageUrl": "/api/v1/media/images/11111111-1111-4111-8111-111111111111.jpg",
  "startAt": "2026-08-01T00:00:00",
  "endAt": "2026-08-31T23:59:00",
  "published": true,
  "featured": true,
  "couponDefinitionId": 10,
  "relatedProductIds": [1, 2],
  "version": 2
}
```

- 공개 목록은 `published=true`이고 `[startAt, endAt)` 경계에서 아직 끝나지 않은 현재·예정 이벤트를 반환한다. 진행 중 이벤트를 먼저, 예정 이벤트를 다음에 두고 각 그룹은 시작 시각과 ID 오름차순으로 안정 정렬한다. 홈은 이 중 `featured=true`인 이벤트를 노출한다.
- 이벤트 노출 경계는 서버 `Clock` 기준이며 공개 응답은 예약 게시 변경을 즉시 반영하도록 `Cache-Control: no-store`다.
- 상세에서 미게시·종료 이벤트는 존재 여부를 구분하지 않고 `404 NOT_FOUND`다. 게시된 시작 전 이벤트는 사전 안내를 위해 조회할 수 있다.
- `couponDefinitionId`는 관리자가 연결한 쿠폰 정의 ID이며 연결하지 않으면 `null`이다. 이벤트 상세의 회원은 이 ID로 기존 `POST /api/v1/me/coupons` 발급 계약을 사용하고, 서버가 쿠폰 활성·공개 발급·기간과 회원당 한 번 조건을 다시 검증한다.

#### 2.20.2 관리자 이벤트

- `GET /api/v1/admin/events`, `GET /api/v1/admin/events/{id}` — 게시 여부와 무관한 전체 조회
- `POST /api/v1/admin/events` — 이벤트 생성
- `PUT /api/v1/admin/events/{id}` — `expectedVersion`을 포함한 전체 수정
- `DELETE /api/v1/admin/events/{id}?expectedVersion={version}` — 낙관적 잠금 버전 확인 뒤 삭제

생성·수정 바디는 `title`, `summary`, `content`, nullable `imageUrl`, `startAt`, `endAt`, `published`, `featured`, nullable `couponDefinitionId`, nullable `relatedProductIds`를 사용한다. 수정은 `expectedVersion`을 추가한다. 기간은 `startAt < endAt`이어야 하며 관련 상품과 연결 쿠폰 ID는 실제 리소스만 허용한다. 이벤트에는 쿠폰을 최대 한 개 연결하며, 연결 쿠폰이 없으면 `couponDefinitionId=null`이다. 버전 충돌은 `409 CONFLICT`다.

#### 2.20.3 회원 쿠폰

- `GET /api/v1/me/coupons/claimable` — 현재 공개 발급 가능하고 이 회원이 아직 발급받지 않은 쿠폰 정의를 최신순 최대 100개 조회
- `GET /api/v1/me/coupons` — 회원에게 발급된 쿠폰 최근 100개
- `POST /api/v1/me/coupons` + `{ "definitionId": 10 }` — 공개 쿠폰 1장 발급

```json
{
  "id": 81,
  "definitionId": 10,
  "name": "여름 10% 할인",
  "discountType": "PERCENT",
  "discountValue": 10,
  "minOrderAmount": 30000,
  "maxDiscountAmount": 10000,
  "validFrom": "2026-08-01T00:00:00",
  "validUntil": "2026-08-31T23:59:00",
  "status": "AVAILABLE",
  "claimedAt": "2026-08-08T12:00:00",
  "reservedAt": null,
  "usedAt": null
}
```

- 한 쿠폰 정의는 회원당 한 번만 발급한다. 상태는 `AVAILABLE|RESERVED|REDEEMED|EXPIRED|CANCELED`다.
- `FIXED`는 고정 원화 할인이고 `maxDiscountAmount=null`이다. `PERCENT`는 1~100%이며 양수 `maxDiscountAmount`를 반드시 둔다. 주문 상품액에 정률을 적용해 원 미만을 버린 결과가 0원이면 `422 CHANGE_NOT_ALLOWED`로 거절한다.
- 관리자가 정의를 비활성화한 뒤 아직 예약·사용하지 않은 발급 쿠폰은 회원 조회 시 `CANCELED`로 정리해 화면의 사용 가능 표시와 prepare 검증을 일치시킨다.
- 발급 기간·활성·공개 발급 조건을 만족하지 않으면 `422 CHANGE_NOT_ALLOWED`, 중복 발급은 `409 CONFLICT`다.

#### 2.20.4 회원 적립금

- `GET /api/v1/me/rewards` — 현재 잔액과 최근 원장 100건 조회

```json
{
  "availableBalance": 1200,
  "reservedBalance": 300,
  "debtBalance": 0,
  "history": [
    {
      "id": 30,
      "type": "EARN",
      "amount": 1500,
      "availableAfter": 1500,
      "reservedAfter": 0,
      "debtAfter": 0,
      "orderId": 20,
      "createdAt": "2026-08-08T12:00:00"
    }
  ]
}
```

- 1P는 주문에서 1원으로 사용한다. `availableBalance`는 즉시 사용 가능, `reservedBalance`는 결제 prepare 중 예약, `debtBalance`는 이미 쓴 적립금을 환불로 회수할 때 잔액이 부족해 이후 적립에서 먼저 상계할 금액이다.
- 원장 유형은 `EARN|RESERVE|RELEASE|USE|RESTORE|EXPIRE|REVOKE|ADJUST`다. 만료·예약·사용·복원·회수는 원장과 잔액을 같은 트랜잭션으로 갱신한다.
- 배송 완료 또는 픽업 완료 시 배송비·쿠폰·사용 적립금을 제외한 상품 순결제액의 1%를 원 미만 버림으로 한 번 적립하고, 적립분은 1년 뒤 만료한다.

#### 2.20.5 관리자 쿠폰

- `GET /api/v1/admin/coupons`, `GET /api/v1/admin/coupons/{id}` — 쿠폰 정의 조회
- `POST /api/v1/admin/coupons` — 쿠폰 정의 생성
- `PUT /api/v1/admin/coupons/{id}` — `expectedVersion`을 포함한 전체 수정
- `DELETE /api/v1/admin/coupons/{id}?expectedVersion={version}` — 신규 발급만 막도록 비활성화

생성·수정 바디는 `name`, `discountType`, `discountValue`, `minOrderAmount`, nullable `maxDiscountAmount`, `validFrom`, `validUntil`, `active`, `publiclyClaimable`을 사용한다. 한 장이라도 발급된 뒤에는 기존 권리와 표시를 소급 변경하지 않도록 이름·할인 조건·유효기간을 바꿀 수 없고 `active`, `publiclyClaimable`만 변경할 수 있다. 이미 발급된 쿠폰은 정의를 비활성화해도 감사 이력과 주문 스냅샷을 유지한다. 버전 충돌은 `409 CONFLICT`, 발급 뒤 경제 조건 변경은 `409 COUPON_TERMS_IMMUTABLE`로 구분한다.

---

## 3. API 에러 계약

### 3.1 에러 응답 포맷

```json
{
  "code": "ALREADY_REFUNDED",
  "message": "이미 환불된 건입니다.",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

- `requestId`는 선택 필드다.
- HTTP 요청은 클라이언트 `X-Request-Id`가 UUID 또는 최대 64자의 안전한 ASCII 토큰이면 그대로
  내려주고, 없거나 형식이 안전하지 않으면 `RequestIdFilter`가 새 UUID를 생성한다.
  배치 실행 오류는 `batch-*` 형식 `requestId`를 사용한다.

### 3.2 HTTP 상태코드 × 에러 코드 목록

| HTTP | 에러 코드 | 발생 상황 |
|------|----------|----------|
| 400 | `INVALID_INPUT` | 요청 바디/파라미터 검증 실패 또는 요청 JSON 형식 오류 |
| 400 | `PHONE_VERIFICATION_FAILED` | 인증 코드 불일치 또는 만료 |
| 400 | `EMAIL_VERIFICATION_FAILED` | 이메일 인증 코드가 불일치·만료·미발송 상태이거나 이미 사용됨 |
| 400 | `PASSWORD_RESET_FAILED` | 비밀번호 재설정의 계정·전화번호·인증코드 확인 실패 |
| 401 | `UNAUTHORIZED` | 보호된 API에 유효한 관리자 또는 회원 인증 없이 접근 |
| 401 | `INVALID_CREDENTIALS` | 로그인 자격 증명 또는 현재 비밀번호 불일치 |
| 403 | `FORBIDDEN` | 인증은 됐지만 요청 권한이 없거나 CSRF 토큰이 없거나 일치하지 않음 |
| 403 | `REAUTHENTICATION_REQUIRED` | 민감한 계정 변경 전에 필요한 최근 본인 확인이 없거나 만료됨 |
| 404 | `NOT_FOUND` | 주문·예약·이용권·상품 미존재 |
| 405 | `METHOD_NOT_ALLOWED` | 존재하는 경로에 허용되지 않은 HTTP 메서드 요청 |
| 406 | `NOT_ACCEPTABLE` | 요청한 응답 미디어 타입을 제공할 수 없음 |
| 409 | `ALREADY_REFUNDED` | 이미 환불된 주문에 승인·거절 시도 |
| 409 | `INVENTORY_NOT_ENOUGH` | 재고 차감 시 수량 부족 |
| 409 | `CAPACITY_EXCEEDED` | 클래스별 슬롯 정원 초과 예약 시도 |
| 409 | `DUPLICATE_BOOKING` | 동일 예약자 + 동일 슬롯 활성 예약 중복 |
| 409 | `SLOT_NOT_AVAILABLE` | 비활성 슬롯 예약 시도 |
| 409 | `BOOKING_CONFLICT` | 낙관적 락 충돌에 의한 동시 변경 요청 |
| 409 | `CART_SNAPSHOT_CHANGED` | 장바구니 결제의 `expectedCartVersion`과 현재 장바구니 스냅샷이 다름 |
| 409 | `PAYMENT_CONFIRM_IN_PROGRESS` | 동일 결제의 confirm 요청이 이미 처리 중 |
| 409 | `PAYMENT_RECONCILIATION_REQUIRED` | PG 승인 여부가 불명확해 운영자 확인이 필요하며 새 결제를 시작하면 안 됨 |
| 409 | `COUPON_TERMS_IMMUTABLE` | 한 장 이상 발급된 쿠폰의 이름·할인 조건·유효기간 변경 시도 |
| 409 | `REVIEW_ALREADY_EXISTS` | 같은 주문 품목 또는 예약에 활성 후기 중복 작성 |
| 409 | `REVIEW_RECREATION_BLOCKED` | 숨김 이력이 있는 삭제 후기의 원천으로 재작성 시도 |
| 409 | `REVIEW_REPORT_ALREADY_EXISTS` | 같은 회원이 같은 후기를 다시 신고 |
| 409 | `REVIEW_CONTENT_CHANGED` | 회원 또는 관리자가 불러온 뒤 후기 본문·평점·사진이 변경됨 |
| 409 | `CONFLICT` | 주문 승인/픽업/배치, 문의·Q&A 중복 답변 등 현재 상태와 충돌하는 요청 |
| 409 | `LOCAL_PASSWORD_NOT_SET` | 소셜 전용 회원이 현재 비밀번호 변경을 요청 |
| 409 | `PHONE_ALREADY_IN_USE` | 회원가입 또는 휴대폰 변경 번호를 다른 회원이 이미 사용 중 |
| 409 | `EMAIL_ALREADY_EXISTS` | 회원가입·기준 이메일 등록 주소 중복 또는 최초 관리자 username 중복 |
| 410 | `PAYMENT_ATTEMPT_EXPIRED` | 결제 준비 후 30분 안에 confirm을 시작하지 않음 |
| 410 | `PAYMENT_RESULT_RETENTION_EXPIRED` | 최종 결제 결과의 30일 재조회 보존 기간이 지남 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | 요청 본문의 미디어 타입을 처리할 수 없음 |
| 429 | `TOO_MANY_REQUESTS` | 처리율 제한 초과 |
| 422 | `REFUND_NOT_ALLOWED` | 취소 보상 마감 이후 환불 요청 |
| 422 | `PRODUCTION_REFUND_NOT_ALLOWED` | 제작 시작 후 주문 거절/일반 환불 시도 |
| 422 | `CHANGE_NOT_ALLOWED` | 슬롯 시작 1시간 이내 변경 요청 |
| 422 | `PASS_EXPIRED` | 만료된 8회권으로 예약 또는 전체 환불 시도 |
| 422 | `PASS_CREDIT_INSUFFICIENT` | 잔여 크레딧 0인 8회권으로 예약 시도 |
| 422 | `PASS_NOT_APPLICABLE` | 이용권 계획이 선택 클래스 카테고리 또는 `passEligible` 조건을 충족하지 않음 |
| 422 | `REWARD_BALANCE_INSUFFICIENT` | 주문에 요청한 적립금이 현재 사용 가능 잔액보다 큼 |
| 422 | `CLASS_INACTIVE` | 비활성 클래스로 회차 조회 또는 예약·결제 시도 |
| 422 | `PAYMENT_METHOD_NOT_ALLOWED` | 계좌이체(`BANK_TRANSFER`)로 예약금 결제 시도 |
| 422 | `PHONE_VERIFICATION_REQUIRED` | 회원 휴대폰이 없거나 소유 확인이 완료되지 않아 결제를 시작할 수 없음 |
| 422 | `PASSWORD_UNCHANGED` | 현재와 같은 비밀번호로 변경·재설정 시도 |
| 422 | `POLICY_CONSENT_REQUIRED` | 현재 이용약관·개인정보처리방침 버전 동의가 없거나 일치하지 않음 |
| 422 | `ACCOUNT_WITHDRAWAL_BLOCKED` | 미종결 결제 시도·주문·클레임·예약·예약 취소 후속 작업·환불, 사용 가능한 8회권, 예약 적립금 또는 적립금 부채가 있어 탈퇴할 수 없음 |
| 422 | `REVIEW_NOT_ALLOWED` | 배송·픽업 또는 수강이 완료되지 않은 거래로 후기 작성 시도 |
| 422 | `REVIEW_DELETED` | 삭제된 후기 변경 시도 |
| 422 | `REVIEW_INTERACTION_NOT_ALLOWED` | 숨김·삭제 후기에 도움돼요 또는 신고 시도 |
| 422 | `REVIEW_SELF_INTERACTION_NOT_ALLOWED` | 작성자가 자기 후기에 도움돼요 또는 신고 시도 |
| 422 | `REVIEW_IMAGE_LIMIT_EXCEEDED` | 후기에 5장을 넘는 사진 첨부 시도 |
| 422 | `REVIEW_REPORT_DECISION_NOT_ALLOWED` | 대기 상태가 아닌 신고를 재판단하거나 대기 값으로 전환 시도 |
| 500 | `INTERNAL_ERROR` | 서버 내부 처리 오류 또는 내부 JSON 직렬화/역직렬화 실패 |
| 502 | `PAYMENT_FAILED` | PG가 결제 확정(`/payments/confirm`)을 최종 거절 |
| 503 | `PAYMENT_CONFIRM_RETRYABLE` | PG 결제 확정 결과를 같은 결제 정보로 재확인할 수 있는 일시 실패 |
| 503 | `SERVICE_UNAVAILABLE` | fail-closed 처리율 제한 저장소 장애 또는 인증 SMS·이메일 SMTP 등 필수 외부 작업을 시작·완료할 수 없음 |

Spring MVC가 확정한 `Allow`, content negotiation 등 표준 응답 헤더는 위 `ErrorResponse` 형식으로
본문을 바꿔도 보존한다. 이름과 업무 의미를 아는 DB 유일 제약만 해당 400/409 코드로 번역하며,
알 수 없는 무결성 위반은 입력 오류로 추측하지 않고 원인을 기록한 `500 INTERNAL_ERROR`로 처리한다.

---

문서 끝.

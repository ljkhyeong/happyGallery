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
- 전체 `/api/v1/**` OpenAPI를 생성하되, React 생성 client의 현재 실사용 범위는 공개 상품 목록·카테고리·상세 조회다. 다른 API는 필수값·nullable·enum과 인증 헤더를 확인한 뒤 순차 전환한다.

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
- 로그인 성공 시 UUID 세션 토큰을 발급하고, 이후 요청에 `Authorization: Bearer {token}` 헤더를 사용한다.
- 세션 만료: 8시간
- 세션 저장소는 Redis 기반 `AdminSessionStore`를 사용한다. 여러 인스턴스가 떠 있어도 같은 세션을 본다.
- Redis에는 관리자 토큰 원문을 키로 쓰지 않고 토큰 HMAC을 사용하며, 세션 JSON도 AES-GCM 암호문으로 저장한다.
- 세션에는 발급 당시 `credentialVersion`을 저장한다. 비밀번호 변경으로 DB 버전이 증가하면 기존 버전의 모든 세션은 즉시 인증에 실패하고, Redis 세션 키는 커밋 후 일괄 삭제한다.

#### 인증 엔드포인트

```http
POST /api/v1/admin/auth/login
Content-Type: application/json

{ "username": "admin", "password": "..." }
```

```json
{ "token": "uuid-session-token" }
```

- 성공: `200 OK`
- 실패: `401 UNAUTHORIZED`
  - `{ "code": "INVALID_CREDENTIALS", "message": "아이디 또는 비밀번호가 올바르지 않습니다." }`

```http
POST /api/v1/admin/auth/logout
Authorization: Bearer {token}
```

- 성공: `204 No Content`
- 관리자 클라이언트는 `204`를 받은 뒤에만 `sessionStorage`의 토큰을 제거한다. 요청 실패나 응답 유실 때는 현재 토큰을 유지하고 로그아웃 완료를 확인하지 못했음을 표시한다. 별도 API 요청에서 `401`을 받은 경우에는 이미 무효인 토큰이므로 서버 로그아웃 호출 없이 로컬 토큰을 제거한다.

```http
PATCH /api/v1/admin/auth/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "admin123456",
  "newPassword": "new-admin-123456"
}
```

- 새 비밀번호는 10~100자다.
- 성공: `204 No Content`. 현재 세션을 포함해 해당 관리자에게 발급된 기존 세션을 모두 폐기하므로 새 비밀번호로 다시 로그인해야 한다.
- 실패:
  - `401 INVALID_CREDENTIALS` — 현재 비밀번호 불일치
  - `403 FORBIDDEN` — 계정 ID가 없는 local API key 인증으로 변경 시도
  - `422 PASSWORD_UNCHANGED` — 현재 비밀번호와 새 비밀번호가 같음

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
- 주문 승인/거절/제작 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.
  - API Key 폴백 경로와 배치 이력은 `decided_by_admin_id = null`일 수 있다.

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

- Spring Security 적용으로 기존 공개 조회 API의 `ETag`, `If-None-Match`, `304 Not Modified` 계약은 바뀌지 않는다.
- API가 명시한 `Cache-Control: no-store` 등 응답별 캐시 정책은 그대로 적용된다.

### 1.4 민감정보 형식과 오류 노출

- 회원가입 전화번호는 공백·하이픈을 제거한 숫자 형식으로 통일하며, 회원 응답의 `phone`도 같은 형식을 사용한다.
- 휴대폰 인증과 비회원 결제 payload의 표준 전화번호 형식은 `^01[0-9]{8,9}$`이다.
- 서버 로그에는 전화번호, 인증 코드, 결제 키, 관리자 세션 토큰과 외부 서비스 오류 원문을 남기지 않는다.
- 모든 `/api/v1/**` 요청은 IP 기준 기본 처리율 제한을 적용하고, 인증·결제·검증처럼 비용이 큰 경로는 더 엄격한 독립 버킷을 사용한다.
- 인증 코드 발송·회원가입 코드 시도, 결제 확정과 비회원 이력 인증은 검증된 전화번호·주문번호·회원 ID 기준 제한도 함께 적용한다.
- Redis 처리율 제한 버킷은 IP, 전화번호, 주문번호 또는 회원 ID 원문 대신 HMAC 식별자를 사용한다.
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
  - `400 INVALID_INPUT` — 이름/카테고리 공란, durationMin/price/bufferMin 형식 오류, `passEligible` 누락 또는 콘텐츠 길이 초과
- 정책:
  - `category`는 앞뒤 공백을 제거하고 대문자 토큰으로 정규화해 저장·응답한다.
  - `price`는 1원 이상 `9,007,199,254,740,991원` 이하의 정수다.
  - `description`, `imageUrl`, `preparationInfo`, `targetAudience`는 선택값이다. `imageUrl`은 관리자 미디어 업로드 응답 경로 또는 유효한 URL을 사용한다.
  - 새 클래스는 `ACTIVE`로 생성된다. `passEligible`은 구매한 이용권 계획의 카테고리 정책과 함께 8회권 사용 가능 여부를 결정한다.

#### 2.1.2 슬롯 생성

```http
POST /api/v1/admin/slots
Content-Type: application/json
Authorization: Bearer {token}

{
  "classId": 1,
  "startAt": "2026-03-01T10:00:00"
}
```

```json
{
  "id": 42,
  "classId": 1,
  "startAt": "2026-03-01T10:00:00",
  "endAt": "2026-03-01T12:00:00",
  "capacity": 8,
  "bookedCount": 0,
  "adminActive": true,
  "bufferBlocked": false,
  "isActive": true
}
```

- 성공: `201 Created`
- 에러:
  - `404 NOT_FOUND` — classId에 해당하는 클래스 없음
  - `400 INVALID_INPUT` — 동일 classId + startAt 슬롯 이미 존재
- 정책:
  - `endAt`은 요청받지 않고 `startAt + class.durationMin`으로 서버가 계산한다.
  - 같은 클래스의 같은 `startAt`에는 종료 시각과 무관하게 슬롯을 하나만 생성할 수 있다.
  - 이미 예약된 같은 클래스 슬롯의 뒤쪽 버퍼에 포함되면 생성 응답의 `isActive`는 `false`다.
  - 같은 클래스의 슬롯 생성과 예약·반납은 클래스 행 잠금으로 직렬화해, 동시 생성된 버퍼 슬롯도 예약 상태를 빠뜨리지 않는다.
  - `adminActive`는 관리자 비활성화 여부, `bufferBlocked`는 예약 버퍼 차단 여부다.
  - `isActive`는 `adminActive=true`이고 `bufferBlocked=false`일 때만 `true`다.

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

- 성공: `200 OK` + 2.1.2와 같은 슬롯 응답
- 에러:
  - `404 NOT_FOUND` — slotId에 해당하는 슬롯 없음
- 정책:
  - `adminActive`만 `true`로 복구한다.
  - `bufferBlocked=true`이면 활성화 후에도 `isActive=false`다.

#### 2.1.5 클래스 전체 조회·수정·상태 변경

- `GET /api/v1/admin/classes` — `ACTIVE`, `INACTIVE` 클래스를 모두 반환한다.
- `PATCH /api/v1/admin/classes/{id}` — 이름·카테고리·가격·`passEligible`·설명·대표 이미지·준비물·대상 안내를 수정한다. 운영 시간과 버퍼는 기존 예약 시간축에 영향을 주므로 이 API에서 바꾸지 않는다.
- `PATCH /api/v1/admin/classes/{id}/status` — `{ "status": "ACTIVE|INACTIVE" }`로 공개·예약 가능 상태를 변경한다.
- 성공: `200 OK`, 응답은 2.1.1의 클래스 응답과 같다.
- `INACTIVE` 클래스는 공개 목록, 새 슬롯 생성과 결제 prepare 대상에서 제외한다. 기존 예약 이력은 유지한다.

#### 2.1.6 슬롯 일괄 미리보기·생성

```http
POST /api/v1/admin/slots/bulk/preview
POST /api/v1/admin/slots/bulk
Authorization: Bearer {token}
Content-Type: application/json

{
  "classId": 1,
  "dateFrom": "2026-08-01",
  "dateTo": "2026-08-31",
  "weekdays": ["TUESDAY", "THURSDAY"],
  "startTimes": ["10:00:00", "14:00:00"]
}
```

- 기간은 시작일·종료일을 포함해 최대 93일, 요일은 최대 7개, 시작 시각은 최대 24개이며 생성 후보는 최대 500개다.
- 미리보기는 DB를 바꾸지 않고 `CREATABLE`, `SKIPPED_DUPLICATE`, `SKIPPED_PAST`와 `bufferBlocked`를 반환한다.
- 실제 생성은 만들 수 있는 후보를 `CREATED`로 반환하고 과거·중복 후보는 항목별로 건너뛴다. 응답에는 `totalCount`, `creatableCount`, `createdCount`, `skippedCount`, `items`가 포함된다.
- 비활성 또는 없는 클래스, 역전된 날짜, 빈 요일/시각, 상한 초과는 거절한다.

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
    "available": true
  }
]
```

- 성공: `200 OK`
- 정책:
  - `ACTIVE` 상태 상품만 노출한다.
  - 응답은 상품 상세 조회와 동일한 필드 구조를 사용한다.
  - 재고 수량 원문은 노출하지 않고 `available`만 공개한다.
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
  "available": true
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — productId 미존재
- 정책:
  - 판매 중지 상품의 기존 상세 링크는 유지하되, 재고가 남아 있어도 `available=false`를 반환한다.
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
  - `admin_active = true`, `buffer_block_count = 0`이고 `booked_count < capacity`인 슬롯만 노출한다.
  - 정렬은 `startAt` 오름차순이다.

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
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg"
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
  "status": "ACTIVE",
  "available": true,
  "quantity": 5
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
  - `description`, `imageUrl`은 선택값이며 `imageUrl`은 `/`로 시작하는 서비스 경로 또는 `http(s)` URL이어야 한다.

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
  "type": "DECREASE",
  "quantity": 2,
  "reason": "오프라인 매장 판매"
}
```

```json
{
  "id": 10,
  "productId": 1,
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

#### 2.3.6 상품 표시 정보 수정

```http
PATCH /api/v1/admin/products/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "시그니처 캔들 리뉴얼",
  "category": "CANDLE",
  "price": 42000,
  "description": "리뉴얼한 향과 용기를 적용했습니다.",
  "imageUrl": "/api/v1/media/images/21ad89d4-73ca-43af-a11e-d7953851acb0.jpg"
}
```

- 성공: `200 OK`, 현재 재고를 포함한 `ProductResponse` 반환
- 상품 유형과 재고 수량, 판매 상태는 이 API에서 바꾸지 않는다. 재고와 상태는 각 전용 API를 사용한다.
- 이미 결제된 주문은 `order_items.product_name`, `unit_price` 스냅샷을 사용하므로 상품명·가격 변경의 영향을 받지 않는다.

### 2.4 예약 API

#### 2.4.1 휴대폰 인증 코드 발송

```http
POST /api/v1/bookings/phone-verifications

{ "phone": "01012345678" }
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
  - 인증 코드는 응답과 서버 로그에 포함하지 않는다.
  - 인증 코드는 독립 트랜잭션으로 먼저 저장하고 외부 SMS는 트랜잭션 밖에서 호출한다. NHN이 발송 요청을 정상 접수했다고 기록된 코드만 인증에 사용할 수 있다.
  - 발송 요청 실패 응답 뒤 재요청하면 새 코드를 발급한다. 발급 ID가 더 큰 코드의 접수 완료만 같은 전화번호의 이전 미소모 코드를 무효화하며, 이전 요청의 접수 완료가 늦게 돌아와도 최신 코드 상태를 덮지 않는다.
  - 개발/테스트 환경에서는 `GET /api/v1/admin/dev/phone-verifications/latest?phone=` 로 코드를 조회할 수 있다.

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
  "slotId": 42,
  "startAt": "2026-03-01T10:00:00",
  "endAt": "2026-03-01T12:00:00",
  "className": "향수 클래스",
  "status": "BOOKED",
  "depositAmount": 5000,
  "balanceAmount": 45000,
  "guestName": "홍길동",
  "guestPhone": "010****5678",
  "cancelPolicy": {
    "cancellable": true,
    "refundable": true,
    "deadlineAt": "2026-03-01T00:00:00",
    "passCreditRestorable": false,
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
  "status": "BOOKED"
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 동일 슬롯으로 변경 시도
  - `404 NOT_FOUND` — 예약 미존재 또는 token 불일치
  - `409 CAPACITY_EXCEEDED` — 새 슬롯 정원 초과
  - `409 DUPLICATE_BOOKING` — 동일 전화번호 + 새 슬롯에 활성 예약이 이미 존재
  - `409 SLOT_NOT_AVAILABLE` — 새 슬롯 비활성
  - `409 BOOKING_CONFLICT` — 낙관적 락 충돌
  - `422 CHANGE_NOT_ALLOWED` — 현재 슬롯 시작 1시간 이내
- 정책:
  - 현재 슬롯 시작 1시간 전까지 횟수 제한 없이 변경 가능
  - 변경마다 `booking_history`에 `RESCHEDULED` 이력 누적
  - `bookings` 행은 항상 1건 유지한다.

#### 2.4.5 비회원 예약 취소

```http
DELETE /api/v1/bookings/{bookingId}
X-Access-Token: {accessToken}
```

```json
{
  "bookingId": 1,
  "status": "CANCELED",
  "refundable": true,
  "refundAmount": 5000,
  "refund": {
    "amount": 5000,
    "status": "REQUESTED"
  }
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
  - `400 INVALID_INPUT` — `BOOKED` 상태가 아닌 예약
- 정책:
  - 크레딧은 예약 시 `USE` ledger로 이미 소모되어 추가 변동이 없다.

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
  - `refundAmount = refundCredits × (totalPrice / totalCredits)`
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
  "status": "PAID_APPROVAL_PENDING",
  "totalAmount": 121000,
  "shippingFee": 3000,
  "paidAt": "2026-03-08T20:30:00",
  "approvalDeadlineAt": "2026-03-09T20:30:00",
  "items": [
    { "productId": 1, "productName": "시그니처 캔들", "qty": 2, "unitPrice": 39000 },
    { "productId": 3, "productName": "우드 트레이", "qty": 1, "unitPrice": 40000 }
  ],
  "fulfillment": {
    "type": "SHIPPING",
    "expectedShipDate": null,
    "pickupDeadlineAt": null,
    "carrier": null,
    "trackingNumber": null
  },
  "refund": null
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — orderId 미존재 또는 token 불일치
- 정책:
  - 신규 서명 토큰은 HMAC 서명과 만료 시각을 검증한 뒤 서명된 claim의 nonce 해시를 DB 저장값과 비교한다. 레거시 32자 16진수 토큰은 전체 원문의 SHA-256 해시를 비교하는 호환 경로를 유지한다.
  - 신규 주문의 `fulfillment`는 결제 confirm 시 함께 생성되며 고객이 선택한 `type`, 예상 출고일, 픽업 마감과 배송 추적 정보를 반환한다. 배송지 원문은 고객 응답에 포함하지 않는다.
  - `shippingFee`는 prepare 당시 서버 정책 스냅샷이다. `totalAmount`에는 상품 합계와 배송비가 모두 포함되며 픽업 주문의 배송비는 0원이다.
  - 각 항목의 `productName`, `unitPrice`는 prepare 당시 스냅샷이다. 배송 출발 뒤에는 `carrier`, `trackingNumber`를 함께 반환한다.
  - 환불 이력이 있으면 `refund`에 `amount`, `status`를 반환하고, 없으면 `null`이다. 고객 응답에는 `refundId`, 실패 사유, 시도 횟수를 노출하지 않는다.
  - `status=PICKUP_EXPIRED`는 기성품 미수령 환불이며 `refund`에 진행 상태를 반환한다. `status=PICKUP_FORFEITED`는 주문제작 상품의 미수령 종료이며 `refund=null`이다.
  - `DELETE /api/v1/orders/{id}`는 비회원 접근 토큰으로, `DELETE /api/v1/me/orders/{id}`는 회원 세션으로 본인 주문을 확인한다. `PAID_APPROVAL_PENDING`만 `CUSTOMER_CANCELED`로 전이하고 재고 복구·이력·환불 요청을 함께 처리한다.
  - `POST /api/v1/orders/{id}/delay-response`와 `POST /api/v1/me/orders/{id}/delay-response`는 `{ "decision": "ACCEPT|REJECT" }`를 받는다. `DELAY_CONSENT_PENDING`에서 수락하면 `DELAY_ACCEPTED`, 거절하면 `DELAY_REJECTED_CANCELED`와 전액 환불 요청으로 전이한다.
  - 고객 액션 응답은 `{orderId,status,refund}`다. `refund`가 `REQUESTED`여도 PG 완료를 뜻하지 않으며 주문 상세에서 진행 상태를 다시 확인한다.

#### 2.6.3 주문 가격 정책 조회

```http
GET /api/v1/orders/policy
```

```json
{ "shippingFee": 3000 }
```

- 인증 없이 결제 전 고정 배송비를 조회한다. 실제 결제 금액은 prepare 시 서버 설정으로 다시 확정한다.
- `PICKUP`은 이 값과 무관하게 배송비 0원이다. 현재 무료 배송 임계값은 없고 운영 기본값은 `ORDER_SHIPPING_FEE=0`이다.

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
        { "productId": 1, "productName": "시그니처 캔들", "qty": 2, "unitPrice": 39000 },
        { "productId": 3, "productName": "우드 트레이", "qty": 1, "unitPrice": 40000 }
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
  - `keyword`가 `ORD-{숫자}` 형식의 주문번호이면 해당 주문 ID와 정확 일치로 검색한다. 그 외 숫자·문자열은 주문 ID 부분 일치 또는 회원·비회원 이름 정확 일치로 검색한다.
  - `dateFrom`~`dateTo`는 KST 기준 주문 생성일 범위를 의미한다.
  - 결과는 `createdAt DESC` 기준 OFFSET 페이지로 반환한다.
  - `createdAt`은 UTC 오프셋(`Z`)을 포함해 브라우저가 서울 시각으로 정확히 변환할 수 있게 한다.

#### 2.7.3 주문 운영 엔드포인트

- `POST /api/v1/admin/orders/{id}/approve`
  - 응답: `200 OK` 본문 없음
  - 정책:
    - `PAID_APPROVAL_PENDING`만 승인 가능
    - `MADE_TO_ORDER` 상품이 있으면 `IN_PRODUCTION`, 아니면 `APPROVED_FULFILLMENT_PENDING`으로 전이
    - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다
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
- `POST /api/v1/admin/orders/{id}/delay`
  - 응답: `{ "orderId": 5, "status": "DELAY_CONSENT_PENDING", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - `IN_PRODUCTION`에서만 지연 제안 가능
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
    - 이력은 `DELAY_CANCEL`로 기록하고 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.
    - `DELAY_ACCEPTED`는 이미 지연을 수락한 상태이므로 400으로 거절한다.
- `POST /api/v1/admin/orders/{id}/resume-production`
  - 응답: `{ "orderId": 5, "status": "IN_PRODUCTION", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - `DELAY_ACCEPTED`에서만 제작 재개 가능
    - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다
- `POST /api/v1/admin/orders/{id}/prepare-pickup`
  - 요청: `{ "pickupDeadlineAt": "2026-04-16T18:00:00" }`
  - 응답: `{ "orderId": 5, "status": "PICKUP_READY", "pickupDeadlineAt": "2026-04-16T18:00:00" }`
  - 정책:
    - 결제 시 고객이 `PICKUP`을 선택한 주문만 처리한다.
    - `pickupDeadlineAt`은 서버 `Clock` 기준 현재보다 이후여야 한다.
    - 이력은 `PICKUP_READY`로 기록하고 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.
- `POST /api/v1/admin/orders/{id}/complete-pickup`
  - 응답: `{ "orderId": 5, "status": "PICKED_UP", "pickupDeadlineAt": "2026-04-16T18:00:00" }`
  - 정책:
    - 이력은 `PICKUP_COMPLETE`로 기록하고 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.

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
  - 기성품 주문은 재고를 복구하고 환불 요청 이력을 만든 뒤 `PICKUP_EXPIRED`로 전이한다. PG 환불은 부모 트랜잭션 커밋 후 비동기로 실행한다.
  - 주문제작 상품이 하나라도 포함된 주문은 제작 완료 상품으로 보아 환불 요청과 재고 복구 없이 `PICKUP_FORFEITED`로 전이한다.
  - 이력은 각각 `PICKUP_EXPIRED`, `PICKUP_FORFEITED`로 기록하며 자동 처리이므로 adminId는 `null`이다.
  - `successCount`는 만료 상태 전이에 성공한 건수이며 PG 환불 완료 건수가 아니다.
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
  - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.
  - API Key 폴백 경로는 `null`일 수 있다.

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

{ "carrier": "CJ대한통운", "trackingNumber": "123456789012" }
```

```json
{ "orderId": 5, "status": "SHIPPING_PREPARING", "expectedShipDate": "2026-04-15" }
```

```json
{ "orderId": 5, "status": "SHIPPED", "expectedShipDate": "2026-04-15", "carrier": "CJ대한통운", "trackingNumber": "123456789012" }
```

```json
{ "orderId": 5, "status": "DELIVERED", "expectedShipDate": "2026-04-15" }
```

- 정책:
  - `APPROVED_FULFILLMENT_PENDING` → `SHIPPING_PREPARING` → `SHIPPED` → `DELIVERED` 순서만 허용한다.
  - 결제 시 고객이 `SHIPPING`을 선택한 주문만 배송 흐름을 시작할 수 있다. 픽업 주문은 상태 변경 전에 거절한다.
  - `mark-shipped`의 `carrier`와 `trackingNumber`는 공백일 수 없고 각각 최대 50자, 100자다. 두 값은 fulfillment에 한 쌍으로 저장하고 고객·관리자 상세에 노출한다.
  - 각 전이는 `order_approvals` 이력에 `PREPARE_SHIPPING`, `SHIP`, `DELIVER`로 기록한다.
  - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.

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
  - `decision`: `APPROVE`, `REJECT`, `CUSTOMER_CANCEL`, `DELAY`, `DELAY_ACCEPT`, `DELAY_REJECT`, `DELAY_CANCEL`, `AUTO_REFUND`, `PRODUCTION_COMPLETE`, `RESUME_PRODUCTION`, `PICKUP_READY`, `PICKUP_COMPLETE`, `PICKUP_EXPIRED`, `PICKUP_FORFEITED`, `PREPARE_SHIPPING`, `SHIP`, `DELIVER`

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
  "createdAt": "2026-03-24T09:00:00"
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — noticeId 미존재
- 정책:
  - 상세 조회 시 `viewCount`를 1 증가시킨 뒤 최신 값을 반환한다.
  - `200 OK` 응답에는 `ETag` 헤더를 포함한다.
  - `If-None-Match`가 현재 ETag와 같으면 `304 Not Modified`를 반환한다.

#### 2.8.3 관리자 공지 CRUD

- `GET /api/v1/admin/notices`
  - 응답: 공개 목록 조회와 동일한 배열
- `POST /api/v1/admin/notices`
  - 요청: `{ "title": "점검 공지", "content": "3/28 점검 예정", "pinned": true }`
  - 응답: `201 Created` + 공지 상세 응답
- `PUT /api/v1/admin/notices/{id}`
  - 요청: `{ "title": "수정 공지", "content": "본문 수정", "pinned": false }`
  - 응답: `200 OK` + 공지 상세 응답
- `DELETE /api/v1/admin/notices/{id}`
  - 응답: `204 No Content`

공통 에러:
- `401 UNAUTHORIZED` — 관리자 인증 실패
- `404 NOT_FOUND` — noticeId 미존재

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
    "bookerType": "GUEST",
    "bookerName": "홍길동",
    "bookerPhone": "010****5678",
    "className": "향수 클래스",
    "startAt": "2026-03-20T10:00:00",
    "endAt": "2026-03-20T12:00:00",
    "status": "BOOKED",
    "depositAmount": 5000,
    "depositPaidAt": "2026-03-18T14:00:00",
    "balanceAmount": 45000,
    "balanceStatus": "UNPAID",
    "balancePaidAt": null,
    "arrears": false,
    "passBooking": false
  }
]
```

- 성공: `200 OK`
- 정책:
  - `bookerType`은 `GUEST` 또는 `MEMBER`로 구분한다.
  - 비회원 이력 가져오기(claim) 이후 `userId`가 설정된 예약은 `MEMBER`로 표시한다.
  - 선택 귀속 요청의 주문 ID와 예약 ID는 각각 최대 100건이며 모든 ID는 양수여야 한다.
  - User 정보는 batch fetch(`UserReaderPort.findAllById`)로 조합한다.
  - `date` 필수, `status`는 선택(미입력 시 전체).

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
  - `page`는 0 미만이면 0으로, `size`는 1~100 범위로 보정하며 표현 가능한 OFFSET을 넘으면 `400 INVALID_INPUT`으로 거절한다.
  - `keyword`가 `BK-{숫자}` 형식의 예약번호이면 해당 예약 ID와 정확 일치로 검색한다. 그 외 숫자·문자열은 예약 ID 부분 일치 또는 회원·비회원 이름 정확 일치로 검색한다.
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
      "passPurchaseId": null,
      "paymentAttemptId": null,
      "amount": 5000,
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
  "status": "SUCCEEDED",
  "attemptCount": 1,
  "failReason": null
}
```

- 성공: `200 OK`
- 에러: `404 NOT_FOUND` — refundId 미존재
- 주문 거절·지연 거절 취소·8회권 환불 시작 응답의 `refundId`로 실제 PG 상태를 조회한다.

#### 2.11.3 환불 재시도

```http
POST /api/v1/admin/refunds/{refundId}/retry
Authorization: Bearer {token}
```

```json
{
  "refundId": 42,
  "amount": 5000,
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
  - `RECONCILIATION_REQUIRED`는 Toss cancel을 바로 다시 호출하지 않는다. `paymentKey`로 결제와 취소 내역을 조회해 요청 금액과 정확히 같은 최신 `DONE` 취소, `transactionKey`, 결제 상태 `CANCELED|PARTIAL_CANCELED`를 모두 확인하면 `SUCCEEDED`로 화해한다.
  - 취소 내역이 없고 결제 상태가 `DONE`이면 미취소로 확정해 `RETRYABLE`로 바꾼다. 다음 실행부터 최초 멱등키로 cancel을 호출한다. 금액·상태 모순이나 조회 실패는 `RECONCILIATION_REQUIRED`를 유지해 중복 취소를 피한다.
  - PG 조회 응답의 `paymentKey`, 취소 금액, `transactionKey`가 저장 요청과 일치하는지 결과 저장 전에 다시 확인한다.
  - PG 호출 전 선점과 호출 후 결과 저장은 부모 주문/예약 트랜잭션 및 PG 네트워크 구간과 분리된 짧은 `REQUIRES_NEW` 트랜잭션으로 처리한다.
  - `paymentAttemptId`가 있으면 PG 승인 후 주문·예약·8회권 생성에 실패한 결제의 보상 환불이다.
  - 최초 환불과 자동·수동 재처리는 같은 `refunds.idempotency_key`를 Toss `Idempotency-Key` 헤더로 사용한다.

#### 2.11.4 실패 알림 목록과 재처리

- `GET /api/v1/admin/notifications/failed`
  - 자동 재시도를 모두 소진한 `FAILED` outbox를 오래된 순서로 최대 100건 반환한다.
  - `createdAt`은 UTC 오프셋을 포함한 ISO-8601 시각으로 반환한다.
- `POST /api/v1/admin/notifications/{outboxId}/retry`
  - 성공: `200 OK`, 기존 outbox를 `PENDING`으로 다시 연 결과를 반환한다.
  - 에러: `404 NOT_FOUND`, `400 INVALID_INPUT`(최종 실패가 아닌 outbox)

재처리는 새 알림 요청을 만들지 않고 기존 outbox와 멱등키를 그대로 사용한다. 다음 scheduler 주기 또는 dispatcher가 발송을 다시 시도한다.

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

### 2.12 회원 API (`/api/v1/me`)

회원 인증은 `HG_SESSION` HttpOnly 쿠키 기반이며, Spring Security의 회원 principal로 검증한다.

#### 2.12.0 회원 인증 정책

- 회원 세션은 `HG_SESSION` HttpOnly 쿠키로 유지한다.
- 로그인·회원가입·소셜 로그인 성공 시 기존 세션 ID를 회전하고 새 ID로 회원 세션을 유지한다.
- 상태를 변경하는 요청은 1.3의 SPA CSRF 계약에 따라 `X-XSRF-TOKEN` 헤더를 함께 보낸다.
- 회원 로그인은 이메일/비밀번호(local)와 Google, Naver OAuth2를 함께 지원한다.
- 소셜 계정은 `user_social_accounts`에 `(provider, provider_id_hmac)`로 저장한다. 한 회원은 Google과 Naver 계정을 각각 하나씩 연결할 수 있다.
- Google은 `email_verified=true`인 프로필만 수용한다.
- 외부 provider ID가 처음인데 같은 이메일의 기존 회원이 있으면 제공자와 관계없이 자동 연결하지 않는다. 이미 연결된 provider ID 로그인과 이메일 충돌이 없는 신규 가입만 허용한다.
- 소셜 로그인으로 새로 생성된 회원은 `password_hash`가 비어 있을 수 있다.
- 회원 응답의 `localPasswordEnabled`는 이메일 로그인 비밀번호가 설정되어 있는지를 나타낸다.
- 이메일은 앞뒤 공백을 제거한 소문자, 전화번호는 공백·하이픈을 제거한 숫자 형식으로 통일한다. 응답에는 복호화한 값을 반환한다.

#### 2.12.0.1 이메일 회원가입

```http
POST /api/v1/auth/signup

{
  "email": "member@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "01012345678",
  "verificationCode": "483921"
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
  - `429 TOO_MANY_REQUESTS` — 회원가입 처리율 제한 초과
- 정책:
  - 인증 코드 발급은 2.4.1을 사용한다.
  - 회원 저장과 인증 코드 1회 소모를 같은 트랜잭션에서 처리하며 성공한 회원은 `phoneVerified=true`다.
  - 같은 전화번호의 회원가입 인증 코드 시도는 5회/10분으로 제한한다.
  - 로그인 성공과 동일하게 세션 ID를 회전한다.

#### 2.12.0.2 소셜 로그인 시작

```http
GET /api/v1/auth/social/authorization/{provider}
```

- `{provider}`: `google` 또는 `naver`
- 성공: `302 Found`, `Location`은 해당 제공자의 authorization endpoint
- 에러:
  - `429 TOO_MANY_REQUESTS` — 로그인 시작 IP 버킷 분당 10회 초과
- 정책:
  - 브라우저는 JSON URL 발급 API를 먼저 호출하지 않고 이 경로로 직접 이동한다.
  - Spring Security OAuth2 Client가 `state`를 포함한 authorization request를 만들고 callback 전까지만 현재 Redis HTTP 세션에 저장한다.
  - callback URI는 provider별 `GOOGLE_OAUTH_REDIRECT_URI`, `NAVER_OAUTH_REDIRECT_URI` 설정에 고정하며 브라우저 요청값으로 받지 않는다.
  - Google은 `openid`, `profile`, `email` 범위의 OIDC 로그인을 사용한다. 로그인만을 위해 refresh token을 요청하거나 저장하지 않는다.

#### 2.12.0.3 소셜 로그인 callback

```http
GET /api/v1/auth/social/callback/{provider}?code=...&state=...
```

- 이 경로는 Google/Naver가 호출하는 backend callback이며 프런트가 직접 호출하지 않는다.
- 성공: `302 Found` → `/auth/callback?newUser=true|false`
- 실패: `302 Found` → `/auth/callback?error=SOCIAL_LOGIN_FAILED`
- 기존 이메일과 충돌: `302 Found` → `/auth/callback?error=SOCIAL_ACCOUNT_LINK_REQUIRED`
- 처리율 제한 초과: `429 TOO_MANY_REQUESTS`
- 정책:
  - Spring Security가 callback의 `state`와 세션의 authorization request를 비교하고 한 번 사용한 authorization request를 제거한 뒤 code를 토큰으로 교환한다.
  - Google ID Token과 UserInfo, Naver UserInfo를 공통 소셜 프로필로 변환한 뒤에만 application의 계정 연결 트랜잭션을 시작한다.
  - 성공 시 세션 ID를 한 번 회전하고 `customerUserId`, `customerCredentialVersion`, `userId:credentialVersion` 형식의 principal 인덱스를 장기 인증 상태로 저장한다.
  - OAuth `SecurityContext`, access token, refresh token은 세션에 저장하지 않는다. 다음 요청은 기존 `CustomerAuthenticationFilter`가 `customerUserId`로 회원 principal을 다시 구성한다.
  - 성공 후 기존 CSRF 토큰이 폐기되므로 클라이언트는 새 CSRF 토큰을 발급받는다.
  - `newUser=true`이면 프런트는 마이페이지의 최초 전화번호 등록 온보딩을 연다. callback 상태를 잃고 마이페이지에 직접 진입해도 현재 회원의 `phone=null`이면 같은 온보딩을 연다.
  - 소셜 로그인 시작과 callback은 서로 분리된 IP 버킷으로 각각 분당 10회 제한한다.

#### 2.12.0.4 회원 휴대폰 등록·변경

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
  "email": "social@example.com",
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
  - `409 PHONE_ALREADY_IN_USE` — 다른 회원이 이미 사용하는 전화번호
  - `429 TOO_MANY_REQUESTS` — 같은 전화번호의 인증 코드 확인 시도 초과
- 정책:
  - 인증 코드 발급은 2.4.1의 `POST /api/v1/bookings/phone-verifications`를 사용한다.
  - 회원 행 잠금 아래 새 번호의 인증 코드를 한 번 소비하고 `phone_enc`, `phone_hmac`, `phone_verified=true`를 같은 트랜잭션에서 저장한다.
  - 전화번호가 없는 소셜 회원의 최초 등록과 기존 회원의 번호 변경에 같은 API를 사용한다. `users.phone_hmac`은 null 외 값에 UNIQUE 제약을 적용한다.
  - 비회원 이력 가져오기는 `/api/v1/me/guest-claims/**` 계약을 사용하며 번호 변경만으로 자동 이관하지 않는다.
  - `GET /api/v1/me`의 `phone`은 최초 등록 전 `null`일 수 있다.

#### 2.12.0.5 로그인 비밀번호 변경

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
  - 현재 비밀번호는 `PasswordEncoder.matches(...)`로 확인하고 새 비밀번호는 BCrypt로 다시 해시한다.
  - 성공하면 `credential_version`을 증가시키고 현재 요청을 포함한 모든 회원 세션을 무효화한다.
  - 소셜 로그인만 사용하는 회원은 이 API 대신 2.12.0.6의 SMS 재설정으로 최초 로컬 비밀번호를 설정한다.

#### 2.12.0.6 검증된 휴대폰으로 비밀번호 재설정

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
  - 이메일과 회원에게 저장된 `phoneVerified=true` 전화번호가 일치하고, 같은 번호의 미소모·유효 SMS 코드를 한 번 소비해야 한다.
  - 계정·전화번호·인증코드 중 어느 값이 틀렸는지는 `PASSWORD_RESET_FAILED` 하나로 응답해 계정 존재 여부를 구분하지 못하게 한다.
  - `password_hash=null`인 소셜 전용 회원도 성공할 수 있으며, 성공 후 이메일 로그인이 활성화된다.
  - 성공하면 `credential_version`을 증가시키고 해당 회원의 모든 세션을 무효화한다.

#### 2.12.0.7 회원 탈퇴

```http
DELETE /api/v1/me
Cookie: HG_SESSION={sessionToken}
```

- 성공: `204 No Content`, 현재 세션을 포함한 기존 회원 세션 폐기
- 에러:
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `422 ACCOUNT_WITHDRAWAL_BLOCKED` — 미완료 주문, `BOOKED` 예약, 사용 가능한 미만료 8회권 또는 미완료 환불이 있음
- 정책:
  - 회원 행을 잠그고 차단 활동을 다시 확인해 탈퇴와 새 거래 생성을 직렬화한다.
  - 이메일·이름은 재사용 가능한 탈퇴 식별값으로 바꾸고 전화번호·비밀번호·소셜 연결을 제거한다. `withdrawnAt`과 새 자격 버전을 저장하며 주문·예약·정산 이력은 보존한다.
  - 탈퇴 회원은 로그인과 일반 회원 조회에서 제외한다. 커밋 뒤 이전 자격 버전의 Redis 세션을 폐기한다.

#### ~~2.12.1 회원 예약 생성~~ (2026-04-22 제거)

> 회원 예약 생성도 `POST /api/v1/payments/prepare` (`context=BOOKING`, `payload.userId` 지정) → `POST /api/v1/payments/confirm`으로 단일화됨. 8회권 사용 예약은 `payload.passId`를 채워 amount=0 → confirm 직접 호출 경로를 탄다. 2.15 결제 API 참조.

#### ~~2.12.2 회원 주문 생성~~ (2026-04-22 제거)

> 회원 주문 생성도 `POST /api/v1/payments/prepare` (`context=ORDER`, `payload.userId` 지정) → `POST /api/v1/payments/confirm`으로 단일화됨. 2.15 결제 API 참조.

#### 2.12.3 회원 목록/상세 조회

- `GET /api/v1/me/bookings` — 회원 예약 목록
- `GET /api/v1/me/bookings/{id}` — 회원 예약 상세
- `GET /api/v1/me/orders` — 회원 주문 목록
- `GET /api/v1/me/orders/{id}` — 회원 주문 상세
- `GET /api/v1/me/passes` — 회원 8회권 목록
- `GET /api/v1/me/passes/{id}` — 회원 8회권 상세
- `POST /api/v1/me/passes/{id}/refund` — 소유한 8회권 잔여 횟수 정산 환불
- `DELETE /api/v1/me/orders/{id}` — 승인 대기 주문 취소
- `POST /api/v1/me/orders/{id}/delay-response` — 제작 지연 제안 수락/거절

회원 주문 액션은 세션 소유권을 검증한다. 취소는 `PAID_APPROVAL_PENDING`, 지연 응답은 `DELAY_CONSENT_PENDING`에서만 허용하며 응답의 환불 상태는 실제 PG 완료와 분리한다.

회원 예약 상세 응답은 `passBooking`과 `cancelPolicy`를 포함한다.

```json
{
  "bookingId": 1,
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
  "secret": true,
  "password": "1234"
}
```

- 성공: `201 Created`
- 에러:
  - `401 UNAUTHORIZED` — 회원 세션 없음
  - `400 INVALID_INPUT` — 비밀글인데 비밀번호가 비어 있음
  - `404 NOT_FOUND` — 상품 미존재
- 정책:
  - 작성 주체는 회원(User)만 허용한다.
  - `secret=true`일 때 비밀번호를 설정해 공개 상세 조회 전 검증한다.
  - 응답에는 작성 결과 요약만 반환한다.

#### 2.12.5 회원 1:1 문의 작성/조회

- `POST /api/v1/me/inquiries` — 회원 문의 생성
- `GET /api/v1/me/inquiries` — 내 문의 목록
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
  - 응답에는 `hasReply`, `replyContent`, `repliedAt`를 포함한다.

#### 2.12.6 회원 장바구니

- `GET /api/v1/me/cart` — 내 장바구니 조회
  - 응답:

```json
{
  "items": [
    {
      "productId": 1,
      "productName": "시그니처 캔들",
      "price": 39000,
      "qty": 2,
      "subtotal": 78000,
      "available": true
    }
  ],
  "totalAmount": 78000
}
```

- `POST /api/v1/me/cart/items`
  - 요청: `{ "productId": 1, "qty": 2 }`
  - 응답: `201 Created`
- `POST /api/v1/me/cart/merge`
  - 요청: `{ "idempotencyKey": "UUID", "items": [{ "productId": 1, "qty": 2 }] }`
  - 응답: `204 No Content`
  - 로그인 직전의 비회원 장바구니를 한 번에 합친다. 같은 회원과 멱등키의 재요청은 수량을 다시 더하지 않는다.
  - 같은 회원과 멱등키로 다른 상품·수량을 보내면 `409 CONFLICT`로 거절한다.
- `PUT /api/v1/me/cart/items/{productId}`
  - 요청: `{ "qty": 3 }`
  - 응답: `200 OK` 본문 없음
- `DELETE /api/v1/me/cart/items/{productId}`
  - 응답: `204 No Content`
- 장바구니 결제는 별도 checkout API를 두지 않는다. `POST /api/v1/payments/prepare`에 `context=ORDER`, `payload.userId`, `payload.cartCheckout=true`, `payload.items=[]`를 보내 시작한다.

공통 정책:
- 인증 실패 시 `401 UNAUTHORIZED`
- 장바구니는 회원 전용이며 `user_id + product_id` 단위로 중복 없이 관리한다.
- 추가·수정·병합 수량은 상품별 1~99개다. 병합 요청에서 같은 상품이 여러 번 나오면 합산 수량에도 같은 상한을 적용한다.
- 비회원 장바구니 병합의 멱등키 기록과 회원 장바구니 수량 변경은 같은 DB 트랜잭션으로 처리한다.
- 장바구니 병합 멱등 응답은 요청 생성 후 7일간 보장한다. 클라이언트는 이 기간을 넘겨 같은 키를 재사용하지 않으며 서버는 보존 배치에서 오래된 기록을 정리한다.
- 클라이언트는 병합 응답을 확인할 때까지 회원·멱등키·상품 스냅샷을 바꾸지 않는다. 성공 후 스냅샷 수량만 로컬 장바구니에서 차감하고, 도중에 추가된 수량은 새 멱등키로 이어서 병합한다.
- 상품이 `ACTIVE`가 아니거나 재고가 없으면 `available=false`로 표시되며, checkout 시 구매 가능한 항목만 주문으로 전환한다.
- 장바구니 prepare는 구매 가능한 항목만 서버에서 선택하고, confirm 성공 시 prepare에서 확정한 수량만 차감한다. 결제 진행 중 추가한 같은 상품 수량과 다른 상품은 유지한다.

#### 2.12.7 회원 알림함

- `GET /api/v1/me/notifications?page=0&size=20`
  - 응답:

```json
[
  {
    "id": 101,
    "channel": "KAKAO",
    "eventType": "ORDER_PAID",
    "status": "SUCCESS",
    "sentAt": "2026-03-28T09:15:00",
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
- 목록은 `sentAt DESC` 기준 페이지네이션으로 조회한다.
- `readAt != null`이면 `read=true`로 본다.

### 2.13 공개 Product Q&A API
#### 2.13.1 상품 Q&A 목록 조회

```http
GET /api/v1/products/{productId}/qna
```

- 성공: `200 OK`
- 정책:
  - 작성자 이름은 마스킹해 반환한다.
  - `secret=true`인 글은 제목을 `[비밀글입니다]`로 가려서 반환한다.
  - 공개 목록에는 본문/답변 전문을 포함하지 않는다.

#### 2.13.2 비밀글 비밀번호 검증 후 상세 조회

```http
POST /api/v1/products/{productId}/qna/{id}/verify

{
  "password": "1234"
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 비밀번호 불일치
  - `404 NOT_FOUND` — Q&A 미존재 또는 URL의 상품에 속하지 않음
- 정책:
  - 비밀글이 아니면 그대로 상세를 반환한다.
  - 비밀번호가 일치하면 제목/본문/답변을 포함한 상세를 반환한다.

### 2.14 관리자 Q&A / 문의 API

#### 2.14.1 관리자 상품 Q&A 조회/답변

- `GET /api/v1/admin/qna?productId={productId}` — 특정 상품의 Q&A 목록 조회
- `POST /api/v1/admin/qna/{id}/reply` — Q&A 답변 등록

정책:
- 인증: `Authorization: Bearer {token}`
- 답변 작성 시 `replyContent`, `repliedAt`, `repliedBy`를 기록한다.
- 이미 답변이 있는 글에 재답변을 시도하면 서버가 거절한다.
- 답변 저장과 `PRODUCT_QNA_ANSWERED` 회원 알림 outbox insert를 같은 트랜잭션으로 처리한다. 멱등키는 회원·이벤트·`PRODUCT_QNA`·Q&A ID 조합이다.

#### 2.14.2 관리자 1:1 문의 조회/답변

- `GET /api/v1/admin/inquiries?cursor={cursor}&size=20` — 최신 문의 커서 페이지 조회
- `GET /api/v1/admin/inquiries/{id}` — 문의 상세 조회
- `POST /api/v1/admin/inquiries/{id}/reply` — 문의 답변 등록

정책:
- 인증: `Authorization: Bearer {token}`
- 회원 이름을 함께 반환한다.
- 목록 응답은 `{content, nextCursor, hasMore}`이고 `size` 범위는 1~100이다.
- 이미 답변이 있는 문의에 재답변을 시도하면 서버가 거절한다.
- 답변 저장과 `INQUIRY_ANSWERED` 회원 알림 outbox insert를 같은 트랜잭션으로 처리한다. 외부 Alimtalk/SMS 발송은 커밋 뒤 실행하며 같은 문의의 중복 발송 요청은 멱등키로 합친다.

### 2.15 결제 API (`/api/v1/payments`)

주문/예약/8회권의 표준 결제 생성 경로는 `POST /api/v1/payments/prepare` → `POST /api/v1/payments/confirm`이다.
서버가 `prepare` 단계에서 `orderId(UUID)`와 `amount`를 확정해 `payment_attempt` 레코드(`PENDING`)로 저장하고,
프론트가 Toss 결제창을 통과한 뒤 `confirm`이 동일 `amount` 일치를 강제한 뒤 도메인 저장(주문/예약/8회권)을 수행한다.
회원 장바구니도 같은 prepare/confirm 경로를 사용한다.

회원/비회원 구분은 요청 본문이 아니라 인증 컨텍스트(`HG_SESSION` 쿠키 유무)로 결정한다.
회원 경로는 현재 회원의 `phone`이 존재하고 `phoneVerified=true`여야 하며, 미등록 상태에서는 `422 PHONE_VERIFICATION_REQUIRED`를 반환한다.
8회권 사용 예약처럼 amount가 0이면 응답된 `amount=0`을 보고 프론트가 PG 호출 없이 `confirm`을 직접 호출한다.

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
      { "productId": 1, "qty": 2 }
    ],
    "fulfillmentType": "PICKUP",
    "shippingAddress": null
  }
}
```

```json
{
  "orderId": "f2d3a1b4-9d24-4f0a-8a8a-7c8b06f5b1a2",
  "amount": 78000,
  "context": "ORDER"
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — payload context와 `context` 필드 불일치, 항목 누락, 상품별 수량 1~99 범위 위반, 주문 금액 안전 정수 범위 초과 또는 overflow, 회원/비회원 정보 불일치 등
  - `404 NOT_FOUND` — 상품/슬롯 미존재
  - `422 PAYMENT_METHOD_NOT_ALLOWED` — `BookingPayload.paymentMethod=BANK_TRANSFER`
- 정책:
  - `payload.type`은 `ORDER` / `BOOKING` / `PASS` 중 하나로, 상위 `context`와 일치해야 한다.
  - 금액은 서버가 산출한다. 클라이언트가 `amount`를 보내도 무시되며, `payment_attempt.amount`는 서버 계산값이다.
  - 모든 컨텍스트의 최종 `amount`는 0원 이상 `9,007,199,254,740,991원` 이하의 웹 안전 정수여야 한다. 0원은 유효한 8회권 예약처럼 외부 PG 호출이 없는 내부 승인에만 사용한다.
    - `ORDER`: 동일 `productId`의 수량을 먼저 합쳐 상품별 1~99개 제한을 적용하고, 상품을 한 번에 조회한 뒤 `productId.price * qty`를 overflow 검출 산술로 합산한다. `SHIPPING`이면 `app.order.shipping-fee`의 고정액을 더하고 `PICKUP`이면 0원을 더한다. 총액은 `9,007,199,254,740,991원` 이하로 제한한다.
    - `BOOKING`: `passId`가 있으면 0 (8회권 사용 예약), 없으면 `slot.bookingClass.price * 10%`
    - `PASS`: `app.pass.total-price`(기본 `PASS_TOTAL_PRICE=240000`)
  - 서버는 prepare 시점의 `ORDER` 상품명·항목 단가·배송비, `BOOKING` 예약금·잔금, `PASS` 총 가격과 계획을 내부 payload로 저장한다. 내부 payload 전체는 `payment_attempt.payload_enc`에 AES-GCM 암호문으로 저장한다. confirm은 현재 가격을 다시 계산하지 않고 이 스냅샷으로 도메인을 생성하며, 저장된 결제 금액과 `payment_attempt.amount`가 다르면 PG 호출 전에 거절한다.
  - 클라이언트의 `ORDER` payload에는 단가를 받지 않는다.
  - `ORDER` payload는 `fulfillmentType=SHIPPING|PICKUP`을 반드시 포함한다. `SHIPPING`은 구조화된 `shippingAddress`가 필수이고 `PICKUP`은 `shippingAddress=null`이어야 한다.
  - 클라이언트는 `GET /api/v1/orders/policy`의 `shippingFee`를 사전 표시용으로 사용하되 요청 금액으로 보내지 않는다. prepare가 현재 설정을 다시 읽어 확정하고 주문에 스냅샷으로 저장한다.
  - 직접 주문과 장바구니 주문 모두 `ACTIVE` 상품만 확정한다. 판매 중지 상품은 재고가 남아 있어도 `400 INVALID_INPUT`으로 거절한다.
  - 회원 장바구니는 `cartCheckout=true`를 지정한다. 이때 서버는 클라이언트의 `items`를 사용하지 않고 장바구니에서 구매 가능한 항목을 확정한다.
  - 비회원 경로(`HG_SESSION` 없음)는 payload에 `phone/verificationCode/name`이 모두 채워져 있어야 한다 (`PASS` 제외 — 8회권은 회원 전용).
  - prepare 응답의 `orderId`는 Toss 결제창에 그대로 전달한다.

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
  "paymentMethod": "CARD"      // CARD | EASY_PAY (BANK_TRANSFER 거절)
}

// BOOKING (8회권 사용 예약 — 회원 전용, amount=0)
{
  "type": "BOOKING",
  "userId": 7,
  "slotId": 42,
  "passId": 9
}

// PASS (8회권 구매 — 회원 전용)
{
  "type": "PASS",
  "userId": 7
}
```

- 8회권 사용 예약은 회원이 예약 가능 슬롯을 직접 선택해 한 회차씩 생성하며, 성공할 때마다 크레딧 1회를 차감한다.
- 신규 8회권 구매는 `REGULAR_CRAFT_8` 계획으로 확정한다. 해당 이용권은 클래스의 `passEligible=true`와 비향수 카테고리를 모두 충족해야 예약 prepare가 성공한다.
- 운영자가 8회 일정을 일괄 배정하는 별도 API는 제공하지 않는다.

#### 2.15.2 결제 확정 (confirm)

```http
POST /api/v1/payments/confirm
Cookie: HG_SESSION={sessionToken}   # 회원 경로일 때만
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
  "accessToken": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — prepare 시점 amount와 불일치, payload 인증 정보 불일치
  - `404 NOT_FOUND` — `payment_attempt` 미존재
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
  - 서버는 `payment_attempt.amount`와 요청 `amount`가 일치하지 않으면 `400 INVALID_INPUT`으로 거절한다.
  - 서버는 `PENDING/RETRYABLE -> PROCESSING`을 새 processing token과 함께 짧은 트랜잭션으로 선점한 뒤 DB 트랜잭션 밖에서 PG `confirm`을 호출한다. stale 재선점 뒤 이전 token의 실패 결과는 상태에 반영하지 않지만, 늦게 도착한 PG 성공은 같은 요청임을 재검증한 뒤 `APPROVED`로 화해한다.
  - Toss `Idempotency-Key`는 prepare에서 생성한 `orderId`를 사용하며 같은 결제 재시도에서 변경하지 않는다.
  - Toss 승인 응답의 `paymentKey`, `orderId`는 confirm 요청값과 모두 같아야 한다. 다르면 성공으로 저장하지 않고 같은 멱등키로 재확인 가능한 실패로 처리한다.
  - PG 성공은 별도 트랜잭션으로 `APPROVED`에 저장하고, 이후 도메인 저장과 `CONFIRMED` 전이는 한 트랜잭션으로 처리한다.
  - 이미 `CONFIRMED`인 결제를 같은 인증 주체·금액·paymentKey로 재호출하면 PG와 도메인 생성을 반복하지 않고 최초 `context`, `domainId`, `accessToken`을 그대로 반환한다.
  - 성공 화면은 URL의 동일한 `paymentKey`, `orderId`, `amount`를 유지하고 `PAYMENT_CONFIRM_IN_PROGRESS`, `PAYMENT_CONFIRM_RETRYABLE`, 네트워크 오류 또는 필수 인프라 일시 장애에만 명시적 재확인을 제공한다. `PAYMENT_FAILED`와 `PAYMENT_RECONCILIATION_REQUIRED`처럼 최종 또는 운영자 확인이 필요한 상태에는 재확인을 제공하지 않는다.
  - PG 최종 거절은 `FAILED`, 타임아웃·서킷 오픈 같은 일시 실패는 `RETRYABLE`로 저장한다. `FAILED`로 종결된 결제의 동일 confirm 재호출은 PG를 다시 호출하지 않고 저장된 실패 사유의 `502 PAYMENT_FAILED`를 반환한다.
  - PG 승인 후 도메인 저장이 실패하면 `paymentAttemptId` 기반 보상 환불을 요청하고 기존 환불 자동·수동 복구 경로로 처리한다. amount=0 내부 승인 실패는 외부 결제가 없으므로 보상 환불을 만들지 않는다.
  - PG 승인 상태 또는 보상 환불 요청 저장까지 실패해 `PROCESSING`·`RETRYABLE`·`APPROVED`가 1분 이상 남으면, 서버 배치가 매분 최대 10건을 자동 재개한다. `PROCESSING/RETRYABLE`은 저장된 요청과 같은 `orderId` 멱등키로 PG confirm을 재확인하고, `APPROVED`는 PG 호출 없이 fulfillment를 재개한다. 마지막 복구 시각을 저장해 건별 1분 backoff와 후보 순환을 적용한다. 생성 후 14일이 지난 유료 미확정 PG 호출은 자동·사용자 재승인 모두 막고 `RECONCILIATION_REQUIRED`로 격리하며, PG를 호출하지 않는 0원 결제는 기간과 무관하게 내부 처리를 재개한다. 복구는 저장 payload의 결제 주체를 사용하며 공개 confirm API의 요청·응답 형식은 바뀌지 않는다.
  - confirm 요청 `paymentKey`는 `payment_attempt.payment_key`, PG 승인 응답의 `paymentKey`는 `payment_attempt.confirmed_payment_key`와 생성된 도메인 레코드의 `payment_key`에 저장한다. 이후 환불은 승인 응답의 `paymentKey`를 PG cancel 호출의 원결제 식별자로 사용한다.
  - 환불 이력은 원결제 식별자인 Toss `paymentKey`를 `refunds.payment_key`, 환불 거래 식별자인 Toss cancel `transactionKey`를 `refunds.refund_transaction_key`에 분리해 저장한다. 자동·수동 재처리는 `refunds.payment_key`와 최초 `idempotency_key`를 다시 사용한다.
  - 비회원 경로의 신규 `accessToken`은 HMAC-SHA256 서명과 기본 7일 만료 시각을 포함한다. 주문·예약에는 nonce의 SHA-256 해시만 저장하며, 서명 없는 32자 hex 토큰은 기존 데이터 조회용 폴백으로만 허용한다. 응답 유실 뒤 동일 confirm 재호출을 위해 원문 토큰은 `payment_attempt`에 AES-GCM 암호문으로 저장한다. 회원 경로는 `accessToken=null`.
  - `domainId`는 context에 따라 `orderId`(`ORDER`), `bookingId`(`BOOKING`), `passId`(`PASS`)다.
  - 비회원 휴대폰 인증 실패는 confirm 단계에서 fulfillment가 호출하는 `VerifiedGuestResolver`가 던지는 `400 PHONE_VERIFICATION_FAILED`로 매핑된다.
  - confirm은 행 잠금 아래 30분 유효시간을 다시 확인하고, 경계를 넘긴 `PENDING`을 `CANCELED`로 전이한 뒤 `payload_enc`를 제거하고 `410 PAYMENT_ATTEMPT_EXPIRED`를 반환한다. 매분 배치도 같은 기준으로 confirm을 시작하지 않은 결제를 일괄 정리한다. 만료된 orderId는 새 prepare부터 다시 시작해야 한다.
  - 최종 상태인 결제 시도는 생성 30일 뒤 `payload_enc`와 `fulfilled_access_token_enc`를 제거한다. 이후 같은 orderId로 결과를 다시 요청하면 `410 PAYMENT_RESULT_RETENTION_EXPIRED`를 반환한다. 결제 감사 필드와 복구·대사·보상 진행 상태는 유지한다.

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

### 2.17 local 전용 Dev API

`local` 프로필에서만 등록되는 관리자 dev API다. 운영 프로필에서는 빈이 등록되지 않는다.

#### 2.17.1 환불 실패 재현 훅

- `POST /api/v1/admin/dev/payment/refunds/fail-next`
  - 요청: `{ "reason": "로컬 smoke 강제 환불 실패" }` (본문 생략 가능)
  - 응답: `{ "status": "ARMED", "reason": "..." }`
- `DELETE /api/v1/admin/dev/payment/refunds/fail-next`
  - 응답: `204 No Content`

정책:
- 관리자 Bearer 인증을 통과해야 한다.
- 다음 PG 환불 1건만 실패시키고, 실패 사유는 재시도 검증에 사용한다.

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
- 프론트는 복구 결과와 토큰을 만료 시각까지만 현재 브라우저 탭의 `sessionStorage`에 보관한다. 주문·예약 ID는 URL 쿼리로 전달하고 토큰은 URL에 넣지 않아, 목록 이동과 새로고침 뒤에도 같은 복구 세션을 이어간다.
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
  "phone": "01012345678",
  "postalCode": "06236",
  "addressLine1": "서울시 강남구 테헤란로 1",
  "addressLine2": "2층",
  "businessHours": "화-토 10:00-18:00",
  "mapUrl": "https://map.example.com/workshop",
  "parkingInfo": "건물 뒤편 2대 가능",
  "updatedAt": "2026-07-21T10:00:00"
}
```

- `name`은 필수이고 나머지 안내 필드는 선택값이다. 공개·관리자 응답은 같은 구조를 사용한다.

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
- `GET /api/v1/media/images/{fileName}`은 인증 없이 실제 이미지 MIME으로 반환하고 `Cache-Control: public, max-age=31536000, immutable`을 적용한다.
- 허용된 UUID 파일명 형식이 아니거나 파일이 없으면 `404 NOT_FOUND`다.

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
- HTTP 요청은 `RequestIdFilter`에서 생성된 값을 그대로 내려주고, 배치 실행 오류는 `batch-*` 형식 `requestId`를 사용한다.

### 3.2 HTTP 상태코드 × 에러 코드 목록

| HTTP | 에러 코드 | 발생 상황 |
|------|----------|----------|
| 400 | `INVALID_INPUT` | 요청 바디/파라미터 검증 실패 또는 요청 JSON 형식 오류 |
| 400 | `PHONE_VERIFICATION_FAILED` | 인증 코드 불일치 또는 만료 |
| 400 | `PASSWORD_RESET_FAILED` | 비밀번호 재설정의 계정·전화번호·인증코드 확인 실패 |
| 401 | `UNAUTHORIZED` | 보호된 API에 유효한 관리자 또는 회원 인증 없이 접근 |
| 401 | `INVALID_CREDENTIALS` | 로그인 자격 증명 또는 현재 비밀번호 불일치 |
| 403 | `FORBIDDEN` | 인증은 됐지만 요청 권한이 없거나 CSRF 토큰이 없거나 일치하지 않음 |
| 404 | `NOT_FOUND` | 주문·예약·이용권·상품 미존재 |
| 409 | `ALREADY_REFUNDED` | 이미 환불된 주문에 승인·거절 시도 |
| 409 | `INVENTORY_NOT_ENOUGH` | 재고 차감 시 수량 부족 |
| 409 | `CAPACITY_EXCEEDED` | 슬롯 정원(8명) 초과 예약 시도 |
| 409 | `DUPLICATE_BOOKING` | 동일 예약자 + 동일 슬롯 활성 예약 중복 |
| 409 | `SLOT_NOT_AVAILABLE` | 비활성 슬롯 예약 시도 |
| 409 | `BOOKING_CONFLICT` | 낙관적 락 충돌에 의한 동시 변경 요청 |
| 409 | `PAYMENT_CONFIRM_IN_PROGRESS` | 동일 결제의 confirm 요청이 이미 처리 중 |
| 409 | `PAYMENT_RECONCILIATION_REQUIRED` | PG 승인 여부가 불명확해 운영자 확인이 필요하며 새 결제를 시작하면 안 됨 |
| 409 | `CONFLICT` | 주문 승인/픽업/배치 등 비예약 운영 액션의 충돌 |
| 409 | `LOCAL_PASSWORD_NOT_SET` | 소셜 전용 회원이 현재 비밀번호 변경을 요청 |
| 409 | `PHONE_ALREADY_IN_USE` | 회원가입 또는 휴대폰 변경 번호를 다른 회원이 이미 사용 중 |
| 410 | `PAYMENT_ATTEMPT_EXPIRED` | 결제 준비 후 30분 안에 confirm을 시작하지 않음 |
| 410 | `PAYMENT_RESULT_RETENTION_EXPIRED` | 최종 결제 결과의 30일 재조회 보존 기간이 지남 |
| 429 | `TOO_MANY_REQUESTS` | 처리율 제한 초과 |
| 422 | `REFUND_NOT_ALLOWED` | 취소 보상 마감 이후 환불 요청 |
| 422 | `PRODUCTION_REFUND_NOT_ALLOWED` | 제작 시작 후 주문 거절/일반 환불 시도 |
| 422 | `CHANGE_NOT_ALLOWED` | 슬롯 시작 1시간 이내 변경 요청 |
| 422 | `PASS_EXPIRED` | 만료된 8회권으로 예약 또는 전체 환불 시도 |
| 422 | `PASS_CREDIT_INSUFFICIENT` | 잔여 크레딧 0인 8회권으로 예약 시도 |
| 422 | `PASS_NOT_APPLICABLE` | 이용권 계획이 선택 클래스 카테고리 또는 `passEligible` 조건을 충족하지 않음 |
| 422 | `CLASS_INACTIVE` | 비활성 클래스로 슬롯 생성 또는 예약·결제 시도 |
| 422 | `PAYMENT_METHOD_NOT_ALLOWED` | 계좌이체(`BANK_TRANSFER`)로 예약금 결제 시도 |
| 422 | `PHONE_VERIFICATION_REQUIRED` | 회원 휴대폰이 없거나 소유 확인이 완료되지 않아 결제를 시작할 수 없음 |
| 422 | `PASSWORD_UNCHANGED` | 현재와 같은 비밀번호로 변경·재설정 시도 |
| 422 | `ACCOUNT_WITHDRAWAL_BLOCKED` | 진행 중인 거래·환불 또는 사용 가능한 8회권이 있어 탈퇴할 수 없음 |
| 500 | `INTERNAL_ERROR` | 서버 내부 처리 오류 또는 내부 JSON 직렬화/역직렬화 실패 |
| 502 | `PAYMENT_FAILED` | PG가 결제 확정(`/payments/confirm`)을 최종 거절 |
| 503 | `PAYMENT_CONFIRM_RETRYABLE` | PG 결제 확정 결과를 같은 결제 정보로 재확인할 수 있는 일시 실패 |
| 503 | `SERVICE_UNAVAILABLE` | fail-closed 처리율 제한 저장소 장애 또는 인증 SMS 등 필수 외부 작업을 시작·완료할 수 없음 |

---

문서 끝.

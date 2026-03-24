# spec.md
happyGallery API 계약 기준 문서

상태:
- 현재 구현 기준의 요청/응답 계약과 운영용 에러 코드를 정리한다.
- 비즈니스 규칙 원문은 `docs/PRD/0001_기준_스펙/spec.md`를 우선 기준으로 본다.
- 기술 결정 배경은 `docs/ADR/` 문서를 우선 기준으로 본다.

---

## 0. 문서 목적

- 클라이언트와 서버가 맞춰야 하는 HTTP 계약을 한 곳에 모은다.
- core PRD에서 분리된 요청/응답 예시와 에러 포맷을 유지한다.
- 현재 운영 중인 v1 기준 API의 기본 계약을 문서화한다.

---

## 1. 공통 계약

### 1.1 API 버전 정책

- 기본 전략: `URI Versioning`
  - 표준 경로: `/api/v1/**`
  - 예시: `/api/v1/bookings`, `/api/v1/admin/orders`
- 호환 정책:
  - 기존 무버전 경로(`/bookings`, `/passes`, `/products`, `/admin/**`)는 구버전 클라이언트 호환을 위해 한시 유지한다.
  - 신규 기능 추가와 문서화는 `/api/v1/**`를 기준으로 한다.
  - 브레이킹 변경은 `/api/v2/**`로 분리하고, `/api/v1/**`는 공지된 deprecate 기간 이후 제거한다.

### 1.2 관리자 인증 정책

#### 프로덕션 인증

- 관리자 로그인 API를 통해 사용자명/비밀번호 기반으로 인증한다.
- 로그인 성공 시 UUID 세션 토큰을 발급하고, 이후 요청에 `Authorization: Bearer {token}` 헤더를 사용한다.
- 세션 만료: 8시간
- 세션 저장소는 Redis 기반 `AdminSessionStore`를 사용한다. 여러 인스턴스가 떠 있어도 같은 세션을 본다.

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
  - `{ "code": "UNAUTHORIZED", "message": "아이디 또는 비밀번호가 올바르지 않습니다." }`

```http
POST /api/v1/admin/auth/logout
Authorization: Bearer {token}
```

- 성공: `204 No Content`

#### API Key 폴백

- **기본값은 `enable-api-key-auth=false`, `apiKey=""`** 이다. 프로덕션에서 설정이 빠져도 API Key 경로는 비활성 상태를 유지한다.
- `local` 프로필에서만 `enable-api-key-auth=true`와 `ADMIN_API_KEY`를 명시적으로 설정한다.
- 기본 관리자 계정은 Flyway migration에 포함하지 않고, `LocalAdminSeedService`(`@Profile("local")`)로 local 환경에서만 seed한다.
- 인증키 소스: 서버 설정 `app.admin.api-key`, 환경 변수 `ADMIN_API_KEY`
- 주문 승인/거절/제작 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.
  - API Key 폴백 경로와 배치 이력은 `decided_by_admin_id = null`일 수 있다.

#### 적용 대상

- `/api/v1/admin/**` (로그인/로그아웃 제외)
- 레거시 `/admin/**`

#### 인증 실패 응답

- `401 UNAUTHORIZED`
- `{ "code": "UNAUTHORIZED", "message": "관리자 인증이 필요합니다." }`

---

## 2. API 카탈로그

### 2.1 Admin API — 슬롯 관리

#### 2.1.1 슬롯 생성

```http
POST /api/v1/admin/slots
Content-Type: application/json
Authorization: Bearer {token}

{
  "classId": 1,
  "startAt": "2026-03-01T10:00:00",
  "endAt": "2026-03-01T12:00:00"
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
  "isActive": true
}
```

- 성공: `201 Created`
- 에러:
  - `404 NOT_FOUND` — classId에 해당하는 클래스 없음
  - `400 INVALID_INPUT` — 동일 classId + startAt 슬롯 이미 존재

#### 2.1.2 슬롯 비활성화

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
  "isActive": false
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — slotId에 해당하는 슬롯 없음

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
    "price": 39000,
    "available": true
  }
]
```

- 성공: `200 OK`
- 정책:
  - `ACTIVE` 상태 상품만 노출한다.
  - 응답은 상품 상세 조회와 동일한 필드 구조를 사용한다.
  - 재고 수량 원문은 노출하지 않고 `available`만 공개한다.

#### 2.2.2 공개 상품 상세 조회

```http
GET /api/v1/products/{id}
```

```json
{
  "id": 1,
  "name": "시그니처 캔들",
  "type": "READY_STOCK",
  "price": 39000,
  "available": true
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — productId 미존재

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
    "bufferMin": 30
  }
]
```

- 성공: `200 OK`
- 정책:
  - 현재 등록된 전체 클래스를 반환한다.
  - 프론트 예약 생성 화면은 이 응답을 기준으로 클래스 선택지를 구성한다.

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
  - `is_active = true` 이고 `booked_count < capacity` 인 슬롯만 노출한다.
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
  "price": 39000,
  "quantity": 5
}
```

```json
{
  "id": 1,
  "name": "시그니처 캔들",
  "type": "READY_STOCK",
  "price": 39000,
  "status": "ACTIVE",
  "available": true,
  "quantity": 5
}
```

- 성공: `201 Created`
- 에러:
  - `400 INVALID_INPUT` — 이름/유형/가격/수량 검증 실패
  - `401 UNAUTHORIZED` — 관리자 인증 실패

#### 2.3.2 ACTIVE 상품 목록 조회

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
- 정책:
  - 인증 코드는 응답에 포함하지 않고 서버 로그에만 기록한다.
  - 개발/테스트 환경에서는 `GET /api/v1/admin/dev/phone-verifications/latest?phone=` 로 코드를 조회할 수 있다.

#### 2.4.2 게스트 예약 생성

```http
POST /api/v1/bookings/guest

{
  "phone": "01012345678",
  "verificationCode": "483921",
  "name": "홍길동",
  "slotId": 42,
  "depositAmount": 5000,
  "paymentMethod": "CARD"
}
```

```json
{
  "bookingId": 1,
  "bookingNumber": "BK-00000001",
  "accessToken": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
  "slotId": 42,
  "status": "BOOKED",
  "depositAmount": 5000,
  "balanceAmount": 45000,
  "className": "향수 클래스"
}
```

- 성공: `201 Created`
- 에러:
  - `400 PHONE_VERIFICATION_FAILED` — 코드 불일치 또는 만료(5분)
  - `404 NOT_FOUND` — slotId 미존재
  - `409 CAPACITY_EXCEEDED` — 슬롯 정원 초과
  - `409 DUPLICATE_BOOKING` — 동일 전화번호 + 동일 슬롯 중복
  - `409 SLOT_NOT_AVAILABLE` — 비활성 슬롯 예약 시도
  - `422 PAYMENT_METHOD_NOT_ALLOWED` — `BANK_TRANSFER` 사용 시도
- 정책:
  - 휴대폰 인증 성공 시 전화번호 기준 Guest를 조회하고 없으면 생성한다.
  - `accessToken`(32자 hex)은 생성 응답에서 1회만 반환되며, DB에는 SHA-256 해시만 저장된다.
  - 비회원 예약은 예약금 결제만 허용한다.

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
  "guestPhone": "010****5678"
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — bookingId 미존재 또는 token 불일치

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
  - `409 DUPLICATE_BOOKING` — 동일 전화번호 + 새 슬롯 이미 예약
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
  "refundable": true
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — bookingId 미존재 또는 token 불일치
  - `400 INVALID_INPUT` — `BOOKED` 상태가 아닌 예약 취소 시도
- 환불 정책:
  - 예약금 결제: `refundable=true`이면 PG 환불 요청
  - 8회권 결제: `refundable=true`이면 `REFUND` ledger와 remaining credit 복구
  - `refundable=false`이면 크레딧 소멸 유지

### 2.5 8회권 API

#### ~~2.5.1 게스트 8회권 구매~~ (2026-03-19 제거)

> 8회권 구매는 회원 전용으로 전환됨. `POST /api/v1/me/passes` 참조.
> guest 소유 8회권 상태는 지원하지 않는다.

#### ~~2.5.2 휴대폰 인증 기반 8회권 구매~~ (2026-03-19 제거)

> 상동. 회원 8회권 구매는 `POST /api/v1/me/passes`로 단일화.

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

```json
{
  "canceledBookings": 2,
  "refundCredits": 6,
  "refundAmount": 240000
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — passId 미존재
- 정책:
  - 미래 `BOOKED` 예약 자동 취소
  - `REFUND` ledger 기록 후 `remaining_credits = 0`
  - 실제 PG 환불은 `refundAmount`를 참고해 관리자가 수동 처리
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
  - 만료된 pass의 `remaining_credits = 0`, `EXPIRE` ledger 기록
  - `failureReasons`는 내부 예외명을 그대로 노출하지 않고 `CONFLICT`, `NOT_FOUND`, `ALREADY_PROCESSED`, `BUSINESS_ERROR`, `INTERNAL_ERROR`로 정규화한다.

### 2.6 사용자 주문 API

#### 2.6.1 주문 생성

```http
POST /api/v1/orders

{
  "phone": "01012345678",
  "verificationCode": "483921",
  "name": "홍길동",
  "items": [
    { "productId": 1, "qty": 2 },
    { "productId": 3, "qty": 1 }
  ]
}
```

```json
{
  "orderId": 12,
  "accessToken": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
  "status": "PAID_APPROVAL_PENDING",
  "totalAmount": 118000,
  "paidAt": "2026-03-08T20:30:00"
}
```

- 성공: `201 Created`
- 에러:
  - `400 PHONE_VERIFICATION_FAILED` — 인증 코드 불일치 또는 만료
  - `404 NOT_FOUND` — productId 미존재
  - `409 INVENTORY_NOT_ENOUGH` — 재고 부족
- 정책:
  - 휴대폰 인증 성공 시 전화번호 기준 Guest를 조회하고 없으면 생성한다.
  - 주문 생성 시 `accessToken`(32자 hex)을 1회 발급한다. DB에는 SHA-256 해시만 저장되므로, 이 응답 이후에는 원본 토큰을 복구할 수 없다.
  - 주문 아이템 단가는 서버가 다시 조회해 확정한다.
  - 생성 직후 상태는 `PAID_APPROVAL_PENDING`이고 승인 마감은 결제 시각 + 24시간이다.

#### 2.6.2 주문 상세 조회

```http
GET /api/v1/orders/{orderId}
X-Access-Token: {accessToken}
```

```json
{
  "orderId": 12,
  "status": "PAID_APPROVAL_PENDING",
  "totalAmount": 118000,
  "paidAt": "2026-03-08T20:30:00",
  "approvalDeadlineAt": "2026-03-09T20:30:00",
  "items": [
    { "productId": 1, "qty": 2, "unitPrice": 39000 },
    { "productId": 3, "qty": 1, "unitPrice": 40000 }
  ],
  "fulfillment": {
    "type": "SHIPPING",
    "expectedShipDate": null,
    "pickupDeadlineAt": null
  }
}
```

- 성공: `200 OK`
- 에러:
  - `404 NOT_FOUND` — orderId 미존재 또는 token 불일치
- 정책:
  - `X-Access-Token` 헤더의 토큰을 SHA-256 해시하여 DB 저장값과 비교한다.
  - `fulfillment`는 아직 생성되지 않은 경우 `null`일 수 있다.

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
      "orderNumber": "HG-20260324-00123",
      "status": "PAID_APPROVAL_PENDING",
      "totalAmount": 118000,
      "paidAt": "2026-03-24T11:32:10",
      "createdAt": "2026-03-24T11:32:10"
    }
  ],
  "nextCursor": "MjAyNi0wMy0yNFQxMTozMjoxMHwxMjM",
  "hasMore": true
}
```

- 성공: `200 OK`
- 정책:
  - 상태 필터가 없으면 전체 주문을 `createdAt DESC, id DESC` 기준으로 조회한다.
  - `cursor`는 `Base64("{ISO_LOCAL_DATE_TIME}|{id}")` 형식이다.
  - 프론트는 `hasMore=true`일 때만 `nextCursor`로 다음 페이지를 요청한다.

#### 2.7.2 주문 운영 엔드포인트

- `POST /api/v1/admin/orders/{id}/approve`
  - 응답: `200 OK` 본문 없음
  - 정책:
    - `PAID_APPROVAL_PENDING`만 승인 가능
    - `MADE_TO_ORDER` 상품이 있으면 `IN_PRODUCTION`, 아니면 `APPROVED_FULFILLMENT_PENDING`으로 전이
    - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다
- `POST /api/v1/admin/orders/{id}/reject`
  - 응답: `200 OK` 본문 없음
  - 정책:
    - 승인 대기 주문만 거절 가능
    - 재고 복구 + 환불 실행 + `REJECTED` 전이
- `PATCH /api/v1/admin/orders/{id}/expected-ship-date`
  - 요청: `{ "expectedShipDate": "2026-04-15" }`
  - 응답: `{ "orderId": 5, "status": "IN_PRODUCTION", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - `IN_PRODUCTION`, `DELAY_REQUESTED`, `SHIPPING_PREPARING` 상태에서만 설정 가능
    - `SHIPPING` 타입 fulfillment에서만 설정 가능 (`PICKUP` 타입은 400)
- `POST /api/v1/admin/orders/{id}/delay`
  - 응답: `{ "orderId": 5, "status": "DELAY_REQUESTED", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - `IN_PRODUCTION`에서만 지연 요청 가능
- `POST /api/v1/admin/orders/{id}/resume-production`
  - 응답: `{ "orderId": 5, "status": "IN_PRODUCTION", "expectedShipDate": "2026-04-15" }`
  - 정책:
    - `DELAY_REQUESTED`에서만 제작 재개 가능
    - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다
- `POST /api/v1/admin/orders/{id}/prepare-pickup`
  - 요청: `{ "pickupDeadlineAt": "2026-04-16T18:00:00" }`
  - 응답: `{ "orderId": 5, "status": "PICKUP_READY", "pickupDeadlineAt": "2026-04-16T18:00:00" }`
- `POST /api/v1/admin/orders/{id}/complete-pickup`
  - 응답: `{ "orderId": 5, "status": "PICKED_UP", "pickupDeadlineAt": "2026-04-16T18:00:00" }`

공통 에러:
- `401 UNAUTHORIZED` — 관리자 인증 실패
- `404 NOT_FOUND` — orderId 또는 관련 fulfillment 미존재
- `400 INVALID_INPUT` — 허용되지 않은 상태 전이
- `409 ALREADY_REFUNDED` — 자동환불/거절 완료 주문에 대한 승인·거절 재시도

#### 2.7.3 픽업 만료 배치 수동 트리거

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
  - 성공 건은 `PICKUP_EXPIRED`로 전이하고 환불/재고 복구를 수행한다.
  - `failureReasons`는 내부 예외명을 그대로 노출하지 않고 `CONFLICT`, `NOT_FOUND`, `ALREADY_PROCESSED`, `BUSINESS_ERROR`, `INTERNAL_ERROR`로 정규화한다.

#### 2.7.4 제작 완료

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
  - `400 INVALID_INPUT` — `IN_PRODUCTION` 또는 `DELAY_REQUESTED` 상태가 아닌 주문
- 정책:
  - `IN_PRODUCTION` 또는 `DELAY_REQUESTED` → `APPROVED_FULFILLMENT_PENDING`
  - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.
  - API Key 폴백 경로는 `null`일 수 있다.

#### 2.7.5 배송 흐름

```http
POST /api/v1/admin/orders/{id}/prepare-shipping
POST /api/v1/admin/orders/{id}/mark-shipped
POST /api/v1/admin/orders/{id}/mark-delivered
Authorization: Bearer {token}
```

```json
{ "orderId": 5, "status": "SHIPPING_PREPARING", "expectedShipDate": "2026-04-15" }
```

```json
{ "orderId": 5, "status": "SHIPPED", "expectedShipDate": "2026-04-15" }
```

```json
{ "orderId": 5, "status": "DELIVERED", "expectedShipDate": "2026-04-15" }
```

- 정책:
  - `APPROVED_FULFILLMENT_PENDING` → `SHIPPING_PREPARING` → `SHIPPED` → `DELIVERED` 순서만 허용한다.
  - 각 전이는 `order_approvals` 이력에 `PREPARE_SHIPPING`, `SHIP`, `DELIVER`로 기록한다.
  - 이력의 adminId는 Bearer 세션에서 검증된 관리자 ID를 사용한다.

#### 2.7.6 주문 결정 이력 조회

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
  - 결정 시간 순으로 정렬된 전체 이력을 반환한다.
  - `decision`: `APPROVE`, `REJECT`, `DELAY`, `AUTO_REFUND`, `PRODUCTION_COMPLETE`, `RESUME_PRODUCTION`, `PREPARE_SHIPPING`, `SHIP`, `DELIVER`

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
    "bookerType": "GUEST",
    "bookerName": "홍길동",
    "bookerPhone": "010****5678",
    "className": "향수 클래스",
    "slotStart": "2026-03-20T10:00:00",
    "slotEnd": "2026-03-20T12:00:00",
    "status": "BOOKED",
    "depositAmount": 5000,
    "balanceAmount": 45000
  }
]
```

- 성공: `200 OK`
- 정책:
  - `bookerType`은 `GUEST` 또는 `MEMBER`로 구분한다.
  - guest claim 이후 `userId`가 설정된 예약은 `MEMBER`로 표시한다.
  - User 정보는 batch fetch(`UserReaderPort.findAllById`)로 조합한다.
  - `date` 필수, `status`는 선택(미입력 시 전체).

### 2.9 환불 실패 관리 Admin API

#### 2.8.1 환불 실패 목록 조회

```http
GET /api/v1/admin/refunds/failed
Authorization: Bearer {token}
```

```json
[
  {
    "refundId": 42,
    "bookingId": 15,
    "orderId": null,
    "amount": 5000,
    "failReason": "PG 타임아웃",
    "createdAt": "2026-03-01T14:30:00"
  }
]
```

- 성공: `200 OK`

#### 2.8.2 환불 재시도

```http
POST /api/v1/admin/refunds/{refundId}/retry
Authorization: Bearer {token}
```

- 성공: `200 OK` (본문 없음)
- 에러:
  - `404 NOT_FOUND` — refundId 미존재
  - `400 INVALID_INPUT` — `FAILED` 상태가 아닌 환불 재시도
- 정책:
  - `FAILED` 상태 환불만 재시도 가능하다.
  - 성공 시 `SUCCEEDED`, 재실패 시 `FAILED` 유지
  - 환불 실행/실패 이력 저장은 부모 주문/예약 트랜잭션과 분리된 `REQUIRES_NEW` 트랜잭션으로 처리한다.

### 2.10 회원 API (`/api/v1/me`)

회원 인증은 `HG_SESSION` HttpOnly 쿠키 기반이며, `CustomerAuthFilter`에서 검증한다.

#### 2.10.1 회원 예약 생성

```http
POST /api/v1/me/bookings
Cookie: HG_SESSION={sessionToken}

{
  "slotId": 42,
  "depositAmount": 5000,
  "paymentMethod": "CARD",
  "passId": 7
}
```

- 성공: `201 Created`
- 에러: 게스트 예약 생성과 동일한 슬롯/정원/8회권 에러 + `401 UNAUTHORIZED`
- 정책:
  - 회원 예약은 access token을 발급하지 않는다.
  - `passId`는 해당 회원 소유 8회권만 사용 가능하다.

#### 2.10.2 회원 주문 생성

```http
POST /api/v1/me/orders
Cookie: HG_SESSION={sessionToken}

{
  "items": [
    { "productId": 1, "qty": 2 }
  ]
}
```

- 성공: `201 Created`
- 에러: 게스트 주문 생성과 동일한 재고/상품 에러 + `401 UNAUTHORIZED`
- 정책:
  - 회원 주문은 access token을 발급하지 않는다.
  - 아이템 단가는 서버가 조회해 확정한다(`OrderCreationService.resolveItemPrices`).

#### 2.10.3 회원 목록/상세 조회

- `GET /api/v1/me/bookings` — 회원 예약 목록
- `GET /api/v1/me/bookings/{id}` — 회원 예약 상세
- `GET /api/v1/me/orders` — 회원 주문 목록
- `GET /api/v1/me/orders/{id}` — 회원 주문 상세
- `GET /api/v1/me/passes` — 회원 8회권 목록
- `GET /api/v1/me/passes/{id}` — 회원 8회권 상세

공통 정책:
- 인증 실패 시 `401 UNAUTHORIZED`
- 다른 회원의 리소스 접근 시 `404 NOT_FOUND`

#### 2.10.4 회원 상품 Q&A 작성

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
  - `404 NOT_FOUND` — 상품 미존재
- 정책:
  - 작성 주체는 회원(User)만 허용한다.
  - `secret=true`일 때 비밀번호를 설정해 공개 상세 조회 전 검증한다.
  - 응답에는 작성 결과 요약만 반환한다.

#### 2.10.5 회원 1:1 문의 작성/조회

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

### 2.11 공개 Product Q&A API

#### 2.11.1 상품 Q&A 목록 조회

```http
GET /api/v1/products/{productId}/qna
```

- 성공: `200 OK`
- 정책:
  - 작성자 이름은 마스킹해 반환한다.
  - `secret=true`인 글은 제목을 `[비밀글입니다]`로 가려서 반환한다.
  - 공개 목록에는 본문/답변 전문을 포함하지 않는다.

#### 2.11.2 비밀글 비밀번호 검증 후 상세 조회

```http
POST /api/v1/products/{productId}/qna/{id}/verify

{
  "password": "1234"
}
```

- 성공: `200 OK`
- 에러:
  - `400 INVALID_INPUT` — 비밀번호 불일치
  - `404 NOT_FOUND` — Q&A 미존재
- 정책:
  - 비밀글이 아니면 그대로 상세를 반환한다.
  - 비밀번호가 일치하면 제목/본문/답변을 포함한 상세를 반환한다.

### 2.12 관리자 Q&A / 문의 API

#### 2.12.1 관리자 상품 Q&A 조회/답변

- `GET /api/v1/admin/qna?productId={productId}` — 특정 상품의 Q&A 목록 조회
- `POST /api/v1/admin/qna/{id}/reply` — Q&A 답변 등록

정책:
- 인증: `Authorization: Bearer {token}`
- 답변 작성 시 `replyContent`, `repliedAt`, `repliedBy`를 기록한다.
- 이미 답변이 있는 글에 재답변을 시도하면 서버가 거절한다.

#### 2.12.2 관리자 1:1 문의 조회/답변

- `GET /api/v1/admin/inquiries` — 전체 문의 목록 조회
- `GET /api/v1/admin/inquiries/{id}` — 문의 상세 조회
- `POST /api/v1/admin/inquiries/{id}/reply` — 문의 답변 등록

정책:
- 인증: `Authorization: Bearer {token}`
- 회원 이름을 함께 반환한다.
- 이미 답변이 있는 문의에 재답변을 시도하면 서버가 거절한다.

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
| 400 | `INVALID_INPUT` | 요청 바디/파라미터 검증 실패 |
| 400 | `PHONE_VERIFICATION_FAILED` | 인증 코드 불일치 또는 만료 |
| 404 | `NOT_FOUND` | 주문·예약·이용권·상품 미존재 |
| 409 | `ALREADY_REFUNDED` | 이미 자동환불된 주문에 승인 시도 |
| 409 | `INVENTORY_NOT_ENOUGH` | 재고 차감 시 수량 부족 |
| 409 | `CAPACITY_EXCEEDED` | 슬롯 정원(8명) 초과 예약 시도 |
| 409 | `DUPLICATE_BOOKING` | 동일 전화번호 + 동일 슬롯 중복 예약 |
| 409 | `SLOT_NOT_AVAILABLE` | 비활성 슬롯 예약 시도 |
| 409 | `BOOKING_CONFLICT` | 낙관적 락 충돌에 의한 동시 변경 요청 |
| 409 | `CONFLICT` | 주문 승인/픽업/배치 등 비예약 운영 액션의 충돌 |
| 429 | `TOO_MANY_REQUESTS` | 처리율 제한 초과 |
| 422 | `REFUND_NOT_ALLOWED` | D-1 00:00 이후 환불 요청 |
| 422 | `PRODUCTION_REFUND_NOT_ALLOWED` | 제작 시작 후 주문 거절/환불 시도 |
| 422 | `CHANGE_NOT_ALLOWED` | 슬롯 시작 1시간 이내 변경 요청 |
| 422 | `PASS_EXPIRED` | 만료된 8회권으로 예약 시도 |
| 422 | `PASS_CREDIT_INSUFFICIENT` | 잔여 크레딧 0인 8회권으로 예약 시도 |
| 422 | `PAYMENT_METHOD_NOT_ALLOWED` | 계좌이체(`BANK_TRANSFER`)로 예약금 결제 시도 |

---

문서 끝.

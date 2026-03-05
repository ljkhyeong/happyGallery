# spec.md
공방 연동 쇼핑몰 + 체험 예약 시스템 스펙

## 0. 문서 목적
- 요구사항을 **명확히** 하고, 팀/미래의 나에게 **기준선(단일 진실)** 을 제공한다.
- 설계 의사결정(Architecture Decision)과 데이터 모델을 고정해, 구현 중 “감으로 변경”되는 일을 줄인다.
- “가치 있는 테스트” 중심으로 핵심 리스크를 커버한다.

---

## 1. 프로젝트 개요
### 1.1 도메인
- 오프라인 공방(실매장)과 **온라인 쇼핑몰 재고를 공유**한다.
- 판매 대상:
    - 공예품(목공예, pop글씨, 뜨개질 등): 주로 **단일 작품(수량 1)**, 일부는 **예약 제작**
- 체험 대상:
    - 조향: **원데이 클래스만**
    - 기타 공예: **원데이 클래스 + 정규과정(월 8회권)**
- 체험 후 완성품은 가져갈 수 있으나 **향수/공예 완성품을 온라인에서 별도 판매하지는 않음**(체험 결과물).

### 1.2 핵심 목표
- 온라인/오프라인 충돌을 운영 정책(승인/거절/환불)로 흡수하면서도,
- 고객 경험(예약, 알림, 환불 규칙)을 일관되게 제공한다.

---

## 2. 용어 정의
- POS: 오프라인 결제/재고 관리 시스템(본 프로젝트는 오프라인 시스템과 직접 연동 여부 미정, 매장 우선권 정책으로 흡수)
- SKU: 재고를 구분하는 단위 코드(단일 작품도 “상품 1건 = SKU 1개”로 취급 가능)
- 슬롯(Slot): 체험 예약 시간 단위(시간 슬롯 방식)
- 예약금(Deposit): 체험 예약 시 결제하는 금액(클래스 가격의 10%)
- 잔금(Balance): 체험 당일 현장 결제 금액(시작 전 결제가 원칙이나 시스템 강제 없음)
- D-1: 체험 전날(환불 규칙 경계는 **전날 00:00, Asia/Seoul 기준**)

---

## 3. 비즈니스 규칙(확정)
### 3.1 공예품 주문(온라인/오프라인 재고 공유)
- 재고는 온라인/오프라인 **하나로 공유**한다.
- 재고 차감 타이밍: **결제 완료 시 즉시 차감**
- 오프라인 우선권:
    - 온라인 결제 완료 후라도, 사장님이 판매 가능 여부 확인 후 **승인**한다.
    - 판매 불가(재고 오류/오프라인 선판매 등)면 **거절 → 전액 자동 환불 → 재고 복구**
    - 고객이 원하면 “배송 지연”으로 전환 가능(관리자 액션)
- 승인 SLA:
    - 결제 후 **24시간 내 승인/거절**
    - 24시간 초과 시 **자동 환불 + 재고 복구**
    - 자동 환불된 주문을 승인 시도하면 서버가 **409(Conflict)** 로 거절하고 “이미 자동환불됨”을 표시

### 3.2 예약 제작 주문(공예품)
- 결제 후 승인 즉시 **제작 시작**
- 제작 시작 이후 **환불 불가**
- 예상 출고일(배송 예정일)은 관리자 화면에서 설정하고 고객에게 표시
- “배송 지연”은 별도 상태로 관리(`DELAY_REQUESTED`)

### 3.3 픽업(기존 재고)
- 택배/픽업 모두 제공
- 픽업 규칙(기존 재고):
    - 승인 후 픽업 준비
    - 매장 **마감 2시간 전 알림**
    - 마감까지 미수령 시 **자동 결제 취소(환불) + 재고 복구**
- 픽업 규칙(예약 제작):
    - 제작 시작 이후 환불 불가(미수령 자동환불 대상 아님)

---

## 4. 체험(예약) 규칙(확정)
### 4.1 슬롯/정원
- 예약은 **시간 슬롯 방식**
- 슬롯당 최대 정원: **8명**
- 클래스별 소요 시간은 다를 수 있음
- 버퍼(정리/준비):
    - 공통 규칙, **뒤쪽 버퍼만 적용**
    - 단위는 **30분** (`classes.buffer_min = 30`)
    - 예약이 잡힌 경우에만 다음 시작 슬롯을 자동 비활성화(버퍼만큼)
    - 이미 예약된 슬롯이 있으면 기존 예약은 유지하고 “다음부터” 적용
    - **비활성화 대상 범위**: `[슬롯 종료 시각, 슬롯 종료 시각 + buffer_min)` — 시작 포함(inclusive), 끝 미포함(exclusive)
      - 예: 슬롯 10:00~12:00, buffer_min=30 → `start_at ∈ [12:00, 12:30)` 인 슬롯 비활성화
      - 12:30에 시작하는 슬롯은 **비활성화 대상 아님**
- **동시성**: 예약 확정 시 `slots` row를 `SELECT FOR UPDATE`로 잠근 뒤 `booked_count`를 증가한다 (ADR-0003)

### 4.2 예약금/환불/변경
- 예약금: 클래스 가격의 **10%**
- 예약 확정: 슬롯이 비어있으면 **자동 확정**
- 환불 규칙(예약금):
    - 체험 전날 **00:00 이후 환불 불가**
- 변경 규칙:
    - 기본: D-1까지 고객 변경 가능
    - 당일: 시작 **1시간 전까지 변경 가능**, 이후는 1회 소모 처리
    - 변경 횟수 제한 없음(단, 시스템 보호를 위한 rate limit은 기술적으로 적용 가능)

### 4.3 잔금(현장 결제)
- 잔금은 수업 시작 전 결제가 원칙
- 다만 시스템이 강제하지 않음
    - 운영자가 **미수금**으로 유지 가능

---

## 5. 정규과정(월 8회권) 규칙(확정)
### 5.1 결제/유효기간/이월
- 월 8회권은 **월 전체 선결제**
- 크레딧 모델로 관리(총 8회)
- 유효기간: **결제일 기준 90일**
- 만료 7일 전 알림 발송
- 만료 시 남은 크레딧은 **즉시 소멸(환불 없음)**

### 5.2 예약 운영
- 기본은 사장님이 8회치를 배정(예약 생성)
- 고객은 “예약 가능 시간” 범위에서 변경 가능
- 보강 없음:
    - 결석 또는 변경 불가(시작 1시간 전 이후)는 **1회 소모**

### 5.3 환불
- 남은 횟수 정산 환불 가능(잔여 크레딧 기반)
- 환불 시 고객의 **미래 예약은 자동 취소**

---

## 6. 번들 결제(상품 + 예약)
- 상품 주문과 체험 예약금을 **한 번에 결제**할 수 있음
- 기본 정책: 번들은 **전체 취소(함께 취소)** 가 원칙
- 예외: 관리자는 “부분 취소/예외 처리” 가능
- 번들에서 상품이 거절되어 전체 취소될 경우:
    - 체험 예약도 **즉시 슬롯 해제** + 예약금 환불 처리

---

## 7. 알림 정책(확정)
- 채널 우선순위:
    1) 카카오
    2) 실패 시 SMS
    3) 이메일(영수증/보조)
    4) 앱 푸시(리마인드)
- 발송 이벤트:
    - 예약 완료, 예약 변경
    - D-1 리마인드, 당일 아침 리마인드
    - 결제 영수증(예약금/주문 결제/환불)

---

## 8. 아키텍처 결정(Architecture Decisions)
### 8.1 시스템 형태
- 1차 목표는 빠르고 확실한 운영이므로 **모놀리식(단일 서비스)** 로 시작
- 도메인 경계를 코드 구조로 분리해 확장 가능하게 구성

추천 모듈(패키지) 경계:
- `catalog` (상품/재고)
- `order` (주문/승인/환불/픽업/배송/제작)
- `booking` (슬롯/예약/이력/정원)
- `pass` (8회권/크레딧/만료/정산환불)
- `payment` (PG 연동 추상화, 결제/환불)
- `notification` (카카오/SMS/이메일/푸시, fallback, 로그)
- `admin` (운영자 기능)

### 8.2 일관성/동시성 전략
- 재고(단일 작품)와 슬롯 정원(8명)은 **동시성 핵심 구간**
- 원칙:
    - “남은 수량/정원” 업데이트는 DB 트랜잭션 안에서 처리
    - 승인/환불 등 외부 호출(PG)은 상태를 먼저 기록하고 재시도 가능하게 설계

권장 락:
- 단일 재고 작품: 재고 row에 대한 `SELECT ... FOR UPDATE` 또는 버전 기반 낙관적 락
- 슬롯 정원: slot row를 `FOR UPDATE`로 잠그고 예약 카운트를 원자적으로 증가/검증
- 주문 승인/자동환불/픽업 만료 같은 **운영 액션 충돌 가능 구간**은 `orders.version`, `fulfillments.version` 기반 낙관적 락을 사용하고, 충돌 시 제한된 재시도로 흡수한다

### 8.3 상태 머신(핵심)
- 주문: 결제와 승인을 분리한 상태
    - `PAID_APPROVAL_PENDING`
    - `APPROVED_FULFILLMENT_PENDING`
    - `DELAY_REQUESTED`
    - `REJECTED_REFUNDED`
    - `AUTO_REFUNDED_TIMEOUT`
    - 픽업: `PICKUP_READY` → `PICKED_UP` / `PICKUP_EXPIRED_REFUNDED`
    - 제작: `IN_PRODUCTION`(환불 불가 시작점) 등
- 예약: `BOOKED` / `CANCELED` / `NO_SHOW` / `COMPLETED`
    - 결제/미수금은 별도 필드로 분리

### 8.4 비회원 예약 보안
- 비회원 예약은 휴대폰 인증(문자 코드) 필수
- 비회원 예약 조회/변경은 문자로 “예약번호+인증링크” 제공
- 동일 전화번호로 동일 슬롯 중복 예약은 방지

---

## 9. 데이터 모델(초안)
> 구현 시점에는 실제 ERD로 확정한다. 여기서는 “필수 테이블과 핵심 필드”만 고정한다.

### 9.1 사용자/게스트
- `users`
    - id, email, password_hash, name, phone, created_at
- `guests`
    - id, name, phone, phone_verified(boolean), created_at

### 9.2 상품/재고
- `products`
    - id, name, type(READY_STOCK|MADE_TO_ORDER), price, status(ACTIVE|INACTIVE)
- `inventory`
    - product_id(PK/FK), quantity(대부분 1), version, updated_at
    - 단일 작품은 quantity=1
- `made_to_order_spec`
    - product_id(FK), lead_time_hint(optional), refundable_until_state=IN_PRODUCTION

### 9.3 주문/승인/환불/이행
- `orders`
    - id, user_id nullable, guest_id nullable
    - status, total_amount, paid_at, approval_deadline_at, bundle_id nullable, version
- `order_items`
    - id, order_id, product_id, qty, unit_price
- `order_approvals`
    - id, order_id, decided_by_admin_id, decision(APPROVE|REJECT|DELAY), reason, decided_at
- `fulfillments`
    - id, order_id, type(SHIPPING|PICKUP), status, address/pickup_store, expected_ship_date, pickup_deadline_at, version
- `refunds`
    - id, order_id, amount, status(REQUESTED|SUCCEEDED|FAILED), pg_ref, fail_reason, created_at

중요 인덱스:
- `orders(status, approval_deadline_at)`
- `inventory(product_id, version)`
- `refunds(status, created_at)`

### 9.4 클래스/슬롯/예약
- `classes`
    - id, name, category(PERFUME|WOOD|KNIT|...), duration_min, price, buffer_unit=30
- `slots`
    - id, class_id, start_at, end_at
    - capacity=8
    - booked_count
    - is_active(버퍼/운영자 설정 반영)
- `bookings`
    - id, user_id nullable, guest_id nullable
    - class_id, slot_id, status
    - deposit_amount, deposit_paid_at
    - balance_amount, balance_status(UNPAID|PAID), arrears_flag
    - version
- `booking_history`
    - id, booking_id, action(BOOKED|RESCHEDULED|CANCELED|NO_SHOW|COMPLETED)
    - from_slot_id, to_slot_id, actor(CUSTOMER|ADMIN), reason, created_at

동시성 포인트:
- 예약 변경은 `bookings` + `slots.booked_count`가 함께 안전하게 변해야 함(단일 트랜잭션)

### 9.5 8회권(크레딧)
- `pass_purchases`
    - id, user_id/guest_id, purchased_at, expires_at(=purchased_at+90d), total_credits=8, remaining_credits
- `pass_ledger`
    - id, pass_purchase_id
    - type(EARN|USE|REFUND|EXPIRE)
    - amount, related_booking_id nullable, created_at

---

## 10. 테스트 전략(팀 방침 반영)
### 10.1 철학
- 테스트는 “커버리지”가 아니라 **비즈니스 검증/문서화 가치**를 위해 쓴다.
- 전체를 다 테스트하지 않고, **핵심 20%**(주요 사용/변경 지점)로 **80% 신뢰성**을 얻는다.
- 시나리오가 폭증하므로, 적은 양으로 많은 계층을 덮는 **실용적인 통합 테스트**를 우선한다.

### 10.2 테스트 분류(목적 중심)
1) 유스케이스 테스트(통합 테스트 중심)
- 주문 결제 → 승인대기 → 승인/거절/자동환불
- 픽업 마감 자동취소
- 슬롯 정원 8명 동시 예약
- 번들 결제 전체 취소
- 8회권: 결제일 기준 90일, 만료 7일 전 알림, 만료 소멸, 정산 환불

2) 도메인 정책 테스트(빠르고 명확한 단위 테스트)
- D-1 00:00 환불 경계 판정(Asia/Seoul)
- 당일 1시간 전 변경 가능 판정
- 제작 시작 시점 이후 환불 불가 전환
- 잔여 크레딧 정산 환불 계산

3) 직렬화/역직렬화/계약 테스트(필요 시)
- 예약/주문 응답 스키마가 문서와 일치하는지
- 알림 페이로드(카카오/SMS) 구성 규칙이 깨지지 않는지

### 10.3 “작성하지 않기로 한 것”
- 단순 getter/setter, 단순 매핑, 프레임워크 동작을 확인하는 테스트
- 제품 수명이 짧고 수동 검증이 더 효율적인 이벤트성 기능
- 중요 리스크와 무관한 화면 단순 렌더링 수준 테스트(초기에는 제외)

### 10.4 핵심 리스크 기반 최소 테스트 세트(권장)
- 동시성:
    - 단일 재고 동시 주문 충돌: 승인 거절 + 환불 + 재고 복구
    - 슬롯 정원 8명 초과 방지
- 상태 멱등성:
    - 승인/거절 버튼 중복 클릭, 재시도에도 상태 1회 전이
- 시간 경계:
    - D-1 00:00, 당일 1시간 전
- 자동 배치:
    - 승인 SLA 24시간 자동환불
    - 픽업 마감 자동취소
    - 8회권 만료 7일 전 알림 및 만료 소멸

---

## 11. Admin API — 슬롯 관리

### 11.1 슬롯 생성

```
POST /admin/slots
Content-Type: application/json

{
  "classId": 1,
  "startAt": "2026-03-01T10:00:00",
  "endAt":   "2026-03-01T12:00:00"
}

→ 201 Created
{
  "id": 42,
  "classId": 1,
  "startAt": "2026-03-01T10:00:00",
  "endAt":   "2026-03-01T12:00:00",
  "capacity": 8,
  "bookedCount": 0,
  "isActive": true
}
```

에러:
- `404 NOT_FOUND` — classId에 해당하는 클래스 없음
- `400 INVALID_INPUT` — 동일 classId + startAt 슬롯 이미 존재

### 11.2 슬롯 비활성화

```
PATCH /admin/slots/{id}/deactivate

→ 200 OK
{
  "id": 42,
  ...,
  "isActive": false
}
```

에러:
- `404 NOT_FOUND` — slotId에 해당하는 슬롯 없음

---

## 11-B. 예약 API — 게스트 예약 생성/조회 (§5.2)

### 11-B.1 휴대폰 인증 코드 발송

```
POST /bookings/phone-verifications
{ "phone": "01012345678" }

→ 200 OK
{
  "verificationId": 1,
  "phone": "01012345678",
  "code": "483921"   // MVP only — SMS 발송 구현 시 제거
}
```

에러:
- `400 INVALID_INPUT` — 전화번호 형식 불일치 (`^01[0-9]{8,9}$`)

### 11-B.2 게스트 예약 생성

```
POST /bookings/guest
{
  "phone": "01012345678",
  "verificationCode": "483921",
  "name": "홍길동",
  "slotId": 42,

  // 예약금 결제 시 (passId 없을 때 필수)
  "depositAmount": 5000,
  "paymentMethod": "CARD",        // CARD | EASY_PAY (BANK_TRANSFER 차단)

  // 8회권 결제 시 (passId 있으면 depositAmount/paymentMethod 무시)
  "passId": 7
}

→ 201 Created
{
  "bookingId": 1,
  "bookingNumber": "BK-00000001",
  "accessToken": "abc123...",
  "slotId": 42,
  "status": "BOOKED",
  "depositAmount": 5000,
  "balanceAmount": 45000,
  "className": "향수 클래스"
}
```

에러:
- `400 PHONE_VERIFICATION_FAILED` — 코드 불일치 또는 만료(5분)
- `404 NOT_FOUND` — slotId 미존재
- `404 NOT_FOUND` — passId 미존재 (8회권 결제 경로)
- `409 CAPACITY_EXCEEDED` — 슬롯 정원(8명) 초과
- `409 DUPLICATE_BOOKING` — 동일 전화번호 + 동일 슬롯 중복
- `409 SLOT_NOT_AVAILABLE` — 비활성 슬롯 예약 시도
- `422 PASS_EXPIRED` — 만료된 8회권
- `422 PASS_CREDIT_INSUFFICIENT` — 잔여 크레딧 0
- `422 PAYMENT_METHOD_NOT_ALLOWED` — BANK_TRANSFER 사용 시도

### 11-B.3 비회원 예약 조회

```
GET /bookings/{bookingId}?token={accessToken}

→ 200 OK
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

에러:
- `404 NOT_FOUND` — bookingId 미존재 또는 token 불일치

---

## 11-C. 예약 변경 API (§5.3)

### 11-C.1 예약 변경 (비회원)

```
PATCH /bookings/{bookingId}/reschedule
{
  "newSlotId": 43,
  "token": "access_token"
}

→ 200 OK
{
  "bookingId": 1,
  "bookingNumber": "BK-00000001",
  "slotId": 43,
  "startAt": "2026-03-01T14:00:00",
  "endAt":   "2026-03-01T16:00:00",
  "className": "향수 클래스",
  "status": "BOOKED"
}
```

에러:
- `400 INVALID_INPUT` — 동일 슬롯으로 변경 시도
- `404 NOT_FOUND` — 예약 미존재 또는 token 불일치
- `409 CAPACITY_EXCEEDED` — 새 슬롯 정원 초과
- `409 DUPLICATE_BOOKING` — 동일 전화번호 + 새 슬롯 이미 예약
- `409 SLOT_NOT_AVAILABLE` — 새 슬롯 비활성
- `409 BOOKING_CONFLICT` — 낙관적 락 충돌 (동시 변경)
- `422 CHANGE_NOT_ALLOWED` — 현재 슬롯 시작 1시간 이내

변경 정책:
- 현재 슬롯 시작 1시간 전까지 횟수 제한 없이 변경 가능
- 변경마다 `booking_history`에 `RESCHEDULED` 이력 누적 (append-only)
- `bookings` 행은 항상 1건 유지 (in-place 업데이트)

---

## 11-D. 예약 취소 API (§5.4)

### 11-D.1 비회원 예약 취소

```
DELETE /bookings/{bookingId}?token={accessToken}

→ 200 OK
{
  "bookingId": 1,
  "status": "CANCELED",
  "refundable": true      // D-1 00:00 이전이면 true
}
```

에러:
- `404 NOT_FOUND` — bookingId 미존재 또는 token 불일치
- `400 INVALID_INPUT` — BOOKED 상태가 아닌 예약 취소 시도

환불 정책:
- 예약금 결제: `refundable=true`이면 PG 환불 요청(Refund REQUESTED 기록). 실패 시 FAILED 상태 유지.
- 8회권 결제: `refundable=true`이면 REFUND ledger(+1) + remaining_credits 복구. `refundable=false`이면 크레딧 소멸 유지.

---

## 11-E. 8회권 구매 API (§7.1)

### 11-E.1 게스트 8회권 구매

```
POST /passes/guest
{
  "guestId": 1,
  "totalPrice": 320000    // 생략 시 0 처리 (MVP — 실제 PG 연동 전 임시)
}

→ 201 Created
{
  "passId": 7,
  "guestId": 1,
  "expiresAt": "2026-05-28T23:59:59",
  "totalCredits": 8,
  "remainingCredits": 8,
  "totalPrice": 320000
}
```

에러:
- `404 NOT_FOUND` — guestId 미존재

구매 정책:
- 만료일 = 구매일 기준 90일 후 Asia/Seoul 자정
- EARN ledger(+8) 자동 기록

---

## 11-F. 8회권 사용/소모/환불 Admin API (§7.2)

### 11-F.1 결석 처리

```
POST /admin/bookings/{bookingId}/no-show

→ 200 OK
{
  "bookingId": 1,
  "status": "NO_SHOW"
}
```

에러:
- `404 NOT_FOUND` — bookingId 미존재
- `400 INVALID_INPUT` — BOOKED 상태가 아닌 예약

정책: 크레딧은 예약 시 USE ledger로 이미 소모 → 추가 크레딧 변동 없음.

### 11-F.2 8회권 전체 환불

```
POST /admin/passes/{passId}/refund

→ 200 OK
{
  "canceledBookings": 2,    // 자동 취소된 미래 예약 수
  "refundCredits": 6,       // 환불된 크레딧 수 (잔여 전체)
  "refundAmount": 240000    // 환불 계산값 (KRW) — 관리자가 PG에서 수동 처리
}
```

에러:
- `404 NOT_FOUND` — passId 미존재

환불 정책:
- 미래 BOOKED 예약 자동 취소 (슬롯 booked_count 복구, CANCELED 이력 기록)
- REFUND ledger(잔여 크레딧 전체) 기록 후 remaining_credits = 0
- 실제 PG 환불은 `refundAmount`를 참고해 관리자가 수동 처리
- 단가 = totalPrice / totalCredits (8)

### 11-F.3 만료 배치 수동 트리거

```
POST /admin/passes/expire

→ 200 OK
{
  "successCount": 3,
  "failureCount": 0,
  "failureReasons": {}
}
```

정책: 만료된 pass의 remaining_credits = 0, EXPIRE ledger 기록.

## 11-G. 주문 배치 Admin API (§3.3)

### 11-G.1 픽업 만료 배치 수동 트리거

```
POST /admin/orders/expire-pickups

→ 200 OK
{
  "successCount": 2,
  "failureCount": 1,
  "failureReasons": {
    "NotFoundException": 1
  }
}
```

정책:
- `pickup_deadline_at < now` 인 `PICKUP_READY` 주문만 처리한다.
- 성공 건은 `PICKUP_EXPIRED_REFUNDED`로 전이하고 환불/재고 복구를 수행한다.

---

## 12. 비기능 요구사항(초안)
- 타임존: Asia/Seoul 고정
- 감사 로그:
    - 승인/거절/지연, 환불 실패/재시도, 예약 변경 이력은 반드시 남긴다
- 장애 대비:
    - 환불/알림은 실패 시 재시도 가능하게 `FAILED` 상태를 남기고 운영자가 확인 가능
- 보안:
    - 비회원 예약은 휴대폰 인증 기반
    - 관리자 기능은 권한 분리(단일 운영자라도 경로는 분리)
    - **Admin API(`/admin/**`)는 인증/인가 필수** — 현재 MVP 구현에서는 미적용 상태. §11 관리자 화면 구현 시 Spring Security 또는 API Key 방식으로 보호해야 한다.
    - 미적용 기간 동안 `/admin/**` 엔드포인트는 내부망 또는 개발 환경에서만 노출할 것

---

## 13. API 에러 코드 체계

### 13.1 에러 응답 포맷

```json
{
  "code":    "ALREADY_REFUNDED",
  "message": "이미 환불된 건입니다."
}
```

### 13.2 HTTP 상태코드 × 에러 코드 목록

| HTTP | 에러 코드 | 발생 상황 |
|------|----------|----------|
| 400 | `INVALID_INPUT` | 요청 바디/파라미터 검증 실패 |
| 400 | `PHONE_VERIFICATION_FAILED` | 인증 코드 불일치 또는 만료 (§5.2) |
| 404 | `NOT_FOUND` | 주문·예약·이용권·상품 미존재 |
| 409 | `ALREADY_REFUNDED` | 이미 자동환불된 주문에 승인 시도 (§3.1) |
| 409 | `INVENTORY_NOT_ENOUGH` | 재고 차감 시 수량 부족 |
| 409 | `CAPACITY_EXCEEDED` | 슬롯 정원(8명) 초과 예약 시도 (§4.1) |
| 409 | `DUPLICATE_BOOKING` | 동일 전화번호 + 동일 슬롯 중복 예약 (§5.2) |
| 409 | `SLOT_NOT_AVAILABLE` | 비활성 슬롯 예약 시도 (§5.2) |
| 409 | `BOOKING_CONFLICT` | 낙관적 락 충돌 — 동시 변경 요청 (§5.3) |
| 422 | `REFUND_NOT_ALLOWED` | D-1 00:00 이후 환불 요청 (§4.2) |
| 422 | `PRODUCTION_REFUND_NOT_ALLOWED` | 제작 시작 후 주문 거절/환불 시도 (§3.2) |
| 422 | `CHANGE_NOT_ALLOWED` | 슬롯 시작 1시간 이내 변경 요청 (§4.2) |
| 422 | `PASS_EXPIRED` | 만료된 8회권으로 예약 시도 (§7.1) |
| 422 | `PASS_CREDIT_INSUFFICIENT` | 잔여 크레딧 0인 8회권으로 예약 시도 (§7.2) |
| 422 | `PAYMENT_METHOD_NOT_ALLOWED` | 계좌이체(BANK_TRANSFER)로 예약금 결제 시도 (§6.2) |

### 13.3 구현 위치

- `ErrorCode` enum — `common/error/ErrorCode.java`
- `HappyGalleryException` — `common/error/HappyGalleryException.java`
- `ErrorResponse` record — `common/error/ErrorResponse.java`
- 개별 예외 클래스 — `common/error/` (각 에러 코드별 1:1 대응)
- `GlobalExceptionHandler` — `app/web/GlobalExceptionHandler.java`

---
문서 끝.

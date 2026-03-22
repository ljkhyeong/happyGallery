# ADR-0022: 시스템 경계, 상태 모델, 데이터 모델 기준선

**날짜**: 2026-03-17  
**상태**: Accepted

---

## 컨텍스트

기존 `docs/PRD/0001_기준_스펙/spec.md`에는 제품 요구사항 외에도

- 시스템 형태와 패키지 경계
- 동시성/락 전략
- 상태 모델
- 테이블/컬럼/인덱스 수준 데이터 모델

같은 설계 기준이 함께 들어 있었다.

이 내용은 core PRD보다 설계 결정과 구현 기준선에 가깝다. 그래서 ADR로 분리한다.

---

## 결정 사항

### 1. 시스템 형태는 모놀리식 기준을 유지한다

- 1차 운영 목표는 빠르고 확실한 단일 서비스 운영이다.
- 저장소 최상위 모듈은 `app`, `domain`, `infra`, `common` 구조를 유지한다.
- 내부 기능 경계는 다음 도메인 축을 기준으로 나눈다.
  - `catalog`
  - `order`
  - `booking`
  - `pass`
  - `payment`
  - `notification`
  - `admin`

세부 헥사고날 전환 전략은 `ADR-0021`을 따른다.

- 관리자 조회 경로는 `AdminBookingQueryService`/`AdminOrderQueryService`를 기준으로 한다. 컨트롤러가 infra를 직접 보지 않게 하고, User batch fetch로 예약자/주문자 정보를 합쳐 만든다.

### 2. 일관성과 동시성은 DB 트랜잭션을 기준으로 잡는다

- 남은 재고와 슬롯 정원처럼 수량이 줄어드는 값은 DB 트랜잭션 안에서 갱신한다.
- 외부 호출이 섞이는 주문 승인/환불 계열은 상태를 먼저 기록하고 재시도 가능하게 설계한다.

현재 잠금/충돌 처리 기준:

- 슬롯 정원:
  - slot row를 `SELECT ... FOR UPDATE`로 잠그고 `booked_count`를 증가/검증한다.
  - 상세 전략은 `ADR-0003`을 따른다.
- 단일 작품 재고:
  - 재고 row 잠금 또는 버전 기반 낙관적 락으로 보호한다.
- 주문 승인/자동환불/픽업 만료/8회권 만료·환불:
  - `orders.version`, `fulfillments.version`, `pass_purchases.version` 기반 낙관적 락과 제한된 재시도로 충돌을 흡수한다.

### 3. 상태 모델 기준은 주문/예약/이행을 분리해 유지한다

주문 상태 기준:

- `PAID_APPROVAL_PENDING`
- `APPROVED_FULFILLMENT_PENDING`
- `DELAY_REQUESTED`
- `REJECTED`
- `AUTO_REFUND_TIMEOUT`
- 픽업: `PICKUP_READY` → `PICKED_UP` / `PICKUP_EXPIRED`
- 제작: `IN_PRODUCTION` → `DELAY_REQUESTED` → `APPROVED_FULFILLMENT_PENDING`
- 배송: `APPROVED_FULFILLMENT_PENDING` → `SHIPPING_PREPARING` → `SHIPPED` → `DELIVERED`

예약 상태 기준:

- `BOOKED`
- `CANCELED`
- `NO_SHOW`
- `COMPLETED`

세부 전이와 운영 이력 결정은 아래 ADR을 우선 기준으로 한다.

- 주문 승인 모델: `ADR-0013`
- 예약 제작 주문: `ADR-0014`
- 예약 변경/취소: `ADR-0006`, `ADR-0007`
- 8회권 구매/만료/사용/환불: `ADR-0010`, `ADR-0011`

### 4. 데이터 모델 기준선은 현재 논리 모델을 문서화한다

#### 사용자/게스트

- `users`
  - `id`, `email`, `password_hash`, `name`, `phone`, `created_at`
- `guests`
  - `id`, `name`, `phone`, `phone_verified`, `created_at`

#### 상품/재고

- `products`
  - `id`, `name`, `type(READY_STOCK|MADE_TO_ORDER)`, `price`, `status(ACTIVE|INACTIVE)`
- `inventory`
  - `product_id(PK/FK)`, `quantity`, `version`, `updated_at`
- `made_to_order_spec`
  - `product_id(FK)`, `lead_time_hint(optional)`, `refundable_until_state=IN_PRODUCTION`

#### 주문/환불/이행

- `orders`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `access_token VARCHAR(64)` — SHA-256 hex 해시를 저장하고 UNIQUE 제약을 둔다 (V17 migration)
  - `status`, `total_amount`, `paid_at`, `approval_deadline_at`, `bundle_id nullable`, `version`
- `order_items`
  - `id`, `order_id`, `product_id`, `qty`, `unit_price`
- `order_approvals`
  - `id`, `order_id`, `decided_by_admin_id`, `decision`, `reason`, `decided_at`
  - `AUTO_REFUND`는 배치 이력이며 `decided_by_admin_id`는 `null`일 수 있다.
- `fulfillments`
  - `id`, `order_id(unique)`, `type(SHIPPING|PICKUP)`, `expected_ship_date`, `pickup_deadline_at`, `version`
  - 주문 상태의 단일 소스는 `orders.status`다.
  - 주문당 fulfillment는 1건만 유지한다.
- `refunds`
  - `id`, `order_id nullable`, `booking_id nullable`, `amount`, `status`, `pg_ref`, `fail_reason`, `created_at`

#### 클래스/슬롯/예약

- `classes`
  - `id`, `name`, `category`, `duration_min`, `price`, `buffer_unit=30`
- `slots`
  - `id`, `class_id`, `start_at`, `end_at`, `capacity=8`, `booked_count`, `is_active`
- `bookings`
  - `id`, `user_id nullable`(회원 예약 또는 guest claim 시 설정), `guest_id nullable`(게스트 예약 시 설정)
  - `access_token VARCHAR(64)` — SHA-256 hex 해시를 저장하고 UNIQUE 제약을 둔다 (V17 migration, 게스트 예약만)
  - `class_id`, `slot_id`, `status`
  - `deposit_amount`, `deposit_paid_at`
  - `balance_amount`, `balance_status`, `arrears_flag`, `version`
- `booking_history`
  - `id`, `booking_id`, `action`, `from_slot_id`, `to_slot_id`, `actor`, `reason`, `created_at`

#### 상품 Q&A / 1:1 문의

- `product_qna`
  - `id`, `product_id`, `user_id`
  - `title`, `content`, `secret`, `password_hash nullable`
  - `reply_content nullable`, `replied_at nullable`, `replied_by nullable`, `created_at`
- `inquiry`
  - `id`, `user_id`
  - `title`, `content`
  - `reply_content nullable`, `replied_at nullable`, `replied_by nullable`, `created_at`

#### 8회권

- `pass_purchases`
  - `id`, `user_id`, `purchased_at`, `expires_at`, `total_credits=8`, `remaining_credits`, `total_price`, `version`
- `pass_ledger`
  - `id`, `pass_purchase_id`, `type(EARN|USE|REFUND|EXPIRE)`, `amount`, `related_booking_id nullable`, `created_at`

#### 주요 인덱스

- `orders(status, approval_deadline_at)`
- `inventory(product_id, version)`
- `refunds(status, created_at)`

---

## 결과

### 장점

- core PRD는 비즈니스 정책과 제품 요구사항에 집중할 수 있다.
- 현재 설계 기준을 ADR 문서 한 곳에서 볼 수 있다.
- 상태 모델과 스키마 기준을 API 계약 문서와 분리해 관리할 수 있다.

### 단점

- PRD, API PRD, ADR을 함께 봐야 전체 그림이 완성된다.
- 일부 세부 내용은 기존 ADR과 이 문서에 나뉘어 있어 참조 비용이 생긴다.

---

## 참고 문서

- `docs/ADR/0001_핵심_스키마/adr.md`
- `docs/ADR/0003_슬롯_동시성_전략/adr.md`
- `docs/ADR/0013_주문_승인_모델/adr.md`
- `docs/ADR/0014_예약_제작_주문_결정/adr.md`
- `docs/ADR/0021_헥사고날_아키텍처_전환/adr.md`
- `docs/PRD/0001_기준_스펙/spec.md`

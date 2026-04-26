# ADR-0022: 현재 시스템 경계, 상태 모델, 데이터 모델

**날짜**: 2026-03-17  
**상태**: Accepted

---

## 왜 이 문서가 필요한가

핵심 PRD에는 제품 요구사항과 함께 시스템 구조, 상태 모델, 데이터 모델까지 섞여 있었다.  
이 문서는 현재 백엔드가 어떤 경계와 상태, 테이블 구조를 기준으로 움직이는지 따로 정리한 문서다.

---

## 결정

### 1. 시스템 형태는 단일 백엔드 서비스를 유지한다

현재 운영 구조는 프론트와 백엔드의 배포는 분리하지만, 백엔드 자체는 하나의 Spring Boot 서비스로 운영한다.

백엔드 내부 구조는 아래 6개 모듈을 기준으로 본다.

- `bootstrap`: 앱 시작점과 공통 설정
- `adapter-in-web`: HTTP 요청 진입점
- `adapter-out-persistence`: DB 접근
- `adapter-out-external`: 외부 서비스 연동
- `application`: 유스케이스와 업무 흐름
- `domain`: 핵심 도메인 규칙

모듈 원칙과 의존 방향은 `ADR-0021`을 따른다.

### 2. 일관성과 동시성은 DB 트랜잭션을 기준으로 맞춘다

- 재고와 슬롯 정원처럼 줄어드는 수량은 DB 트랜잭션 안에서 갱신한다.
- 외부 호출이 섞이는 승인, 환불, 배치 흐름은 상태를 먼저 기록하고 재시도 가능하게 설계한다.

현재 잠금과 충돌 처리 기준:

- 슬롯 정원: `SELECT ... FOR UPDATE` + `booked_count`
- 단일 작품 재고: row lock 또는 version 기반 낙관적 락
- 주문 승인, 자동 환불, 픽업 만료, 8회권 만료/환불: version 기반 낙관적 락 + 제한된 재시도

### 3. 상태 모델은 주문, 예약, 이행을 나눠 관리한다

주문 상태:

- `PAID_APPROVAL_PENDING`
- `APPROVED_FULFILLMENT_PENDING`
- `DELAY_REQUESTED`
- `REJECTED`
- `AUTO_REFUND_TIMEOUT`
- 픽업: `PICKUP_READY` -> `PICKED_UP` / `PICKUP_EXPIRED`
- 제작: `IN_PRODUCTION` -> `DELAY_REQUESTED` -> `APPROVED_FULFILLMENT_PENDING`
- 배송: `APPROVED_FULFILLMENT_PENDING` -> `SHIPPING_PREPARING` -> `SHIPPED` -> `DELIVERED`

예약 상태:

- `BOOKED`
- `CANCELED`
- `NO_SHOW`
- `COMPLETED`

세부 전이는 아래 ADR을 우선 기준으로 본다.

- 주문 승인: `ADR-0013`
- 예약 제작 주문: `ADR-0014`
- 예약 변경/취소: `ADR-0006`, `ADR-0007`
- 8회권 구매/만료/사용/환불: `ADR-0010`, `ADR-0011`

### 4. 현재 데이터 모델은 아래를 기준으로 본다

#### 사용자와 비회원

- `users`
  - `id`, `email`, `password_hash`, `name`, `phone`, `provider`, `provider_id`, `created_at`
- `guests`
  - `id`, `name`, `phone`, `phone_verified`, `created_at`

#### 상품과 재고

- `products`
  - `id`, `name`, `type(READY_STOCK|MADE_TO_ORDER)`, `price`, `status(ACTIVE|INACTIVE)`
- `inventory`
  - `product_id(PK/FK)`, `quantity`, `version`, `updated_at`
- `made_to_order_spec`
  - `product_id(FK)`, `lead_time_hint(optional)`, `refundable_until_state=IN_PRODUCTION`

#### 주문과 이행

- `orders`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `access_token VARCHAR(64)` — SHA-256 hex 해시 저장
  - `status`, `total_amount`, `paid_at`, `approval_deadline_at`, `bundle_id nullable`, `payment_key nullable`, `version`
- `order_items`
  - `id`, `order_id`, `product_id`, `qty`, `unit_price`
- `order_approvals`
  - `id`, `order_id`, `decided_by_admin_id`, `decision`, `reason`, `decided_at`
- `fulfillments`
  - `id`, `order_id(unique)`, `type(SHIPPING|PICKUP)`, `expected_ship_date`, `pickup_deadline_at`, `version`
- `refunds`
  - `id`, `order_id nullable`, `booking_id nullable`, `amount`, `status`, `pg_ref`, `fail_reason`, `created_at`
- `payment_attempt`
  - `id`, `order_id_external`, `context(ORDER|BOOKING|PASS)`, `amount`, `status`
  - `payment_key nullable`, `pg_ref nullable`, `payload_json`, `created_at`, `confirmed_at nullable`, `version`

#### 클래스, 슬롯, 예약

- `classes`
  - `id`, `name`, `category`, `duration_min`, `price`, `buffer_unit=30`
- `slots`
  - `id`, `class_id`, `start_at`, `end_at`, `capacity=8`, `booked_count`, `is_active`
- `bookings`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `access_token VARCHAR(64)` — 게스트 예약 조회용 SHA-256 hex 해시 저장
  - `class_id`, `slot_id`, `status`
  - `deposit_amount`, `deposit_paid_at`, `payment_key nullable`
  - `balance_amount`, `balance_status`, `arrears_flag`, `version`
- `booking_history`
  - `id`, `booking_id`, `action`, `from_slot_id`, `to_slot_id`, `actor`, `reason`, `created_at`

#### Q&A와 문의

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
  - `id`, `user_id`, `purchased_at`, `expires_at`, `total_credits=8`, `remaining_credits`, `total_price`, `payment_key nullable`, `version`
- `pass_ledger`
  - `id`, `pass_purchase_id`, `type(EARN|USE|REFUND|EXPIRE)`, `amount`, `related_booking_id nullable`, `created_at`

#### 주요 인덱스

- `orders(status, created_at, id)` 커서 조회
- `payment_attempt(order_id_external)` UNIQUE
- `payment_attempt(status, created_at)` 미완료 결제 시도 정리 후보 조회
- `inventory(product_id, version)`
- `notification_log(user_id, sent_at DESC)`
- `notification_log(guest_id, sent_at DESC)`
- `refunds(status, created_at)`

---

## 결과

### 장점

- PRD는 사용자 요구사항에 집중하고, 이 문서는 시스템 구조와 데이터 구조에 집중할 수 있다.
- 상태 모델과 테이블 구조를 한 문서에서 빠르게 확인할 수 있다.

### 단점

- 전체 그림을 보려면 PRD와 다른 ADR을 함께 봐야 한다.

---

## 참고 문서

- `docs/ADR/0001_핵심_스키마/adr.md`
- `docs/ADR/0003_슬롯_동시성_전략/adr.md`
- `docs/ADR/0013_주문_승인_모델/adr.md`
- `docs/ADR/0014_예약_제작_주문_결정/adr.md`
- `docs/ADR/0021_Hexagonal_아키텍처_전환/adr.md`
- `docs/PRD/0001_기준_스펙/spec.md`

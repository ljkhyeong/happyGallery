# ADR-0022: 현재 시스템 경계, 상태 모델, 데이터 모델

**날짜**: 2026-03-17  
**상태**: Accepted

**갱신**: 2026-07-17

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
- `DELAY_REJECTED_CANCELED`
- `REJECTED`
- `AUTO_REFUND_TIMEOUT`
- 픽업: `PICKUP_READY` -> `PICKED_UP` / 기성품 `PICKUP_EXPIRED` / 주문제작 `PICKUP_FORFEITED`
- 제작: `IN_PRODUCTION` -> `DELAY_REQUESTED` -> `APPROVED_FULFILLMENT_PENDING`
- 제작 지연 거절: `IN_PRODUCTION` -> `DELAY_REJECTED_CANCELED`
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
  - `id`, `email_enc`, `email_hmac`, `password_hash nullable`, `name_enc`, `name_hmac`, `phone_enc nullable`, `phone_hmac nullable`, `phone_verified`, `last_login_at`, `created_at`
  - 이메일·이름·전화번호 평문 컬럼은 두지 않는다. 복호화가 필요한 값은 `*_enc`, 정확 일치 조회는 `*_hmac`를 사용한다.
- `user_social_accounts`
  - `id`, `user_id`, `provider(GOOGLE|NAVER)`, `provider_id_hmac`, `created_at`
  - 외부 식별자는 provider 내부에서만 고유하므로 `(provider, provider_id_hmac)`를 유일하게 유지한다. 원문은 저장하지 않는다.
  - 한 회원이 같은 provider의 계정을 둘 이상 연결하지 않도록 `(user_id, provider)`를 유일하게 유지한다.
- `guests`
  - `id`, `name_enc`, `name_hmac`, `phone_enc`, `phone_hmac`, `phone_verified`, `created_at`
  - 비회원 이름·전화번호 평문 컬럼은 두지 않는다. 표시는 암호문 복호화, 동등 검색은 HMAC으로 처리한다.
- `phone_verifications`
  - `id`, `phone_hmac`, `code_hmac`, `code_enc`, `verified`, `expires_at`, `created_at`
  - 전화번호와 인증 코드 평문은 저장하지 않는다. 인증은 전화번호와 코드의 HMAC으로 조회하고, 로컬 전용 코드 조회는 `code_enc`를 복호화한다.

#### 상품과 재고

- `products`
  - `id`, `name`, `type(READY_STOCK|MADE_TO_ORDER)`, `category nullable`, `price`, `status(ACTIVE|INACTIVE)`
- `inventory`
  - `product_id(PK/FK)`, `quantity`, `version`, `updated_at`
- `made_to_order_spec`
  - `product_id(FK)`, `lead_time_hint(optional)`, `refundable_until_state=IN_PRODUCTION`

#### 주문과 이행

- `orders`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `user_id`, `guest_id` 중 정확히 하나만 존재하도록 `chk_orders_exactly_one_owner` `CHECK` 제약으로 강제한다.
  - `access_token VARCHAR(64)` — SHA-256 hex 해시 저장
  - `status`, `total_amount`, `paid_at`, `approval_deadline_at`, `bundle_id nullable`, `payment_key nullable`, `version`
- `order_items`
  - `id`, `order_id`, `product_id`, `qty`, `unit_price`
- `order_approvals`
  - `id`, `order_id`, `decided_by_admin_id`, `decision`, `reason`, `decided_at`
- `fulfillments`
  - `id`, `order_id(unique)`, `type(SHIPPING|PICKUP)`, `expected_ship_date`, `pickup_deadline_at`, `version`
- `refunds`
  - `id`, `order_id nullable`, `booking_id nullable`, `pass_purchase_id nullable`, `payment_attempt_id nullable`
  - 네 참조 중 정확히 하나, `amount`, `payment_key`, `refund_transaction_key`, `idempotency_key UNIQUE`, `fail_reason`
  - `status(REQUESTED|PROCESSING|RETRYABLE|RECONCILIATION_REQUIRED|SUCCEEDED|FAILED)`, `processing_at`, `processing_token`, `attempt_count`, `next_attempt_at`, `created_at`, `updated_at`, `version`
- `payment_attempt`
  - `id`, `order_id_external`, `context(ORDER|BOOKING|PASS)`, `amount`, `status`
  - `processing_at nullable`, `payment_key nullable`, `pg_ref nullable`, `fail_reason nullable`
  - `payload_enc`, `created_at`, `confirmed_at nullable`, `version`
  - 내부 결제 payload는 AES-GCM 암호문으로 저장하고 claim·fulfillment 시점에만 복호화한다.
  - 상태: `PENDING | PROCESSING | RETRYABLE | APPROVED | CONFIRMED | FAILED | COMPENSATION_REQUESTED | COMPENSATION_FAILED | COMPENSATED | CANCELED`

#### 클래스, 슬롯, 예약

- `classes`
  - `id`, `name`, `category`, `duration_min`, `price`, `buffer_unit=30`
- `slots`
  - `id`, `class_id`, `start_at`, `end_at`, `capacity=8`, `booked_count`, `admin_active`, `buffer_block_count`
  - 실제 활성 상태는 `admin_active=true AND buffer_block_count=0`으로 판정한다.
- `bookings`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `user_id`, `guest_id` 중 정확히 하나만 존재하도록 `chk_bookings_exactly_one_owner` `CHECK` 제약으로 강제한다.
  - `access_token VARCHAR(64)` — 게스트 예약 조회용 SHA-256 hex 해시 저장
  - `class_id`, `slot_id`, `status`
  - `deposit_amount`, `deposit_paid_at`, `payment_key nullable`
  - `balance_amount`, `balance_status`, `arrears_flag`, `version`
- `booking_history`
  - `id`, `booking_id`, `action`, `from_slot_id`, `to_slot_id`, `actor`, `reason`, `created_at`

카테고리는 정책 분기를 만들지 않는 표시·필터용 값이므로 enum이 아니라 문자열로 저장한다.
저장·조회 필터 기준은 앞뒤 공백을 제거한 대문자 토큰이다.

#### 주문·예약 소유자 제약 배포

- `Order`, `Booking`은 private 생성자에서 회원·비회원 소유자 중 정확히 하나만 존재하는지 한 번 검증하고, 생성 팩토리와 이력 가져오기가 같은 검증을 사용한다.
- 소유자 조회 결과는 이 불변식과 FK를 신뢰하며, 관리자 응답에서 누락을 `알 수 없음`으로 바꾸어 숨기지 않는다.
- MySQL DDL의 비트랜잭션 특성으로 인해 하나의 실패 마이그레이션 안에 첫 번째 `ALTER`만 남는 상태를 피하도록 예약과 주문 제약을 각각 `V39`, `V40` 마이그레이션으로 분리한다.
- 기존 레코드가 소유자 규칙을 위반하면 `ALTER TABLE` 검증이 실패하여 배포를 중단한다.
- 소유자를 추론해 자동 보정하지 않는다. 배포 전 아래 조회가 모두 0행인지 확인하고, 위반 레코드는 원본 이력을 기준으로 수동 복구한다.
- 마이그레이션이 예상치 못하게 실패하면 데이터를 복구하고 제약이 부분 생성되지 않았는지 확인한 뒤, 실패한 Flyway 이력을 `repair`하고 재실행한다.

```sql
SELECT id, user_id, guest_id
FROM bookings
WHERE (user_id IS NULL AND guest_id IS NULL)
   OR (user_id IS NOT NULL AND guest_id IS NOT NULL);

SELECT id, user_id, guest_id
FROM orders
WHERE (user_id IS NULL AND guest_id IS NULL)
   OR (user_id IS NOT NULL AND guest_id IS NOT NULL);
```

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
- `users(email_hmac)` UNIQUE, `users(name_hmac)` 정확 일치 검색
- `guests(phone_hmac)` UNIQUE, `guests(name_hmac)` 정확 일치 검색
- `user_social_accounts(provider, provider_id_hmac)` UNIQUE
- `phone_verifications(phone_hmac, id)` 최신 인증 조회
- `inventory(product_id, version)`
- `notification_log(user_id, sent_at DESC)`
- `notification_log(guest_id, sent_at DESC)`
- `refunds(status, created_at)`
- `refunds(status, next_attempt_at, created_at)`
- `refunds(status, processing_at, created_at)`

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
- `docs/ADR/0036_개인정보_평문_제거와_블라인드_인덱스_기준/adr.md`
- `docs/PRD/0001_기준_스펙/spec.md`

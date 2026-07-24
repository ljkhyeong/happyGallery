# ADR-0022: 현재 시스템 경계, 상태 모델, 데이터 모델

**날짜**: 2026-03-17  
**상태**: Accepted

**갱신**: 2026-07-23

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
- `REJECTED`
- `CUSTOMER_CANCELED`
- `AUTO_REFUND_TIMEOUT`
- `IN_PRODUCTION`
- `DELAY_CONSENT_PENDING`
- `DELAY_ACCEPTED`
- `DELAY_REJECTED_CANCELED`
- 픽업: `PICKUP_READY` -> `PICKED_UP` / 기성품 `PICKUP_EXPIRED` / 주문제작 `PICKUP_FORFEITED`
- 제작: `IN_PRODUCTION` -> `DELAY_CONSENT_PENDING` -> 고객 수락 `DELAY_ACCEPTED` / 고객 거절 `DELAY_REJECTED_CANCELED`
- 제작 재개·완료: `DELAY_ACCEPTED` -> `IN_PRODUCTION` / `APPROVED_FULFILLMENT_PENDING`
- 배송: `APPROVED_FULFILLMENT_PENDING` -> `SHIPPING_PREPARING` -> `SHIPPED` -> `DELIVERED`
- V74는 기존 `DELAY_REQUESTED` 값을 의미가 분명한 `DELAY_ACCEPTED`로 일괄 이관한다. 알림 이벤트 `ORDER_DELAY_REQUESTED`는 고객에게 동의를 요청한 사건 이름이므로 변경하지 않는다.

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

- `admin_user`
  - `id`, `username(unique)`, `password_hash`, `credential_version`, `created_at`
  - 비밀번호 해시는 롤백 호환 기간에 식별자 없는 BCrypt로 쓰며 `{bcrypt}$2...` 형식도 읽는다. 실제 비밀번호 변경 시 `credential_version`을 증가시키며 관리자 Bearer 세션은 발급 당시 버전과 현재 버전이 같아야 유효하다. 로그인 중 BCrypt 작업 강도만 승격할 때는 버전을 유지한다.
- `admin_setup_lock`
  - `id=1`인 단일 행만 유지한다. 최초 관리자 생성 트랜잭션이 이 행을 `FOR UPDATE`로 잠가 동시 setup 요청을 직렬화한다.
- `data_key_rotation_lock`
  - `id=1`인 단일 행만 유지한다. 개인정보 키 회전 트랜잭션이 `FOR UPDATE NOWAIT`로 잠가 중복 회전을 커밋·롤백까지 직렬화한다.

- `users`
  - `id`, `email_enc`, `email_hmac`, `password_hash nullable`, `credential_version`, `version`, `name_enc`, `name_hmac`, `phone_enc nullable`, `phone_hmac nullable`, `phone_verified`, `last_login_at`, `withdrawn_at nullable`, `created_at`
  - 이메일·이름·전화번호 평문 컬럼은 두지 않는다. 복호화가 필요한 값은 `*_enc`, 정확 일치 조회는 `*_hmac`를 사용한다.
  - 로컬 비밀번호 해시는 롤백 호환 기간에 식별자 없는 BCrypt로 쓰며 `{bcrypt}$2...` 형식도 읽는다.
  - `credential_version`은 실제 비밀번호·로그인 수단 변경마다 증가하며 이전 버전으로 발급한 회원 세션을 거절한다. 로그인 중 BCrypt 작업 강도만 승격할 때는 버전을 유지한다.
  - `version`은 로그인 시각·휴대폰 확인·비밀번호처럼 같은 회원 행을 갱신하는 경로의 stale update를 막는 JPA 낙관적 락 버전이다.
  - `phone_hmac`은 null을 허용하되 값이 있으면 회원 전체에서 유일하다. 전화번호 변경은 새 번호 SMS 소유 확인 뒤 이 제약과 애플리케이션 조회로 중복을 거절한다.
  - 탈퇴는 미완료 주문, `BOOKED` 예약, 사용 가능한 미만료 8회권, 미완료 환불이 없을 때만 허용한다. 이메일·이름을 탈퇴 식별값으로 바꾸고 전화번호·비밀번호·소셜 연결을 제거한 뒤 `withdrawn_at`과 자격 버전을 갱신한다. 이후 일반 회원 조회와 로그인에서 제외하고 기존 세션을 폐기한다.
- `user_social_accounts`
  - `id`, `user_id`, `provider(GOOGLE|NAVER)`, `provider_id_enc nullable`, `provider_id_hmac`, `created_at`
  - 외부 식별자는 provider 내부에서만 고유하므로 `(provider, provider_id_hmac)`를 유일하게 유지한다. 평문은 저장하지 않고, V63 이전 행의 nullable 암호문은 다음 소셜 로그인에서 채운다.
  - 한 회원이 같은 provider의 계정을 둘 이상 연결하지 않도록 `(user_id, provider)`를 유일하게 유지한다.
- `guests`
  - `id`, `name_enc`, `name_hmac`, `phone_enc`, `phone_hmac`, `phone_verified`, `created_at`
  - 비회원 이름·전화번호 평문 컬럼은 두지 않는다. 표시는 암호문 복호화, 동등 검색은 HMAC으로 처리한다.
- `phone_verifications`
  - `id`, `phone_hmac`, `code_hmac`, `code_enc`, `delivered`, `verified`, `expires_at`, `created_at`
  - 전화번호와 인증 코드 평문은 저장하지 않는다. 인증은 전화번호와 코드의 HMAC으로 조회하고, 로컬 전용 코드 조회는 `code_enc`를 복호화한다.
  - NHN이 발송 요청을 정상 접수해 `delivered=true`인 미소모·유효 코드만 인증할 수 있다. 발급 ID가 더 큰 코드의 접수 완료만 같은 번호의 이전 미소모 코드를 무효화하고, 늦게 끝난 이전 요청의 접수 완료는 폐기 상태를 되돌리지 않는다. 소비 조회는 비관적 잠금으로 한 번만 성공한다.

#### 상품과 재고

- `products`
  - `id`, `name`, `type(READY_STOCK|MADE_TO_ORDER)`, `category nullable`, `price`, `description nullable`, `image_url nullable`, `status(ACTIVE|INACTIVE)`, `version`
  - 관리자는 표시 정보와 대표 이미지를 수정하고 상태를 별도 변경한다. `version` 낙관적 락으로 동시에 먼저 읽은 관리자 수정이 앞선 변경을 덮지 못하게 하고 충돌은 409로 반환한다. 공개 목록과 주문 prepare는 `ACTIVE` 상품만 대상으로 한다. 기존 상세 URL은 비활성 상품도 조회하되 `available=false`로 표시한다.
- `inventory`
  - `product_id(PK/FK)`, `quantity`, `version`, `updated_at`
  - `quantity >= 0`을 DB `CHECK` 제약으로도 강제한다.
- `inventory_adjustments`
  - `id`, `product_id(FK)`, `type(INCREASE|DECREASE)`, `quantity`, `quantity_before`, `quantity_after`, `reason`
  - `adjusted_by_admin_id nullable`, `adjusted_by`, `adjusted_at`
  - 관리자 수동 조정은 `inventory` 행 잠금 안에서 수량 변경과 이력 저장을 같은 트랜잭션으로 처리한다.
- `cart_items`
  - `id`, `user_id(FK)`, `product_id(FK)`, `qty`, `created_at`, `updated_at`
  - `(user_id, product_id)`를 유일하게 유지한다.
- `cart_merge_requests`
  - `user_id(FK)`, `idempotency_key`, `payload_hash`, `created_at`
  - `(user_id, idempotency_key)`가 기본 키이며 정규화한 상품·수량의 SHA-256 해시를 함께 저장한다.
  - 비회원 장바구니 병합과 같은 트랜잭션에 삽입하며, 기존 키의 해시가 다르면 멱등키 재사용 충돌로 거절한다.
  - 생성 후 7일 동안 재시도 응답을 보장하고, 이후 보존 배치가 `created_at` 순으로 100건씩 삭제한다.
- 주문제작 여부는 별도 보조 테이블 없이 `products.type=MADE_TO_ORDER`로 판정한다.

#### 주문과 이행

- `orders`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `user_id`, `guest_id` 중 정확히 하나만 존재하도록 `chk_orders_exactly_one_owner` `CHECK` 제약으로 강제한다.
  - `access_token VARCHAR(64)` — SHA-256 hex 해시 저장
  - `status`, `total_amount`, `shipping_fee`, `paid_at`, `approval_deadline_at`, `bundle_id nullable`, `payment_key nullable`, `version`
  - `made_to_order_consent_version`, `made_to_order_consent_disclosure`, `made_to_order_consent_at` nullable — 주문제작 상품 결제 전 별도 고지·동의 스냅샷
  - `total_amount`는 상품 합계와 배송비를 포함한다. 배송비는 prepare 당시 서버 정책을 `shipping_fee`에 스냅샷으로 저장하고 픽업은 0원이다.
- `order_items`
  - `id`, `order_id`, `product_id`, `product_name`, `qty`, `unit_price`
  - `product_name`, `unit_price`는 상품 변경과 무관하게 결제 준비 시점 표시를 보존한다. 기존 `products.name VARCHAR(255)` 전체를 손실 없이 이관할 수 있도록 상품명 스냅샷도 `VARCHAR(255)`를 사용한다.
- `order_approvals`
  - `id`, `order_id`, `decided_by_admin_id`, `decision`, `reason`, `decided_at`
- `fulfillments`
  - `id`, `order_id(unique)`, `type(SHIPPING|PICKUP)`, `expected_ship_date`, `pickup_deadline_at`, `shipping_address_enc nullable`, `carrier nullable`, `tracking_number nullable`, `version`
  - 주문 confirm 시 고객이 선택한 타입으로 함께 생성한다. `SHIPPING`의 구조화 배송지는 AES-GCM 암호문으로 저장하고 관리자 단건 이행 조회에서만 복호화한다.
  - `carrier`, `tracking_number`는 배송 출발 시 한 쌍으로 저장한다. 픽업에는 둘 다 저장하지 않으며 DB `CHECK`로 강제한다.
- `refunds`
  - `id`, `order_id nullable`, `booking_id nullable`, `pass_purchase_id nullable`, `payment_attempt_id nullable`
  - 네 참조 중 정확히 하나, `amount`, `payment_key`, `refund_transaction_key`, `idempotency_key UNIQUE`, `fail_reason`
  - `status(REQUESTED|PROCESSING|RETRYABLE|RECONCILIATION_REQUIRED|SUCCEEDED|FAILED)`, `processing_at`, `processing_token`, `attempt_count`, `next_attempt_at`, `created_at`, `updated_at`, `version`
  - `RECONCILIATION_REQUIRED` 재선점은 취소 재호출보다 PG 취소 내역 조회를 먼저 수행한다. 실제 완료 취소면 성공으로 화해하고 미취소가 확정된 경우만 `RETRYABLE`로 전환한다.
- `payment_attempt`
  - `id`, `order_id_external`, `context(ORDER|BOOKING|PASS)`, `amount`, `status`
  - `processing_at nullable`, `processing_token nullable`, `payment_key nullable`, `confirmed_payment_key nullable`, `fail_reason nullable`
  - `payload_enc`, `fulfilled_domain_id nullable`, `fulfilled_access_token_enc nullable`, `created_at`, `confirmed_at nullable`, `confirm_recovery_attempted_at nullable`, `version`
  - 내부 결제 payload는 AES-GCM 암호문으로 저장하고 claim·fulfillment 시점에만 복호화한다.
  - confirm을 선점할 때마다 새 `processing_token`을 발급하고 현재 토큰 소유자의 PG 결과만 상태에 반영한다.
  - 자동 복구 전에 `confirm_recovery_attempted_at`을 저장해 1분 backoff와 후보 순환을 보장한다.
  - `CONFIRMED` 결과의 도메인 ID와 비회원 접근 토큰 암호문을 저장해 동일 confirm 재호출에 같은 결과를 반환한다.
  - 상태: `PENDING | PROCESSING | RETRYABLE | APPROVED | CONFIRMED | FAILED | RECONCILIATION_REQUIRED | COMPENSATION_REQUESTED | COMPENSATION_FAILED | COMPENSATED | CANCELED`

#### 클래스, 슬롯, 예약

- `classes`
  - `id`, `name`, `category`, `duration_min`, `price`, `buffer_min`, `description nullable`, `image_url nullable`, `preparation_info nullable`, `target_audience nullable`, `pass_eligible`, `status(ACTIVE|INACTIVE)`
  - 공개 목록과 슬롯 생성·결제는 `ACTIVE` 클래스만 대상으로 한다. `pass_eligible`은 구매한 `PassPlan`의 카테고리 정책과 함께 8회권 사용 가능 여부를 결정한다.
- `slots`
  - `id`, `class_id`, `start_at`, `end_at`, `capacity=8`, `booked_count`, `admin_active`, `buffer_block_count`
  - 실제 활성 상태는 `admin_active=true AND buffer_block_count=0`으로 판정한다.
- `bookings`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `user_id`, `guest_id` 중 정확히 하나만 존재하도록 `chk_bookings_exactly_one_owner` `CHECK` 제약으로 강제한다.
  - `access_token VARCHAR(64)` — 게스트 예약 조회용 SHA-256 hex 해시 저장
  - `class_id`, `slot_id`, `status`
  - `deposit_amount`, `deposit_paid_at`, `payment_key nullable`
  - `deposit_amount`, `deposit_paid_at`, `balance_amount`, `balance_status`, `balance_paid_at`, `arrears_flag`, `version`
- `booking_history`
  - `id`, `booking_id`, `action`, `from_slot_id`, `to_slot_id`, `actor`, `reason`, `created_at`

카테고리는 고정 enum이 아니라 확장 가능한 문자열로 저장하며, 저장·조회 필터 기준은 앞뒤 공백을 제거한 대문자 토큰이다.
다만 구매 시 확정한 `PassPlan`은 이 정규화 토큰과 `pass_eligible`을 함께 사용해 이용권 적용 가능 여부를 판단한다.

#### 공방 프로필

- `workshop_profiles`
  - `id=1`, `name`, `phone nullable`, `postal_code nullable`, `address_line1 nullable`, `address_line2 nullable`, `business_hours nullable`, `map_url nullable`, `parking_info nullable`, `business_registration_number nullable`, `representative_name nullable`, `email nullable`, `mail_order_registration_number nullable`, `introduction nullable`, `kakao_talk_id nullable`, `naver_talk_url nullable`, `naver_blog_url nullable`, `instagram_url nullable`, `smart_store_url nullable`, `updated_at`
  - 단일 행 `CHECK(id=1)`로 방문 안내와 공개 사업자 정보를 함께 관리한다. 공개 API는 같은 프로필을 반환하고 관리자 API만 수정한다.
  - `V81`은 아직 운영 배포 전인 스키마에서 불리언 `naver_talk_enabled`를 URL 필드로 같은 릴리스에 대체하므로 기존 필드를 바로 제거한다. 이미 관리자가 입력한 프로필 값은 유지하고 비어 있는 기준 사업자 정보만 채운다. 최초 운영 배포 이후의 컬럼 대체는 구버전·신버전 애플리케이션의 공존을 고려해 추가-전환-제거 순서로 별도 마이그레이션한다.

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

#### 회원 전화번호 유일 제약 배포

- `V73`은 `withdrawn_at` 추가, `phone_hmac` UNIQUE 추가와 기존 일반 인덱스 제거를 하나의 MySQL 8 atomic `ALTER TABLE` 문으로 실행한다. 중복 때문에 UNIQUE 생성이 실패해도 앞선 컬럼 추가만 남는 부분 적용을 허용하지 않는다.
- 배포 전 아래 조회 결과가 0행인지 확인한다. 중복 회원을 자동 병합하거나 임의로 한 행을 선택하지 않는다.

```sql
SELECT phone_hmac, COUNT(*) AS duplicate_count, GROUP_CONCAT(id ORDER BY id) AS user_ids
FROM users
WHERE phone_hmac IS NOT NULL
GROUP BY phone_hmac
HAVING COUNT(*) > 1;
```

- 중복이 있으면 거래·본인 확인 이력을 기준으로 유지할 회원을 수동 결정한다. 나머지 회원은 `phone_enc`, `phone_hmac`를 `NULL`, `phone_verified`를 `FALSE`로 바꾸고 자격·낙관적 락 버전을 함께 갱신한 뒤 재검사한다. 서로 다른 회원의 주문·예약·결제 이력을 자동으로 합치지 않는다.

#### Q&A와 문의

- `product_qna`
  - `id`, `product_id`, `user_id`
  - `title`, `content`, `secret`, `password_hash VARCHAR(255) nullable`
  - 비밀글 비밀번호 해시는 회원·관리자와 같은 BCrypt 호환 형식으로 저장한다.
  - `reply_content nullable`, `replied_at nullable`, `replied_by nullable`, `created_at`

- `inquiry`
  - `id`, `user_id`
  - `title`, `content`
  - `reply_content nullable`, `replied_at nullable`, `replied_by nullable`, `created_at`

#### 알림 outbox

- `notification_outbox`
  - `id`, 수신자 식별자, `event_type`, `idempotency_key`, `status`, 재시도 시각과 횟수, `processed_at`, `read_at`
  - `processing_token`, `locked_at`, `version`으로 재선점 전 실행의 오래된 결과 반영을 차단한다.

#### 8회권

- `pass_purchases`
  - `id`, `user_id`, `purchased_at`, `expires_at`, `plan_code`, `total_credits=8`, `remaining_credits`, `total_price`, `payment_key nullable`, `version`
  - `plan_code`는 구매 시점 이용권 계약 스냅샷이다. 신규 구매는 `REGULAR_CRAFT_8`, 정책 도입 전 데이터는 `LEGACY_ALL_CLASSES`를 사용한다.
- `pass_ledger`
  - `id`, `pass_purchase_id`, `type(EARN|USE|REFUND|EXPIRE)`, `amount`, `related_booking_id nullable`, `created_at`

#### 주요 인덱스

- `orders(status, created_at, id)` 커서 조회
- `payment_attempt(order_id_external)` UNIQUE
- `payment_attempt(status, created_at)` 미완료 결제 시도 정리 후보 조회
- `payment_attempt(status, confirm_recovery_attempted_at, created_at)` confirm 자동 복구 backoff·후보 조회
- `users(email_hmac)` UNIQUE, `users(phone_hmac)` UNIQUE, `users(name_hmac)` 정확 일치 검색
- `guests(phone_hmac)` UNIQUE, `guests(name_hmac)` 정확 일치 검색
- `user_social_accounts(provider, provider_id_hmac)` UNIQUE
- `phone_verifications(phone_hmac, id)` 최신 인증 조회
- `phone_verifications(expires_at, id)` 보존 기간 만료 행을 100건씩 짧게 삭제
- `cart_merge_requests(created_at, user_id, idempotency_key)` 보존 기간 만료 삭제
- `inventory(product_id, version)`
- `inventory_adjustments(product_id, adjusted_at, id)` 최근 수동 조정 이력 조회
- `notification_outbox(user_id, status, processed_at DESC, id DESC)` 회원 알림함 조회
- `notification_outbox(guest_id, status, processed_at DESC, id DESC)` 수신자별 발송 완료 조회
- `notification_outbox(status, processed_at, id)` 발송 완료 보존 정리
- `notification_log(sent_at, id)` 채널 감사 로그 보존 정리
- `notification_log(user_id, event_type, status, sent_at)` 회원 알림 중복 확인
- `notification_log(guest_id, event_type, status, sent_at)` 비회원 알림 중복 확인
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

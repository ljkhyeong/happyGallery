# ADR-0022: 현재 시스템 경계, 상태 모델, 데이터 모델

**날짜**: 2026-03-17  
**상태**: Accepted

**갱신**: 2026-08-27

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
- 공지 관리자 수정·삭제와 공방 프로필 수정: 조회 응답 `version`과 변경 요청 `expectedVersion`의
  명시적 비교 + version 기반 낙관적 락
- 공지 조회수: `view_count = view_count + 1` 원자 갱신. 조회수는 관리자 편집 충돌을
  만들지 않도록 version 갱신과 일반 엔티티 UPDATE에서 제외한다.

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
- 기성품 재고 부족 지연: `PAID_APPROVAL_PENDING` -> `DELAY_CONSENT_PENDING` -> 고객 수락 `DELAY_ACCEPTED` -> 처리 재개 `APPROVED_FULFILLMENT_PENDING` / 고객 거절 `DELAY_REJECTED_CANCELED`
- 주문제작 일정 지연: `IN_PRODUCTION` -> `DELAY_CONSENT_PENDING` -> 고객 수락 `DELAY_ACCEPTED` -> 처리 재개 `IN_PRODUCTION` / 고객 거절 `DELAY_REJECTED_CANCELED`
- 제작 완료: `IN_PRODUCTION` -> `APPROVED_FULFILLMENT_PENDING`
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
  - `id`, `username(unique)`, `password_hash`, `credential_version`, `totp_secret_enc nullable`, `mfa_enabled`, `created_at`
  - 비밀번호 해시는 롤백 호환 기간에 식별자 없는 BCrypt로 쓰며 `{bcrypt}$2...` 형식도 읽는다. 웹 입력은 UTF-8 72바이트 이하로 제한한다. 실제 비밀번호 또는 MFA 설정 변경 시 `credential_version`을 증가시키며 관리자 Bearer 세션은 발급 당시 버전과 현재 버전이 같아야 유효하다. 로그인 중 BCrypt 작업 강도만 승격할 때는 버전을 유지한다.
  - 인증되지 않은 요청이 운영자 계정을 잠그는 것을 막기 위해 V103에서 `failed_login_attempts`, `locked_until`을 제거한다. 로그인 남용은 IP 처리율 제한·MFA·감사 이력으로 통제하고, TOTP 비밀키는 AES-GCM 암호문으로만 저장한다.
- `admin_mfa_challenge`
  - `id`, `admin_user_id`, `token_hmac(unique)`, `expires_at`, `consumed_at nullable`, `created_at`
  - 로그인 2단계 challenge 원문은 저장하지 않고 5분 유효 HMAC만 저장하며 성공 시 한 번 소비한다.
- `admin_mfa_recovery_code`
  - `id`, `admin_user_id`, `code_hash`, `used_at nullable`, `created_at`
  - 복구 코드는 무작위 salt가 있는 비밀번호 해시만 저장하고 한 번만 사용한다.
- `admin_auth_history`
  - `id`, `admin_user_id nullable`, `subject_hmac nullable`, `hmac_key_id nullable`, `outcome`, `created_at`
  - 로그인·MFA 결과만 남기고 입력 사용자명과 challenge 원문은 저장하지 않는다. 180일 보존 뒤 배치 삭제한다.
- `admin_setup_lock`
  - `id=1`인 단일 행만 유지한다. 최초 관리자 생성 트랜잭션이 이 행을 `FOR UPDATE`로 잠가 동시 setup 요청을 직렬화한다.
- `data_key_rotation_lock`
  - `id=1`인 단일 행만 유지한다. 개인정보 키 회전 트랜잭션이 `FOR UPDATE NOWAIT`로 잠가 중복 회전을 커밋·롤백까지 직렬화한다.

- `users`
  - `id`, `email_enc`, `email_hmac`, `password_hash nullable`, `credential_version`, `version`, `name_enc`, `name_hmac`, `phone_enc nullable`, `phone_hmac nullable`, `phone_verified`, `last_login_at`, `withdrawn_at nullable`, `created_at`
  - 이메일·이름·전화번호 평문 컬럼은 두지 않는다. 복호화가 필요한 값은 `*_enc`, 정확 일치 조회는 `*_hmac`를 사용한다.
  - 로컬 비밀번호 해시는 롤백 호환 기간에 식별자 없는 BCrypt로 쓰며 `{bcrypt}$2...` 형식도 읽는다. 웹 입력은 UTF-8 72바이트 이하로 제한한다.
  - `credential_version`은 실제 비밀번호·로그인 수단 변경마다 증가하며 이전 버전으로 발급한 회원 세션을 거절한다. 로그인 중 BCrypt 작업 강도만 승격할 때는 버전을 유지한다.
  - `version`은 로그인 시각·휴대폰 확인·비밀번호처럼 같은 회원 행을 갱신하는 경로의 stale update를 막는 JPA 낙관적 락 버전이다.
  - `phone_hmac`은 null을 허용하되 값이 있으면 회원 전체에서 유일하다. 전화번호 변경은 새 번호 SMS 소유 확인 뒤 이 제약과 애플리케이션 조회로 중복을 거절한다.
  - 탈퇴는 미종결 결제 시도·주문·주문 클레임, `BOOKED` 예약, 미완료 예약 취소 후속 작업, 사용 가능한 미만료 8회권, 미완료 환불이 없을 때만 허용한다. 이메일·이름을 탈퇴 식별값으로 바꾸고 전화번호·비밀번호·소셜 연결을 제거한 뒤 `withdrawn_at`과 자격 버전을 갱신한다. 이후 일반 회원 조회와 로그인에서 제외하고 기존 세션을 폐기한다.
  - 종결 주문·예약의 `user_id`와 운영 이력은 유지한다. 관리자 과거 이력은 활성 회원 조회와 분리된 명시적 조회를 사용해 탈퇴 회원도 `MEMBER`로 반환하되, 익명화된 이름과 제거된 전화번호만 노출한다.
- `user_social_accounts`
  - `id`, `user_id`, `provider(GOOGLE|NAVER)`, `provider_id_enc nullable`, `provider_id_hmac`, `created_at`
  - 외부 식별자는 provider 내부에서만 고유하므로 `(provider, provider_id_hmac)`를 유일하게 유지한다. 평문은 저장하지 않고, V63 이전 행의 nullable 암호문은 다음 소셜 로그인에서 채운다.
  - 한 회원이 같은 provider의 계정을 둘 이상 연결하지 않도록 `(user_id, provider)`를 유일하게 유지한다.
- `guests`
  - `id`, `name_enc`, `name_hmac`, `phone_enc`, `phone_hmac`, `phone_verified`, `created_at`
  - 비회원 이름·전화번호 평문 컬럼은 두지 않는다. 표시는 암호문 복호화, 동등 검색은 HMAC으로 처리한다.
- `phone_verifications`
  - `id`, `phone_hmac`, `purpose`, `code_hmac`, `code_enc`, `delivered`, `verified`, `expires_at`, `created_at`
  - 전화번호와 인증 코드 평문은 저장하지 않는다. 인증은 전화번호·사용 목적·코드의 HMAC으로 조회하고, 로컬 전용 코드 조회는 `code_enc`를 복호화한다.
  - `purpose`는 회원가입, 비밀번호 재설정, 회원 전화번호 등록/변경, 비회원 예약/주문, 이력 가져오기와 이력/결제 복구를 구분한다. 한 목적으로 발급한 코드는 다른 목적에서 소비할 수 없다.
  - NHN이 발송 요청을 정상 접수해 `delivered=true`인 미소모·유효 코드만 인증할 수 있다. 발급 ID가 더 큰 코드의 접수 완료만 같은 번호·같은 목적의 이전 미소모 코드를 무효화하고, 늦게 끝난 이전 요청의 접수 완료는 폐기 상태를 되돌리지 않는다. 소비 조회는 `(phone_hmac, purpose, id)` 인덱스와 비관적 잠금으로 한 번만 성공한다.

#### 상품과 재고

- `products`
  - `id`, `name`, `type(READY_STOCK|MADE_TO_ORDER)`, `category nullable`, `price`, `description nullable`, `image_url nullable`, `specification nullable`, `care_instructions nullable`, `production_lead_days nullable`, `status(ACTIVE|INACTIVE)`, `version`
  - 주문제작은 `specification`과 1~180일 `production_lead_days`가 필수고, 기성품은 제작 기간을 두지 않는다.
  - 관리자는 표시 정보와 대표 이미지를 수정하고 상태를 별도 변경한다. `version` 낙관적 락으로 동시에 먼저 읽은 관리자 수정이 앞선 변경을 덮지 못하게 하고 충돌은 409로 반환한다. 공개 목록·상세와 주문 prepare는 `ACTIVE` 상품만 대상으로 하며, 없거나 비활성인 공개 상세는 동일하게 404로 응답한다.
- `product_option_groups`, `product_option_values`
  - 주문제작 상품의 `SELECT | TEXT` 그룹과 선택값을 안정적인 option key, 표시 순서, 필수 여부로 저장한다.
  - 직접입력 그룹은 안내 문구, 최대 200자 입력 길이와 추가 금액을 가진다. 선택형 그룹에는 선택값을 별도 행으로 둔다.
- `product_variants`, `product_variant_selections`
  - 주문제작 상품의 선택형 옵션 조합마다 `combination_key`, 가격 추가금, 재고, 판매 여부와 낙관적 락 버전을 저장한다.
  - 선택형 옵션이 없으면 `DEFAULT` 조합 한 개를 사용하고, 조합 선택 행은 그룹·값 ID와 표시 순서를 보존한다.
- `inventory`
  - `product_id(PK/FK)`, `quantity`, `version`, `updated_at`
  - `quantity >= 0`을 DB `CHECK` 제약으로도 강제한다.
- `inventory_adjustments`
  - `id`, `product_id(FK)`, `product_variant_id nullable`, `type(INCREASE|DECREASE)`, `quantity`, `quantity_before`, `quantity_after`, `reason`
  - `adjusted_by_admin_id nullable`, `adjusted_by`, `adjusted_at`
  - 관리자 수동 조정은 `inventory` 행 잠금 안에서 수량 변경과 이력 저장을 같은 트랜잭션으로 처리한다.
- `cart_items`
  - `id`, `user_id(FK)`, `product_id(FK)`, `product_variant_id nullable`, `line_key`, `qty`, `created_at`, `updated_at`
  - `(user_id, line_key)`를 유일하게 유지한다. `line_key`는 상품·SKU·정규화된 직접입력값을 식별한다.
- `cart_item_text_inputs`
  - `cart_item_id`, `option_group_id`, `option_key`, `value`, `sort_order`
  - 같은 SKU라도 직접입력 제작 문구가 다른 장바구니 행의 선택을 보존한다.
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
  - `access_token VARCHAR(64) nullable` — 비회원 접근 토큰의 SHA-256 hex 해시 저장. 복구 토큰 하나를 같은 비회원의 여러 주문이 공유할 수 있어 UNIQUE가 아니다.
  - `status`, `total_amount`, `product_amount`, `shipping_fee`, `coupon_discount_amount`, `reward_used_amount`, `pg_paid_amount`, `reward_earn_base_amount`, `issued_coupon_id nullable`, `paid_at`, `approval_deadline_at`, `bundle_id nullable`, `payment_key nullable`, `version`
  - `made_to_order_consent_version`, `made_to_order_consent_disclosure`, `made_to_order_consent_at` nullable — 주문제작 상품 결제 전 별도 고지·동의 스냅샷
  - `total_amount`는 쿠폰 할인 전 상품 합계와 배송비를 포함한다. 고객 결제 총액은 `total_amount - coupon_discount_amount`이고, 이를 적립금과 PG로 나눈 값이 각각 `reward_used_amount`, `pg_paid_amount`다. 배송비는 prepare 당시 서버 정책을 `shipping_fee`에 스냅샷으로 저장하고 픽업은 0원이다.
  - `issued_coupon_id`는 `issued_coupons.id`를 FK로 참조하며, 쿠폰 할인액이 0원보다 큰 주문만 발급 쿠폰을 참조한다.
- `order_items`
  - `id`, `order_id`, `product_id`, `product_variant_id nullable`, `product_name`, `product_type nullable`, `specification nullable`, `care_instructions nullable`, `production_lead_days nullable`, `qty`, `base_price`, `variant_price_adjustment`, `text_option_price_adjustment`, `unit_price`, `gross_amount`, `coupon_discount_amount`, `reward_used_amount`, `net_paid_amount`
  - 상품명·유형·기본가·옵션 추가금·최종 단가와 구매조건은 상품 변경과 무관하게 결제 준비 시점 표시를 보존한다. `product_type=null`인 기존 주문 항목은 알 수 없는 당시 조건을 현재 상품 값으로 역보정하지 않는다.
  - `qty`는 1~99, `unit_price`는 1원 이상이고 `gross_amount = qty * unit_price`이며, V121 `CHECK`가 도메인 산술 불변식을 DB에서도 강제한다.
- `order_item_option_snapshots`
  - `id`, `order_item_id`, `option_type`, `group_name`, `value`, `price_adjustment`, `sort_order`
  - 선택형 옵션명·값과 직접입력 문구를 당시 표시 순서로 보존하고 주문 항목 삭제 시 함께 제거한다.
- `order_approvals`
  - `id`, `order_id`, `decided_by_admin_id`, `decision`, `reason`, `decided_at`
- `fulfillments`
  - `id`, `order_id(unique)`, `type(SHIPPING|PICKUP)`, `expected_ship_date`, `pickup_deadline_at`, `shipping_address_enc nullable`, `carrier nullable`, `tracking_number nullable`, `version`
  - 주문 confirm 시 고객이 선택한 타입으로 함께 생성한다. `SHIPPING`의 구조화 배송지는 AES-GCM 암호문으로 저장하고 소유권이 확인된 고객 주문 상세와 관리자 단건 이행 조회에서만 복호화한다.
  - `carrier`, `tracking_number`는 배송 출발 시 한 쌍으로 저장한다. 픽업에는 둘 다 저장하지 않으며 DB `CHECK`로 강제한다.
- `refunds`
  - `id`, `order_id nullable`, `order_claim_id nullable`, `direct_order_id generated`, `booking_id nullable`, `pass_purchase_id nullable`, `payment_attempt_id nullable`
  - 예약, 직접 주문, 주문 클레임, 8회권, 결제 시도 보상 중 하나의 source와 고객 반환 총액 `amount > 0`, `customer_refund_amount`, `pg_refund_amount`, `reward_restore_amount`, `reward_revoke_amount`, `restore_coupon`, `payment_key`, `refund_transaction_key UNIQUE`, `idempotency_key UNIQUE`, `fail_reason`
  - 고객 반환 총액은 도메인 생성과 `chk_refunds_amount_positive`에서 이중 강제한다. PG 반환액이 0원인 적립금 전액 결제 주문도 환불 행과 후처리를 유지하되 외부 결제사 호출만 건너뛴다.
  - `V106`은 기존 0원 이하 환불 행을 자동 보정하지 않고 atomic `ALTER TABLE`을 실패시킨다. 배포 전 `refunds.amount <= 0` 데이터를 확인하고 근거에 따라 정리해야 한다.
  - 직접 주문 환불은 `direct_order_id`, 주문 클레임 환불은 `order_claim_id`, 나머지는 각 source FK의 UNIQUE로 원본당 한 건을 보장한다. 같은 주문 결제를 공유하는 여러 클레임 환불은 같은 `payment_key`를 가질 수 있다.
  - `status(REQUESTED|PROCESSING|RETRYABLE|RECONCILIATION_REQUIRED|SUCCEEDED|FAILED)`, `processing_at`, `processing_token`, `attempt_count`, `next_attempt_at`, `last_recovery_at`, `created_at`, `updated_at`, `version`
  - `RECONCILIATION_REQUIRED` 재선점은 취소 재호출보다 PG 취소 내역 조회를 먼저 수행한다. 취소 사유에 포함한 멱등키·금액·상태·거래 식별자가 모두 일치하는 실제 완료 취소면 성공으로 화해하고, 해당 멱등키의 취소가 없으며 미취소가 확정된 경우만 `RETRYABLE`로 전환한다.
  - 자동 복구는 `last_recovery_at`, 생성 시각, ID 순으로 후보를 순환해 반복 실패 환불이 뒤 요청을 계속 막지 않게 한다.
- `payment_attempt`
  - `id`, `order_id_external`, `context(ORDER|BOOKING|PASS)`, `amount`, `status`
  - `processing_at nullable`, `processing_token nullable`, `payment_key nullable`, `confirmed_payment_key nullable`, `fail_reason nullable`
  - `payload_enc`, `fulfilled_domain_id nullable`, `fulfilled_access_token_enc nullable`, `created_at`, `confirmed_at nullable`, `confirm_recovery_attempted_at nullable`, `version`
  - 내부 결제 payload는 AES-GCM 암호문으로 저장하고 claim·fulfillment 시점에만 복호화한다.
  - confirm을 선점할 때마다 새 `processing_token`을 발급하고 현재 토큰 소유자의 PG 결과만 상태에 반영한다.
  - 자동 복구 전에 `confirm_recovery_attempted_at`을 저장해 1분 backoff와 후보 순환을 보장한다.
  - `CONFIRMED` 결과의 도메인 ID와 비회원 접근 토큰 암호문을 저장해 동일 confirm 재호출에 같은 결과를 반환한다.
  - 상태: `PENDING | PROCESSING | RETRYABLE | APPROVED | CONFIRMED | FAILED | RECONCILIATION_REQUIRED | COMPENSATION_REQUESTED | COMPENSATION_FAILED | COMPENSATED | CANCELED`
  - PG 호출 전 확정 실패만 혜택 예약을 즉시 해제한다. PG 호출 가능성이 있는 실패는 `RECONCILIATION_REQUIRED`에서 쿠폰·적립금 예약을 보존하고 조회로 미승인이 확인된 뒤 해제한다.

#### 이벤트, 쿠폰과 적립금

- `events`
  - 제목·설명·이미지·게시 시작/종료·게시 여부·홈 추천 여부·낙관적 락 버전을 저장한다. 공개 조회는 게시되었고 종료되지 않은 이벤트만 반환한다.
- `event_products`
  - 이벤트와 연관 상품의 다대다 연결 및 표시 순서를 저장한다.
- `coupon_definitions`
  - 쿠폰 이름, 정액/정률 할인 조건, 최소 주문 금액, 발급 시작/종료와 사용 기한, 활성·공개 발급 여부를 저장한다. 한 장이라도 발급된 뒤에는 경제 조건을 변경하지 않는다.
- `issued_coupons`
  - 회원별 발급 쿠폰의 `AVAILABLE | RESERVED | REDEEMED | EXPIRED | CANCELED` 상태, 예약 결제 시도, 사용 주문과 시각을 저장한다. `(user_id, coupon_definition_id)` 유일 제약으로 공개 쿠폰 중복 발급을 막는다.
- `reward_accounts`, `reward_lots`
  - 계정은 사용 가능·예약·부채 합계를, 적립 단위는 원래/잔여 금액과 만료 시각을 저장한다. 만료가 가까운 lot부터 예약·사용한다.
- `reward_reservations`, `reward_reservation_allocations`
  - 결제 시도별 적립금 예약과 lot별 배분을 저장해 prepare/confirm/만료/대사를 멱등 처리한다.
  - `RESERVED`는 주문·해결 시각이 없고 복원액이 0원이다. `USED`는 주문·해결 시각이 있고 복원액이 예약액 범위에 있으며, `RELEASED`는 주문이 없는 해결 상태이고 복원액이 0원이다. V121 `CHECK`가 이 상태별 조합을 강제한다.
- `reward_ledger`
  - 적립·예약·사용·복원·만료·부채 상환의 모든 증감을 멱등키와 함께 보존한다.

#### 클래스, 슬롯, 예약

- `classes`
  - `id`, `name`, `category`, `duration_min`, `price`, `buffer_min`, `description nullable`, `image_url nullable`, `preparation_info nullable`, `target_audience nullable`, `pass_eligible`, `status(ACTIVE|INACTIVE)`
  - 공개 목록·상세와 슬롯 생성·결제는 `ACTIVE` 클래스만 대상으로 한다. 없거나 비활성인 공개 상세는 동일하게 404로 응답하지만, 슬롯·예약·결제 내부 흐름은 비활성 상태를 422로 구분한다. `pass_eligible`은 구매한 `PassPlan`의 카테고리 정책과 함께 8회권 사용 가능 여부를 결정한다.
  - `price`는 10원 이상이고 브라우저가 원 단위 정수를 정확히 표현하는 상한 이하여야 하며 `V99` CHECK로도 강제한다.
- `slots`
  - `id`, `class_id`, `start_at`, `end_at`, `capacity=8`, `booked_count`, `admin_active`, `buffer_block_count`
  - 실제 활성 상태는 `admin_active=true AND buffer_block_count=0`으로 판정한다.
- `bookings`
  - `id`, `user_id nullable`, `guest_id nullable`
  - `user_id`, `guest_id` 중 정확히 하나만 존재하도록 `chk_bookings_exactly_one_owner` `CHECK` 제약으로 강제한다.
  - `owner_phone_hmac` — 활성 예약 회원·비회원 소유자의 현재 전화번호 HMAC.
    `BOOKED` 상태에서는 반드시 존재하고 취소·완료·노쇼 상태에서는 제거한다.
  - `active_owner_phone_hmac generated` — `BOOKED`일 때만 `owner_phone_hmac`를 노출하고 `(slot_id, active_owner_phone_hmac)` UNIQUE로 회원·비회원 교차 중복을 막는다.
  - `active_user_id generated`, `active_guest_id generated`의 슬롯별 UNIQUE도 유지해 전화번호를 바꾼 같은 계정의 중복 예약을 막는다.
  - `access_token VARCHAR(64) nullable` — 게스트 예약 조회용 SHA-256 hex 해시 저장. 복구 토큰 하나를 같은 비회원의 여러 예약이 공유할 수 있어 UNIQUE가 아니다.
  - `class_id`, `slot_id`, `status`, `source(WEB|PHONE|NAVER_TALK|KAKAO|VISIT)`, `participant_count`
  - `deposit_amount`, `deposit_paid_at`, `payment_key nullable`
  - `deposit_amount`, `deposit_paid_at`, `balance_amount`, `balance_status`, `balance_paid_at`, `arrears_flag`, `version`
  - `participant_count`는 1~8이며 `slots.booked_count`에 인원 단위로 반영한다. 8회권 예약은 1명만 허용한다.
- `booking_history`
  - `id`, `booking_id`, `action`, `from_slot_id`, `to_slot_id`, `actor`, `reason`, `created_at`

카테고리는 고정 enum이 아니라 확장 가능한 문자열로 저장하며, 저장·조회 필터 기준은 앞뒤 공백을 제거한 대문자 토큰이다.
다만 구매 시 확정한 `PassPlan`은 이 정규화 토큰과 `pass_eligible`을 함께 사용해 이용권 적용 가능 여부를 판단한다.

#### 운영 콘텐츠와 공방 프로필

- `notices`
  - `id`, `title`, `content`, `pinned`, `view_count`, `version`, `created_at`
  - 상세 조회는 원자 UPDATE로 조회수를 증가시킨 뒤 최신 행을 읽는다.
  - `view_count`는 일반 엔티티 저장 대상에서 제외해 관리자가 제목·본문·고정 여부를 저장해도
    동시에 증가한 조회수를 덮지 않는다. 관리자 수정·삭제는 조회한 `version`을 `expectedVersion`으로
    제출해 오래된 화면의 변경을 먼저 거부하고, `@Version`으로 비교 직후의 경쟁도 막는다.

- `workshop_profiles`
  - `id=1`, `name`, `phone nullable`, `postal_code nullable`, `address_line1 nullable`, `address_line2 nullable`, `business_hours nullable`, `map_url nullable`, `parking_info nullable`, `business_registration_number nullable`, `representative_name nullable`, `email nullable`, `mail_order_registration_number nullable`, `introduction nullable`, `kakao_talk_id nullable`, `naver_talk_url nullable`, `naver_blog_url nullable`, `instagram_url nullable`, `smart_store_url nullable`, `updated_at`, `version`
  - 단일 행 `CHECK(id=1)`로 방문 안내와 공개 사업자 정보를 함께 관리한다. 공개 API는 같은 프로필을 반환하고 관리자 API만 수정한다. 관리자는 조회 응답의 `version`을 수정 요청의 `expectedVersion`으로 제출하고, 서비스 비교와 `@Version`이 오래된 화면 및 비교 직후의 동시 수정을 차단한다.
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

#### 예약 전화번호 소유자 제약 배포

- `V98`은 영구 DDL 전에 임시 테이블 `NOT NULL`·UNIQUE로 기존 `BOOKED` 예약의 현재 전화번호 HMAC과
  회원·비회원 교차 중복을 검증한다. 검증을 통과한 경우에만 `owner_phone_hmac`을 채우고
  generated column·CHECK·UNIQUE를 atomic `ALTER TABLE`로 추가한다. 종결 예약의 HMAC은 `NULL`로 유지한다.
- 같은 슬롯에 동일 전화번호의 회원 예약과 비회원 예약이 함께 있거나, 활성 예약 소유자의 전화번호 HMAC이 없으면 마이그레이션을 실패시킨다.
- 충돌 예약을 자동 취소하거나 한쪽 소유자로 합치지 않는다. 결제·예약 이력을 확인해 운영자가 정리한 뒤 Flyway를 다시 실행한다.
- 데이터 사전 검증이 아닌 예기치 않은 DB 중단으로 `owner_phone_hmac` 컬럼만 남았다면
  `information_schema`로 generated column·CHECK·인덱스의 부분 적용 여부를 확인하고 생성된 항목을 제거한 뒤 Flyway를 repair·재실행한다.
- 회원 전화번호 변경과 개인정보 키 회전은 회원·비회원 HMAC을 갱신한 같은 트랜잭션에서
  활성 예약 HMAC도 다시 계산한다.

#### 주문 클레임

- `order_claims`
  - 관리자 전체 작업함은 `(requested_at DESC, id DESC)`, 상태별 작업함은 `(status, requested_at DESC, id DESC)` 커서를 사용한다.
  - 두 조회 형태를 각각 같은 순서의 인덱스로 지원해 앞 페이지가 바뀌어도 중복·누락 없이 다음 작업을 조회한다.

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
  - `title`, `content`, `secret`
  - 일반글은 공개 상세를 허용한다. 비밀글은 공개 상세를 거절하고 로그인한 작성자 소유권 또는 관리자 권한으로만 조회한다.
  - `reply_content nullable`, `replied_at nullable`, `replied_by nullable`, `created_at`

- `inquiry`
  - `id`, `user_id`
  - `title`, `content`
  - `reply_content nullable`, `replied_at nullable`, `replied_by nullable`, `created_at`

#### 상품·클래스 후기

- `reviews`
  - `id`, `user_id`, `rating nullable`, `content nullable`, `status(PUBLISHED|HIDDEN)`, `content_revision`, `version`, `created_at`, `updated_at`
  - 상품 후기는 `order_item_id`, `product_id`를, 클래스 후기는 `booking_id`, `booking_class_id`를 가진다. 두 원천 쌍 중 정확히 하나만 존재하도록 DB `CHECK`로 강제한다.
  - `(order_item_id, product_id)`는 `order_items(id, product_id)`를, `(booking_id, booking_class_id)`는 `bookings(id, class_id)`를 복합 FK로 참조해 작성 원천과 후기 대상이 서로 다른 조합을 DB에서도 막는다.
  - `deleted_at`, `recreation_blocked`, `reserved_order_item_id generated`, `reserved_booking_id generated`로 작성자 삭제와 원천 점유를 분리한다. 활성 후기 또는 숨김 이력이 있는 tombstone만 생성 열에 원천 ID를 노출하고 각각 UNIQUE로 보호한다.
  - 작성자 삭제는 `rating`, `content`, 공식 답글과 `hidden_reason`, `hidden_at`, `hidden_by_admin_id`를 제거한 tombstone으로 남긴다. 숨김 이력이 없는 삭제본은 원천을 해제해 재작성을 허용하고 신고·moderation·증거가 없으면 30일 뒤 bounded 보존 배치가 파기한다. 한 번이라도 숨겨진 삭제본은 `recreation_blocked=true`로 원천을 계속 점유한다.
  - 숨김 상태는 `hidden_reason`, `hidden_at`, `hidden_by_admin_id`를 모두 가지며 게시 상태에서는 모두 `NULL`이다.
  - `edited_at`은 회원 본문 수정만 나타내며 moderation·답글 변경 시각과 분리한다. `content_revision`은 본문·평점·사진 변경에서만 증가하며 회원 수정과 관리자 심사의 콘텐츠 동시성을 보호한다. JPA `version`은 상태·답글을 포함한 모든 후기 행 쓰기 충돌을 보호하므로 관리자 상태 변경은 두 토큰을 함께, 답글 변경은 `version`을 비교한다. 공식 답글은 `reply_content`, `reply_admin_id`, `reply_created_at`, `reply_edited_at`을 한 묶음으로 저장한다.
  - 공개 목록과 평균은 삭제되지 않은 `PUBLISHED`만 대상으로 하고, 회원 소유 목록과 관리자 목록은 삭제되지 않은 숨김 상태도 반환한다.
- `review_moderation_actions`
  - 실제 `PUBLISHED <-> HIDDEN` 전이마다 동작, 이전·새 상태, 사유, 관리자, 시각과 `evidence_snapshot_id`를 보존한다.
  - 후기 부모 FK는 `ON DELETE RESTRICT`로 두어 보존기간보다 먼저 부모가 파기되지 않게 한다.
- `review_reports`
  - 후기·신고자별 한 건이며 `PENDING | ACCEPTED | REJECTED` 상태, 신고 사유·상세, 판단 관리자·시각을 저장한다.
  - 신고 시점 상태와 `evidence_snapshot_id`를 저장해 이후 수정·삭제와 무관하게 판단 근거를 유지한다.
  - 후기 부모 FK는 `ON DELETE RESTRICT`로 둔다.
- `review_evidence_snapshots`, `review_evidence_snapshot_images`
  - 신고·moderation 시점의 콘텐츠 revision, 별점, 본문, 수정 시각과 정렬된 사진 URL을 공통 불변 증거로 보존한다.
  - 미결 신고 증거는 만료 시각 없이 유지하고, 종결 신고·moderation 증거는 3년 뒤 사건 메타데이터와 함께 보존 배치에서 삭제한다. V124 이전 신고는 당시 사진을 복원할 수 없어 `LEGACY_REPORT`, `images_complete=false`다.
  - 증거 전용 또는 숨김·삭제 후기에서만 참조되는 파일은 공개 미디어 조회에서 제외하고 Bearer 관리자 증거 경로로만 제공한다. 참조 삭제는 커밋 뒤 다시 참조 여부를 확인한 다음 물리 파일을 지운다.
  - 증거 snapshot의 후기 부모 FK도 `ON DELETE RESTRICT`로 둔다.
- `review_helpful_votes`
  - `(review_id, user_id)` UNIQUE로 회원별 도움돼요를 멱등하게 유지한다.
- `review_images`
  - 후기별 `sort_order(0..4)`를 UNIQUE로 유지하고 `image_url`도 UNIQUE로 보호한다. 미디어 참조 스캔이 이 테이블을 포함해 연결된 파일을 고아로 판단하지 않는다.

후기 증거 스키마는 MySQL DDL의 비트랜잭션 특성 때문에 `V124`~`V136`을 한 번에 묶지 않는다. `V124`는
콘텐츠 revision, `V125`~`V126`은 증거·사진 테이블, `V127`은 과거 신고 증거 데이터, `V128`~`V131`은
moderation·신고의 증거 연결을 각각 추가·백필·확정한다. 삭제 tombstone은 `V132`에서 제약을 임시 완화하고
`V133`에서 숨김 메타데이터를 지운 뒤 `V134`에서 비식별화 제약을 다시 강화한다. `V135`~`V136`은
moderation·종결 신고 보존 조회 인덱스를 각각 추가한다. 각 버전은 단일 atomic DDL 또는 독립 DML 단계만
담아 실패한 migration이 뒤 불변식까지 부분 적용하지 않게 한다.

일반 삭제 tombstone의 bounded 정리를 위해 `V137`은 `(recreation_blocked,deleted_at,id)` 인덱스를 추가한다.
`V138`~`V140`은 신고·moderation·증거의 부모 FK를 테이블별 atomic `ALTER TABLE`에서 `RESTRICT`로 교체한다.
`V141`은 낮은 별점순의 `rating ASC, created_at DESC, id DESC` 혼합 방향을 만족하는 상품·클래스 인덱스를
추가한다. 데이터 파기는 Flyway DML로 한 번에 수행하지 않고 애플리케이션 배치가 최대 100건씩 실행한다.

#### 알림 outbox

- `notification_outbox`
  - `id`, 수신자 식별자, `event_type`, `aggregate_type`, `aggregate_id`, `idempotency_key`, `status`, 재시도 시각과 횟수, `processed_at`, `read_at`
  - 상태는 `PENDING → PROCESSING → SENT|OBSOLETE|FAILED`를 기본 흐름으로 사용한다. `OBSOLETE`는 발송 직전 현재 도메인 상태·시간 구간과 맞지 않아 외부 채널 호출 없이 종결된 시간 의존 리마인드다. 같은 aggregate가 미래 유효 구간에 다시 들어오면 배치만 같은 행을 `OBSOLETE → PENDING`으로 재활성화할 수 있다.
  - `processing_token`, `locked_at`, `version`으로 재선점 전 실행의 오래된 결과 반영을 차단한다.
  - 리마인드 후보는 `(event_type, aggregate_type, aggregate_id)`로 이미 접수된 도메인 이벤트를 제외한다. 멱등키 문자열 형식이 달라도 같은 이력으로 인식한다.
  - `V108`은 영속 enum 확장에 맞춰 `status` 컬럼 comment에 `OBSOLETE`를 반영한다.

#### 8회권

- `pass_purchases`
  - `id`, `user_id NOT NULL`, `purchased_at`, `expires_at`, `plan_code`, `total_credits=8`, `remaining_credits`, `total_price`, `payment_key nullable`, `version`
  - `plan_code`는 구매 시점 이용권 계약 스냅샷이다. 신규 구매는 `REGULAR_CRAFT_8`, 정책 도입 전 데이터는 `LEGACY_ALL_CLASSES`를 사용한다.
  - `V105`는 기존 `fk_pass_user`를 내리고 `user_id NOT NULL` 변경과 `fk_pass_user_v105` 재생성을 하나의 atomic `ALTER TABLE`로 적용한다. MySQL은 같은 ALTER 안에서 제거한 FK 이름을 즉시 재사용할 수 없어 새 이름을 쓴다. 기존 소유자 없는 행은 자동 귀속하지 않고 migration을 실패시키며, 실패한 DDL의 부분 적용이나 FK 유실을 남기지 않는다.
  - 서로 다른 테이블의 DDL은 하나의 Flyway migration 트랜잭션으로 묶이지 않으므로 환불 금액 제약은 별도 `V106`으로 분리한다. 각 migration은 단일 atomic DDL만 실행해 오염 데이터로 실패해도 다른 불변식이 부분 적용된 failed migration을 남기지 않는다.
- `pass_ledger`
  - `id`, `pass_purchase_id`, `type(EARN|USE|REFUND|EXPIRE)`, `amount`, `related_booking_id nullable`, `created_at`

#### 주요 인덱스

- `orders(status, created_at, id)` 커서 조회
- `orders(user_id, created_at DESC, id DESC)` 회원 주문 커서 조회
- `orders(access_token, created_at DESC, id DESC)` 비회원 복구 주문 커서 조회
- `bookings(user_id, created_at DESC, id DESC)` 회원 예약 커서 조회
- `bookings(access_token, created_at DESC, id DESC)` 비회원 복구 예약 커서 조회
- `pass_purchases(user_id, purchased_at DESC, id DESC)` 회원 8회권 커서 조회
- `inquiry(user_id, created_at DESC, id DESC)` 회원 문의 커서 조회
- `inquiry(created_at DESC, id DESC)` 관리자 문의 커서 조회
- `product_qna(product_id, created_at DESC, id DESC)` 공개·관리자 상품 Q&A 커서 조회
- `product_qna(product_id, user_id, created_at DESC, id DESC)` 작성자 상품 Q&A 커서 조회
- `reviews(product_id, status, deleted_at, created_at, id)` 공개 상품 후기 최신순·평균 조회
- `reviews(product_id, status, deleted_at, rating, created_at, id)` 공개 상품 후기 별점 필터·정렬 조회
- `reviews(product_id, status, deleted_at, rating ASC, created_at DESC, id DESC)` 공개 상품 후기 낮은 별점순 조회
- `reviews(booking_class_id, status, deleted_at, created_at, id)` 공개 클래스 후기 최신순·평균 조회
- `reviews(booking_class_id, status, deleted_at, rating, created_at, id)` 공개 클래스 후기 별점 필터·정렬 조회
- `reviews(booking_class_id, status, deleted_at, rating ASC, created_at DESC, id DESC)` 공개 클래스 후기 낮은 별점순 조회
- `reviews(user_id, deleted_at, created_at, id)` 회원 후기 커서 조회
- `reviews(deleted_at, status, created_at, id)` 관리자 상태별 후기 커서 조회
- `reviews(deleted_at, created_at, id)` 관리자 무필터 후기 커서 조회
- `reviews(recreation_blocked, deleted_at, id)` 증거 없는 일반 삭제 후기 보존 만료 후보 조회
- `review_moderation_actions(review_id, created_at, id)` 후기별 운영 이력 조회
- `review_moderation_actions(created_at, id)` 3년 지난 운영 이력 보존 만료 조회
- `review_reports(status, created_at, id)` 관리자 신고 상태별 조회
- `review_reports(decided_at, id)` 3년 지난 종결 신고 보존 만료 조회
- `review_helpful_votes(user_id, review_id)` 회원별 후기 반응 일괄 조회
- `review_images(review_id, sort_order, id)` 후기 사진 표시 순서 조회
- `review_evidence_snapshots(retention_until, id)` 종결 후기 분쟁 증거 보존 만료 조회
- `review_evidence_snapshot_images(snapshot_id, sort_order)`, `review_evidence_snapshot_images(image_url)` 후기 분쟁 증거 사진 순서와 미디어 참조 조회
- `payment_attempt(order_id_external)` UNIQUE
- `payment_attempt(status, created_at)` 미완료 결제 시도 정리 후보 조회
- `payment_attempt(status, id, created_at)` 결제 준비 만료 배치의 ID 키셋 순회
- `payment_attempt(status, confirm_recovery_attempted_at, created_at)` confirm 자동 복구 backoff·후보 조회
- `users(email_hmac)` UNIQUE, `users(phone_hmac)` UNIQUE, `users(name_hmac)` 정확 일치 검색
- `guests(phone_hmac)` UNIQUE, `guests(name_hmac)` 정확 일치 검색
- `user_social_accounts(provider, provider_id_hmac)` UNIQUE
- `phone_verifications(phone_hmac, id)` 최신 인증 조회
- `phone_verifications(expires_at, id)` 보존 기간 만료 행을 100건씩 짧게 삭제
- `cart_merge_requests(created_at, user_id, idempotency_key)` 보존 기간 만료 삭제
- `inventory(product_id, version)`
- `inventory_adjustments(product_id, adjusted_at, id)` 최근 수동 조정 이력 조회
- `product_variants(product_id, active, id)` 주문제작 상품의 판매 가능 SKU 일괄 조회
- `inventory_adjustments(product_variant_id, adjusted_at DESC, id DESC)` 주문제작 SKU 수동 조정 이력 조회
- `notification_outbox(user_id, status, processed_at DESC, id DESC)` 회원 알림함 조회
- `notification_outbox(guest_id, status, processed_at DESC, id DESC)` 수신자별 발송 완료 조회
- `notification_outbox(status, processed_at, id)` `SENT|OBSOLETE|FAILED` terminal outbox 보존 정리
- `notification_outbox(event_type, aggregate_type, aggregate_id)` 예약·8회권·픽업 리마인드 접수 이력 조회
- `notification_log(sent_at, id)` 채널 감사 로그 보존 정리
- `notification_log(user_id, event_type, status, sent_at)` 회원 알림 중복 확인
- `notification_log(guest_id, event_type, status, sent_at)` 비회원 알림 중복 확인
- `product_qna(replied_at, created_at DESC, id DESC)` 관리자 미답변 작업함 커서 조회
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

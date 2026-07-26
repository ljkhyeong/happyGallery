# ADR-0009 예약금 결제 정책 반영 (§6.2)

- **날짜**: 2026-02-26
- **상태**: 결정됨

---

## 컨텍스트

고객이 직접 만드는 `WEB` 예약금은 온라인 PG(카드/간편결제)만 허용하고 계좌이체는 차단해야 한다.
운영자가 전화·메신저·방문 접수의 오프라인 입금을 기록하는 경로는 PG 결제가 아니므로 별도다.
슬롯 변경 시 예약금은 유지(재결제 없음)되어야 하고,
D-1 환불 경계는 §5.4/§6.1에서 이미 강제된다.

---

## 결정 1 — `DepositPaymentMethod` 열거형을 `domain/booking`에 둔다

**선택**: `CARD | EASY_PAY | BANK_TRANSFER`

**이유**: 결제 수단은 예약 도메인의 정책이므로 `domain/booking`에 위치가 적합하다. `order` 도메인에 있는 `FulfillmentType`과 분리해 예약-결제 정책을 독립적으로 관리한다.

---

## 결정 2 — 계좌이체 차단은 서비스 레이어에서 한다

**선택**: 표준 고객 결제의 `BookingPreparer` 진입부에서 `paymentMethod == BANK_TRANSFER`이면
`PaymentMethodNotAllowedException` (HTTP 422)을 즉시 던진다. 관리자 수기 예약은 공개 결제 payload를
사용하지 않고, 입금 완료이면 `BANK_TRANSFER`, 미입금이면 `null`을 서버가 정한다.

**이유**: DTO 레벨(@Valid)에서는 유효한 enum 값인지만 검사한다. 어떤 값이 허용되는지는 비즈니스 규칙이므로 서비스 레이어가 책임진다. 도메인 엔티티(`Booking`)는 어떤 `DepositPaymentMethod`든 저장할 수 있도록 제한을 두지 않는다 — 미래에 특수 케이스(관리자 입력 등)를 허용할 여지를 남긴다.

---

## 결정 3 — `bookings.payment_method` 컬럼은 NULL 허용 (V4 마이그레이션)

**선택**: `payment_method VARCHAR(15) NULL`

**이유**: §5.x에서 이미 생성된 예약 레코드와 오프라인 입금 전 예약을 표현해야 한다. 고객 PG 예약은
항상 `CARD|EASY_PAY`, 운영자 수기 예약은 입금 완료 여부에 따라 `BANK_TRANSFER|null`이다.

---

## 결정 4 — "변경 시 예약금 유지"는 별도 코드 없이 §5.3 구현으로 충족

**선택**: `BookingRescheduleService`는 `slot`만 교체하며 `depositAmount`를 건드리지 않는다. 코드 추가 없음. 대신 `reschedule_success_and_5times_proofTest`에 `depositAmount` 불변 단언 추가.

**이유**: 과잉 구현 방지. 테스트 단언으로 회귀를 방지하는 것으로 충분하다.

---

## 결정 5 — 예약금과 잔금의 실제 결제 시각을 각각 기록

- 결제 confirm 트랜잭션에서 예약금 예약을 생성할 때 `deposit_paid_at`을 기록한다.
- 현장 잔금 결제는 관리자 명령으로 `balance_status=PAID`, `balance_paid_at=현재 시각`을 함께 갱신한다.
- 8회권 예약처럼 `balance_amount=0`인 예약은 생성 시점부터 `balance_status=PAID`이며
  실제 현장 결제가 없으므로 `balance_paid_at`은 비워 둔다.
- 잔금 결제 완료 시 `arrears_flag=false`로 정리하고, 결제 완료 잔금을 다시 미수로 표시하지 못하게 한다.
- 잔금이 있는 예약을 `PAID`로 처리한 뒤에는 고객 취소를 막는다. 기존 고객 취소는 예약금만 PG 환불하므로, 현장 수납한 잔금은 관리자 정산 없이 취소 상태로 넘기지 않도록 한다.

실제 돈이 이동한 시각을 별도 컬럼으로 보존해야 생성 시각과 정산 시각이 다른 현장 결제를 기간 매출에
정확히 반영할 수 있다.

## 결정 6 — 수업 완료와 잔금 정산 상태를 분리

- `BOOKED -> COMPLETED`는 슬롯 종료 이후에만 허용한다.
- 잔금이 미결제라면 운영자가 `arrears_flag=true`로 명시한 뒤에만 완료할 수 있다.
- 완료 후에도 미수금을 받을 수 있으며, 결제 처리 시 미수 표시는 자동 해제된다.
- 완료는 `BookingHistoryAction.COMPLETED` 이력을 추가하지만 이미 진행된 수업의 `booked_count`나
  버퍼 차단 수는 변경하지 않는다.
- 잔금 결제와 미수 설정·해제도 실제 상태가 달라질 때만 `BALANCE_PAID`, `ARREARS_MARKED`,
  `ARREARS_CLEARED` 관리자 이력을 같은 트랜잭션에 남긴다.
- 예약의 `@Version`과 단일 트랜잭션으로 잔금·미수·완료 동시 갱신 충돌을 감지한다.

---

## 결과

| 파일 | 역할 |
|------|------|
| `domain/booking/DepositPaymentMethod.java` | CARD / EASY_PAY / BANK_TRANSFER 열거형 |
| `db/migration/V4__add_payment_method.sql` | bookings.payment_method 컬럼 추가 |
| `domain/booking/Booking.java` | paymentMethod 필드 + 생성자 파라미터 |
| `domain/error/ErrorCode.java` | PAYMENT_METHOD_NOT_ALLOWED (422) 추가 |
| `domain/error/PaymentMethodNotAllowedException.java` | 예외 클래스 |
| `application/.../payment/context/booking/BookingPreparer.java` | 표준 결제 진입점에서 BANK_TRANSFER 차단, 예약금·잔금 확정 |
| `application/.../payment/port/in/PaymentPayload.java` | 공개 `BookingPayload`와 내부 `PreparedBookingPayload` 분리 |
| `adapter-in-web/.../payment/PaymentController.java` | 결제 prepare/confirm 단일 진입점 |
| `GuestBookingUseCaseIT` | bankTransfer_returns422 Proof 테스트 |
| `BookingRescheduleUseCaseIT` | depositAmount 불변 단언 추가 |
| `db/migration/V57__add_booking_balance_paid_at.sql` | 잔금 결제 시각, 정합성 제약, 기간 조회 인덱스 추가 |
| `DefaultBookingSettlementService` | 잔금 결제·미수·수업 완료를 단일 트랜잭션으로 처리 |

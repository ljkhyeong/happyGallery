# ADR-0009 예약금 결제 정책 반영 (§6.2)

- **날짜**: 2026-02-26
- **상태**: 결정됨

---

## 컨텍스트

예약금은 온라인 PG(카드/간편결제)만 허용하고 계좌이체는 차단해야 한다.
슬롯 변경 시 예약금은 유지(재결제 없음)되어야 하고,
D-1 환불 경계는 §5.4/§6.1에서 이미 강제된다.

---

## 결정 1 — `DepositPaymentMethod` 열거형을 `domain/booking`에 둔다

**선택**: `CARD | EASY_PAY | BANK_TRANSFER`

**이유**: 결제 수단은 예약 도메인의 정책이므로 `domain/booking`에 위치가 적합하다. `order` 도메인에 있는 `FulfillmentType`과 분리해 예약-결제 정책을 독립적으로 관리한다.

---

## 결정 2 — 계좌이체 차단은 서비스 레이어에서 한다

**선택**: `GuestBookingService.createGuestBooking()` 진입부에서 `paymentMethod == BANK_TRANSFER`이면 `PaymentMethodNotAllowedException` (HTTP 422) 즉시 throw.

**이유**: DTO 레벨(@Valid)에서는 유효한 enum 값인지만 검사한다. 어떤 값이 허용되는지는 비즈니스 규칙이므로 서비스 레이어가 책임진다. 도메인 엔티티(`Booking`)는 어떤 `DepositPaymentMethod`든 저장할 수 있도록 제한을 두지 않는다 — 미래에 특수 케이스(관리자 입력 등)를 허용할 여지를 남긴다.

---

## 결정 3 — `bookings.payment_method` 컬럼은 NULL 허용 (V4 마이그레이션)

**선택**: `payment_method VARCHAR(15) NULL`

**이유**: §5.x에서 이미 생성된 예약 레코드(개발/테스트 DB)를 깨지 않기 위해 nullable로 추가. 신규 예약은 서비스 레이어에서 항상 값이 주입되므로 실제 null이 들어오는 경우는 없다.

---

## 결정 4 — "변경 시 예약금 유지"는 별도 코드 없이 §5.3 구현으로 충족

**선택**: `BookingRescheduleService`는 `slot`만 교체하며 `depositAmount`를 건드리지 않는다. 코드 추가 없음. 대신 `reschedule_success_and_5times_proofTest`에 `depositAmount` 불변 단언 추가.

**이유**: 과잉 구현 방지. 테스트 단언으로 회귀를 방지하는 것으로 충분하다.

---

## 결과

| 파일 | 역할 |
|------|------|
| `domain/booking/DepositPaymentMethod.java` | CARD / EASY_PAY / BANK_TRANSFER 열거형 |
| `db/migration/V4__add_payment_method.sql` | bookings.payment_method 컬럼 추가 |
| `domain/booking/Booking.java` | paymentMethod 필드 + 생성자 파라미터 |
| `common/error/ErrorCode.java` | PAYMENT_METHOD_NOT_ALLOWED (422) 추가 |
| `common/error/PaymentMethodNotAllowedException.java` | 예외 클래스 |
| `app/booking/GuestBookingService.java` | 진입부 BANK_TRANSFER 차단 |
| `app/web/booking/dto/CreateGuestBookingRequest.java` | @NotNull paymentMethod 필드 추가 |
| `app/web/booking/BookingController.java` | paymentMethod 서비스로 전달 |
| `GuestBookingUseCaseIT` | bankTransfer_returns422 Proof 테스트 |
| `BookingRescheduleUseCaseIT` | depositAmount 불변 단언 추가 |

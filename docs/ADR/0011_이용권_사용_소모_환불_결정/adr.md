# ADR-0011: 8회권 사용/소모/환불 구현 결정

**날짜**: 2026-02-28
**상태**: 확정

---

## 컨텍스트

§7.2: 8회권 크레딧 소모(예약 연결), 결석 처리, 정산 환불 + 미래 예약 자동 취소를 구현해야 한다.
"크레딧이 돈이다" 원칙 — ledger 선행 기록 → 잔액 변경 순서는 §7.1에서 확립된 규칙.

---

## 결정 1: 예약–8회권 연결 (`pass_purchase_id` FK)

**결정**: `bookings` 테이블에 `pass_purchase_id BIGINT NULL FK` 추가 (V5 마이그레이션).

**이유**:
- `isPassBooking()` 판별, 환불 시 미래 예약 조회 `findFuturePassBookings()` 모두 이 FK에 의존
- nullable: 기존 예약금 결제 예약과의 하위 호환성 유지

**리스크**: 예약 생성 시 passId/depositAmount 경로 분기가 서비스 레이어에 집중됨 → 단일 책임 위반 소지

---

## 결정 2: 결석(NO_SHOW) — 크레딧 추가 변동 없음

**결정**: `PassNoShowService.markNoShow()` 는 상태 전이(`BOOKED → NO_SHOW`)와 이력 기록만 수행. 크레딧 추가 소모·복구 없음.

**이유**: 크레딧은 예약 생성 시 이미 `USE ledger(-1)` + `useCredit()`으로 소멸. "크레딧이 돈이다" 원칙 — 돈을 쓴 뒤 결석해도 환불 없음.

**리스크**: 결석 취소(NO_SHOW → BOOKED) 시나리오가 현재 없음 → 운영 정책 미확정 상태

---

## 결정 3: D-1 이후 취소 — 크레딧 소멸 유지

**결정**: `BookingCancelService`에서 `isPassBooking() && !refundable` 분기는 ledger/크레딧 변동 없이 `booking.cancel()`만 수행.

**이유**: 당일 변경 불가 정책과 동일 경계(D-1 00:00 Asia/Seoul). PG 결제처럼 "취소해도 돈은 안 돌아온다".

---

## 결정 4: 정산 환불 — PG 자동 처리 없음

**결정**: `PassRefundService.refundPass()`는 잔여 크레딧 소멸 + REFUND ledger 기록 + 미래 예약 취소까지만 수행한다. 현재 8회권 구매는 결제 API로 생성되지만, 8회권 전체 환불 엔드포인트는 아직 PG 자동 환불을 호출하지 않고 `refundAmount` 계산값을 응답에 포함해 관리자가 처리한다.

**이유**:
- 8회권 환불은 잔여 크레딧 정산, 미래 예약 취소, 운영자 확인이 함께 필요한 관리자 액션이다.
- `refundAmount = remainingCredits × (totalPrice / totalCredits)` 로 단순 계산

**리스크**:
- 관리자 수동 처리이므로 환불 누락 위험 → 운영 프로세스로 보완 필요
- 결제 API 도입 후 `totalPrice`는 서버 설정 `PASS_TOTAL_PRICE`로 확정되지만, 기존 데이터에 `totalPrice=0`이 있으면 환불액도 0으로 계산된다.

---

## 결정 5: `PurchasePassRequest.totalPrice` — nullable Long

**결정**: 2026-04-26 결제 API 도입 후 구매 생성 요청에서 `totalPrice`를 받지 않는다. 가격은 `app.pass.total-price` (`PASS_TOTAL_PRICE`, 기본 240000)로 서버가 확정한다.

**이유**: 클라이언트 금액 변조를 막고, prepare 단계의 서버 산출 금액과 confirm 금액을 일치시켜야 한다.

**리스크**: 운영 가격 변경 시 환경 변수와 안내 문구가 함께 맞아야 한다.

---

## 구현 파일

| 파일 | 역할 |
|------|------|
| `domain/Booking.java` | `passPurchase` FK 필드, `markNoShow()`, `isPassBooking()` |
| `application/.../booking/DefaultBookingNoShowService.java` | 결석 처리 |
| `application/.../pass/DefaultPassRefundService.java` | 정산 환불 + 미래 예약 자동 취소 |
| `application/.../booking/DefaultBookingCancelService.java` | D-1 이후 취소 시 크레딧 소멸 유지 분기 |
| `adapter-out-persistence/.../booking/BookingRepository.java` | `findFuturePassBookings()` JPQL 쿼리 |
| `adapter-in-web/.../admin/AdminBookingController.java` | `POST /admin/bookings/{id}/no-show` |
| `bootstrap/src/main/resources/db/migration/V5__add_pass_booking_link.sql` | bookings.pass_purchase_id FK, pass_purchases.total_price |

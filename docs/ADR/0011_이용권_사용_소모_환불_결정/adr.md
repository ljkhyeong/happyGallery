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

**결정**: `PassRefundService.refundPass()`는 잔여 크레딧 소멸 + REFUND ledger 기록 + 미래 예약 취소까지만 수행. 실제 PG 환불은 `refundAmount` 계산값을 응답에 포함하고 관리자가 수동 처리.

**이유**:
- 8회권 결제 자체가 PG 추상화(`PaymentProvider`) 밖에서 직접 처리 중 (MVP)
- `refundAmount = remainingCredits × (totalPrice / totalCredits)` 로 단순 계산

**리스크**:
- 관리자 수동 처리이므로 환불 누락 위험 → 운영 프로세스로 보완 필요
- `totalPrice`가 0이면 환불액도 0 (MVP에서 totalPrice 미입력 허용으로 발생 가능)

---

## 결정 5: `PurchasePassRequest.totalPrice` — nullable Long

**결정**: 기존 테스트 호환성 유지를 위해 `long → Long` (nullable). null이면 컨트롤러에서 0으로 폴백.

**이유**: §7.1 구현 당시 기존 테스트가 `totalPrice` 없이 `guestId`만 보내는 패턴을 사용. primitive `long` 타입 시 Jackson이 null 파싱 실패(400).

**리스크**: `totalPrice=0`이면 정산 환불 계산 불가 → 실제 PG 연동 시 필수값으로 전환 필요

---

## 구현 파일

| 파일 | 역할 |
|------|------|
| `domain/Booking.java` | `passPurchase` FK 필드, `markNoShow()`, `isPassBooking()` |
| `app/PassNoShowService.java` | 결석 처리 |
| `app/PassRefundService.java` | 정산 환불 + 미래 예약 자동 취소 |
| `app/BookingCancelService.java` | D-1 이후 취소 시 크레딧 소멸 유지 분기 |
| `infra/BookingRepository.java` | `findFuturePassBookings()` JPQL 쿼리 |
| `web/AdminBookingController.java` | `POST /admin/bookings/{id}/no-show` |
| `web/AdminPassController.java` | `POST /admin/passes/{id}/refund` |
| `db/V5__add_pass_booking_link.sql` | bookings.pass_purchase_id FK, pass_purchases.total_price |

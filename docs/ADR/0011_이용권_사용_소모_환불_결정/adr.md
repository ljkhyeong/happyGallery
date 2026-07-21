# ADR-0011: 8회권 사용/소모/환불 구현 결정

**날짜**: 2026-02-28
**상태**: 확정

---

## 컨텍스트

§7.2: 8회권 크레딧 소모(예약 연결), 결석 처리, 정산 환불 + 미래 예약 자동 취소를 구현해야 한다.
"크레딧이 돈이다" 원칙에 따라 ledger와 잔액 변경은 항상 같은 트랜잭션에서 함께 커밋한다.

---

## 결정 1: 예약–8회권 연결 (`pass_purchase_id` FK)

**결정**: `bookings` 테이블에 `pass_purchase_id BIGINT NULL FK` 추가 (V5 마이그레이션).
회원이 예약 가능 슬롯을 직접 선택해 8회권 예약을 한 회차씩 생성하며, 운영자가 8회 일정을 일괄 배정하는
별도 흐름은 두지 않는다.
8회권 예약 생성 시 `USE` 원장은 저장된 `booking_id`를 `pass_ledger.related_booking_id`에 남긴다.
예약 취소로 1크레딧을 복구하는 `REFUND` 원장도 같은 예약 ID를 남긴다.
8회권 전체 환불처럼 단일 예약이 원인이 아닌 원장은 `related_booking_id`를 비운다.
예약 생성에서는 회원 소유권만 먼저 확인하고, `PassPurchase.useCredit(usedAt)`이 만료·잔여 크레딧 검증과
차감을 한 번에 수행한다. 이후 `USE` 원장을 같은 트랜잭션에 저장하며, 어느 한쪽이라도 실패하면 모두 롤백한다.
크레딧 차감 전에는 구매 시 저장한 `PassPlan`으로 클래스 카테고리와 `passEligible`을 함께 검증한다.
신규 `REGULAR_CRAFT_8`은 `passEligible=true`인 정규 공예 클래스에만 사용할 수 있고 `PERFUME`에는 사용할 수 없다.
`UNIQUE(related_booking_id, type)`으로 같은 예약에 `USE` 또는 예약 취소 `REFUND`가 중복 기록되는 것도 차단한다.

**이유**:
- `isPassBooking()` 판별, 환불 시 미래 예약 조회 `findFutureBookedPassBookings()` 모두 이 FK에 의존
- nullable: 기존 예약금 결제 예약과의 하위 호환성 유지
- 크레딧 사용/복구 원장과 원인 예약을 양방향으로 추적할 수 있어 운영 감사와 장애 대응이 쉬워진다.

**리스크**: 예약 생성 시 passId/depositAmount 경로 분기가 서비스 레이어에 집중됨 → 단일 책임 위반 소지

### 동시성 보완 (2026-07-20)

크레딧을 변경하는 모든 흐름은 `pass_purchases` 행을 `SELECT FOR UPDATE`로 먼저 잠근다.

- 8회권 예약: 8회권 잠금·소유권 확인 → 클래스·슬롯 잠금 → 예약 저장 → `USE` 원장·크레딧 차감
- 8회권 예약 취소: 8회권 잠금 → 클래스·슬롯 잠금·반납 → 기한 내 취소라면 `REFUND` 원장·크레딧 복구
- 8회권 전체 환불: 8회권 잠금 → 미래 예약의 모든 클래스 PK 순 선잠금 → 슬롯 PK 순 잠금·취소 → 환불 원장·잔액 소멸
- 만료: 8회권 잠금 → `EXPIRE` 원장·잔액 소멸

예약 취소의 크레딧 복구와 전체 환불은 이용권 잠금 직후 `expiresAt`을 현재 시각과 다시 비교한다.
만료된 이용권에 잔액이 남아 있으면 `EXPIRE` 원장과 잔액 0을 먼저 같은 트랜잭션에서 확정한다.
예약 취소 자체는 완료하되 크레딧은 복구하지 않고, 전체 환불은 미래 예약 취소나 PG 환불 요청 없이
`PASS_EXPIRED`로 거절한다. 전체 환불의 만료 정규화 트랜잭션을 먼저 커밋하고 바깥 서비스에서 예외로 변환해,
오류 응답 때문에 만료 이력이 롤백되지 않게 한다.

잠긴 `PassPurchase`를 크레딧 변경 메서드에 전달해 클래스·슬롯 잠금 뒤 일반 조회로 8회권을 다시 적재하는
숨은 역순 경로를 허용하지 않는다. 예약 변경은 동일 클래스 안에서만 허용하고, 여러 클래스의 미래 예약을
취소하는 8회권 전체 환불은 클래스 PK 순서를 유지한다. 이 순서는 서로 다른 클래스에서 같은 8회권을
동시에 쓰는 경우와 예약·취소·전체 환불·만료 경합을 동일하게 직렬화한다.

---

## 결정 2: 결석(NO_SHOW) — 크레딧 추가 변동 없음

**결정**: `DefaultBookingNoShowService.markNoShow()`는 주입된 `Clock` 기준으로 슬롯 종료 시각에 도달한 뒤에만
상태 전이(`BOOKED → NO_SHOW`)와 이력 기록을 수행한다. 크레딧 추가 소모·복구는 없다.

**이유**: 미래 예약을 운영자 실수로 노쇼 처리하는 것을 막는다. 크레딧은 예약 생성 시 이미
`USE ledger(-1)` + `useCredit()`으로 소모됐으므로 실제 결석 뒤에도 추가 차감하거나 복구하지 않는다.

**리스크**: 결석 취소(NO_SHOW → BOOKED) 시나리오가 현재 없음 → 운영 정책 미확정 상태

---

## 결정 3: D-1 이후 취소 — 크레딧 소멸 유지

**결정**: `BookingCancelService`에서 `isPassBooking() && !refundable` 분기는 ledger/크레딧 변동 없이 `booking.cancel()`만 수행.

**이유**: 당일 변경 불가 정책과 동일 경계(D-1 00:00 Asia/Seoul). PG 결제처럼 "취소해도 돈은 안 돌아온다".

---

## 결정 4: 정산 환불 — PG 환불 이력과 재시도

**결정**: 관리자와 이용권 소유 회원은 같은 정산 유스케이스로 전체 환불을 요청할 수 있다. 회원 경로는
`POST /api/v1/me/passes/{id}/refund`이며 세션 소유권을 검증하고 사용자별 처리율 제한을 적용한다.
`PassRefundService.refundPass()`는 미래 예약 취소, 잔여 크레딧 소멸, REFUND ledger 기록과 함께 `payment_key` 기반 PG 환불을 요청한다. 환불 이력은 `refunds.pass_purchase_id`로 8회권을 추적하며, 명시적 거절은 `FAILED`, 일시 실패와 결과 불명은 자동 복구 가능한 상태로 남긴다. 자동 취소한 미래 예약은 아직 사용하지 않은 크레딧이므로 `refundCredits = remainingCredits + canceledFutureBookings`로 정산한다.
`expiresAt`에 도달한 이용권은 이 정산 환불 대상에서 제외한다.

**이유**:
- 8회권 환불은 잔여 크레딧 정산, 미래 예약 취소와 PG 환불 추적이 한 번에 필요한 소유자 또는 관리자 액션이다.
- `refundAmount = (totalPrice × refundCredits) / totalCredits`로 비례 계산한다. 원 단위 미만은 버리되 전체 횟수 환불은 원결제액과 정확히 같게 한다.
- 주문/예약 환불과 동일하게 PG 호출 실패를 durable한 환불 이력으로 남겨야 실제 금전 환불과 도메인 상태의 불일치를 운영자가 추적할 수 있다.

**리스크**:
- PG 환불이 완료되지 않아도 미래 예약 취소와 크레딧 소멸은 완료된다. 자동 복구와 관리자 재처리 API로 금전 환불을 보완한다.
- 결제 API 도입 후 `totalPrice`는 서버 설정 `PASS_TOTAL_PRICE`로 확정되지만, 기존 데이터에 `totalPrice=0`이 있으면 환불액도 0으로 계산된다.
- 과거 데이터에 `payment_key`가 없으면 PG 환불을 실행할 수 없으므로 `FAILED` 이력으로 남긴 뒤 운영자가 수동 확인한다.
- 회원 환불 응답은 취소 예약 수·환불 크레딧·금액과 접수된 `refundId`, 현재 `refundStatus`를 반환한다. 실제 PG 완료 여부는 내 8회권 목록·상세의 환불 진행 상태로 다시 확인한다.

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
| `domain/pass/PassPurchase.java` | `useCredit(usedAt)`에서 사용 가능 검증과 차감 수행 |
| `application/.../pass/DefaultPassCreditService.java` | 회원 소유권 확인, 크레딧 차감과 USE 원장 저장 |
| `application/.../booking/DefaultBookingNoShowService.java` | 결석 처리 |
| `application/.../pass/DefaultPassRefundService.java` | 정산 환불 + 미래 예약 자동 취소 |
| `application/.../booking/DefaultBookingCancelService.java` | D-1 이후 취소 시 크레딧 소멸 유지 분기 |
| `adapter-out-persistence/.../booking/BookingRepository.java` | `findFutureBookedPassBookings()` JPQL 쿼리 |
| `adapter-in-web/.../admin/AdminBookingController.java` | `POST /admin/bookings/{id}/no-show` |
| `bootstrap/src/main/resources/db/migration/V5__add_pass_booking_link.sql` | bookings.pass_purchase_id FK, pass_purchases.total_price |

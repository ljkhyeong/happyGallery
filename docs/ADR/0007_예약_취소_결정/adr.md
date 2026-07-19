# ADR-0007: 예약 취소 구현 결정

- **날짜**: 2026-02-25
- **상태**: Accepted
- **관련**: §5.4 예약 취소/노쇼/완료

---

## 컨텍스트

§5.4 구현 과정에서 내린 설계 결정들을 기록한다.

---

## 결정 1: 취소는 D-1 관계없이 항상 허용, 환불 여부만 분기

**결정**: BOOKED 상태이면 언제든 취소 가능. D-1 00:00 전/후에 따라 Refund 생성 여부만 다르다.

**대안**: D-1 이후 취소 자체를 차단하고 422 반환.

**이유**: 예약자 관점에서 취소 자체는 항상 가능해야 한다. "환불 불가"와 "취소 불가"는 별개다. 운영 정책도 강제 취소 차단보다 자연 취소 허용 + 환불 불가 기록이 더 합리적.

**위험**: 취소 후 환불이 안 된다는 사실을 응답(`refundable: false`)으로만 전달 — 고객 UI에서 반드시 명확하게 안내해야 함.

---

## 결정 2: refunds 테이블은 V2 기존 테이블 재사용

**결정**: V2에서 이미 `refunds` 테이블이 `booking_id` 컬럼과 함께 정의되어 있어 별도 마이그레이션 없이 사용.

**이유**: 스키마가 이미 준비되어 있음. 추가 마이그레이션 불필요.

**당시 위험**: `Refund` 엔티티가 `order_id`, 원결제 식별자, `fail_reason` 등 미사용 컬럼을 포함했다. 후속 PG 연동에서 Toss `paymentKey`와 환불 `transactionKey`로 의미를 구체화했다.

---

## 결정 3: 취소 시 슬롯 booked_count 반납 (reschedule과 동일 패턴)

**결정**: `slotRepository.findByIdWithLock()` → `slot.decrementBookedCount()` → 저장.

**이유**: reschedule(§5.3)과 동일한 비관적 락 패턴. 동시 취소 시 count 언더플로우 방지.

---

## 결정 4: CancelResult 내부 record로 (booking, refundable, refund) 반환

**결정**: `BookingCancelUseCase.CancelResult` 내부 record로 취소된 예약, 정책상 보상 가능 여부, 생성된 환불 요청을 함께 반환한다. `refund`는 예약금 PG 환불을 요청했을 때만 존재하며, 8회권 크레딧 복구 또는 환불 불가 취소에서는 `null`이다.

**대안**: boolean을 필드로 Booking에 추가, 또는 별도 DTO.

**이유**: 서비스 레이어에서 컨트롤러로 로컬 취소 결과와 비동기 PG 환불 요청 상태를 함께 전달해야 하는데, Booking 엔티티에 환불 실행 상태를 복제하지 않는 가장 단순한 방법이다.

---

## 결정 5: API — DELETE /bookings/{bookingId} + X-Access-Token

**결정**: `DELETE` 메서드를 쓰고, 비회원 `access_token`은 `X-Access-Token` 헤더로 받는다.

**대안**: `PATCH /bookings/{bookingId}/cancel` with body.

**이유**: `DELETE`가 취소(자원 소멸)의 의미에 더 부합한다. 조회·변경·취소가 같은 헤더 인증 계약을 사용한다. 응답 본문에 `refundable`, `refundAmount`, nullable `refund`를 담아 로컬 취소 결과와 PG 환불 요청 접수를 구분한다.

---

## 결정 6: D-1 환불 불가 시 Refund 레코드 미생성

**결정**: 환불 불가(D-1 이후)이면 `refunds` 테이블에 아무것도 기록하지 않는다.

**대안**: REJECTED 또는 NOT_REFUNDABLE 상태로 기록.

**이유**: `RefundStatus`는 실제 PG 환불 요청의 실행 상태를 나타낸다. "환불 불가"는 실행 상태가 아니라 정책 결과이므로 `booking_history`의 CANCELED 이력과 응답의 `refundable: false`로 충분히 추적 가능하다.

**위험**: 감사 목적으로 "환불 불가 사유"를 별도 기록해야 할 수 있음 — 운영 요건 확인 후 추가 고려.

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

**위험**: `Refund` 엔티티가 `order_id`, `pg_ref`, `fail_reason` 등 현재 사용하지 않는 컬럼을 포함 — PG 연동 구현 시 동일 엔티티를 확장 사용 예정.

---

## 결정 3: 취소 시 슬롯 booked_count 반납 (reschedule과 동일 패턴)

**결정**: `slotRepository.findByIdWithLock()` → `slot.decrementBookedCount()` → 저장.

**이유**: reschedule(§5.3)과 동일한 비관적 락 패턴. 동시 취소 시 count 언더플로우 방지.

---

## 결정 4: CancelResult 내부 record로 (booking, refundable) 반환

**결정**: `BookingCancelService.CancelResult` 내부 record를 사용해 두 값을 함께 반환.

**대안**: boolean을 필드로 Booking에 추가, 또는 별도 DTO.

**이유**: 서비스 레이어에서 컨트롤러로 취소 결과와 환불 여부를 함께 전달해야 하는데, Booking 엔티티를 오염시키지 않는 가장 단순한 방법.

---

## 결정 5: API — DELETE /bookings/{bookingId}?token=xxx

**결정**: `DELETE` 메서드를 쓰고, `access_token`은 쿼리 파라미터로 받는다.

**대안**: `PATCH /bookings/{bookingId}/cancel` with body.

**이유**: `DELETE`가 취소(자원 소멸)의 의미에 더 부합한다. 토큰을 본문이 아닌 쿼리 파라미터로 받아 GET 조회 패턴과 일관성을 유지했다. 응답 본문에 취소 결과를 담아 `200`을 반환한 이유는 환불 가능 여부를 함께 전달하기 위해서다.

---

## 결정 6: D-1 환불 불가 시 Refund 레코드 미생성

**결정**: 환불 불가(D-1 이후)이면 `refunds` 테이블에 아무것도 기록하지 않는다.

**대안**: REJECTED 또는 NOT_REFUNDABLE 상태로 기록.

**이유**: `RefundStatus`에는 REQUESTED / SUCCEEDED / FAILED만 존재. "환불 불가"는 이력 기록 대상이 아닌 정책 결과 — `booking_history`의 CANCELED 이력과 응답의 `refundable: false`로 충분히 추적 가능.

**위험**: 감사 목적으로 "환불 불가 사유"를 별도 기록해야 할 수 있음 — 운영 요건 확인 후 추가 고려.

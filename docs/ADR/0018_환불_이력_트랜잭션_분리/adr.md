# ADR-0018: 환불 이력 저장 트랜잭션 분리(REQUIRES_NEW)

**날짜**: 2026-03-06  
**상태**: Accepted

---

## 컨텍스트

주문 거절/자동환불/예약 취소 흐름에서 환불 호출이 실패하면 `refunds`에 `FAILED` 이력을 남겨
운영자가 재시도할 수 있어야 한다.

기존에는 환불 이력 저장이 부모 유스케이스 트랜잭션(`REQUIRED`) 안에서 처리되어,
후속 단계에서 부모 트랜잭션이 롤백되면 실패 이력도 함께 사라질 가능성이 있었다.

---

## 결정 사항

- 환불 실행/이력 저장 로직을 `RefundExecutionService`로 분리한다.
- `RefundExecutionService` 메서드는 `@Transactional(propagation = REQUIRES_NEW)`를 사용한다.
  - `processOrderRefund(orderId, amount)`
  - `processBookingRefund(bookingId, amount)`
- 부모 트랜잭션은 환불 처리 결과를 사용하되, 환불 이력 커밋 여부는 독립적으로 보장한다.

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 부모 트랜잭션 롤백과 무관하게 환불 실패 이력이 보존된다 |
| 장점 | 운영자 재시도 API(`/api/v1/admin/refunds/failed`, `/retry`) 신뢰성이 올라간다 |
| 단점 | 분리 트랜잭션으로 인해 부모 상태와 환불 이력의 시점 불일치가 발생할 수 있다 |
| 대응 | 이력 보존을 우선하고, 운영 절차/재시도 흐름에서 불일치를 흡수한다 |

---

## 구현 반영

- `app/booking/RefundExecutionService` 추가
- `OrderApprovalService#processRefund` → `RefundExecutionService` 위임
- `BookingCancelService` 예약금 환불 경로 → `RefundExecutionService` 위임
- 롤백 상황 보장 테스트 추가:
  - `RefundExecutionServiceUseCaseIT`

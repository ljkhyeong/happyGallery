# Spring Event 기반 알림 분리

> 2026-07-04 업데이트: 일반 알림은 [ADR-0032](../../ADR/0032_알림_Outbox_전달_보장/adr.md)에 따라
> 도메인 트랜잭션 안에서 `notification_outbox`에 저장하고, 커밋 이후 dispatcher가 발송하는 구조로 전환했다.
> 아래 내용은 전환 전 검토 기록으로만 유지한다.

> 비즈니스 로직과 알림 발송의 강한 결합을 끊고, 도메인 이벤트 발행 → 리스너 소비 구조로 전환하는 패턴을 검토한다.

---

## 배경

당시 주문·예약·환불 서비스가 `NotificationService`를 직접 호출하고 있었다.
코드가 비대해지고, "주문 로직" 안에 결제·재고 차감·알림톡 발송이 섞여 유지보수가 어렵다.

이벤트 퍼블리셔를 사용하면 주문 로직은 `OrderPaidEvent`만 발행하고, 알림 리스너가 이를 감지해 발송하는 구조로 관심사를 분리할 수 있다.

---

## 핵심 개념

| 항목 | 설명 |
|------|------|
| `ApplicationEventPublisher` | 비즈니스 서비스가 도메인 이벤트 객체를 발행한다 |
| `@EventListener` | 이벤트를 감지해 동기 처리한다 |
| `@TransactionalEventListener` | 메인 트랜잭션 커밋 후에만 리스너를 실행한다 (기본: `AFTER_COMMIT`) |
| 관심사 분리 | 발행자는 누가 소비하는지 모르고, 리스너는 여러 개 붙을 수 있다 |

---

## 당시 결합 현황 (10개 직접 호출)

| 호출 위치 | 이벤트 타입 | 비고 |
|-----------|------------|------|
| `OrderService.createPaidOrder()` | ORDER_PAID | 주문 생성 후 |
| `OrderRefundSupport.refundOrder()` | ORDER_REFUNDED | PG 환불 성공 후 |
| `BookingSlotSupport.saveAndComplete()` | BOOKING_CONFIRMED | 예약 생성 후 |
| `DefaultBookingRescheduleService` | BOOKING_RESCHEDULED | 변경 후 |
| `DefaultBookingCancelService` | BOOKING_CANCELED, DEPOSIT_REFUNDED | 취소 후 |
| 배치 3종 (픽업/예약/이용권) | REMINDER 계열 | 스케줄 기반, 이벤트 대상 아님 |

---

## 당시 검토한 전환 구조

```
[OrderService]
  └─ publisher.publishEvent(new OrderPaidEvent(orderId, guestId))

[NotificationEventListener]
  @TransactionalEventListener(phase = AFTER_COMMIT)
  void onOrderPaid(OrderPaidEvent e) {
      notificationService.notifyByGuestId(e.guestId(), ORDER_PAID);
  }
```

---

## 당시 도입을 보류한 이유

1. **비동기 분리가 이미 적용됨** — 당시에는 `NotificationService`가 `@Async("notificationExecutor")`로 실행되어, 알림이 메인 트랜잭션을 블로킹하지 않고 실패도 전파하지 않았다.
2. **리스너가 알림 하나뿐** — 이벤트 패턴은 하나의 이벤트에 여러 리스너가 반응할 때 효과가 극대화되는데, 현재는 알림 한 가지만 존재한다.
3. **배치 호출은 이벤트 대상이 아님** — 리마인더 배치는 트랜잭션 커밋 후 도메인 이벤트와 성격이 다르다.

---

## 도입 적기

- 주문 완료 후 "알림 + 포인트 적립 + 통계 집계" 같은 **2개 이상의 부수효과**가 필요해질 때.
- 서비스 간 의존 방향을 역전시켜 모듈 분리를 추진할 때.
- 외부 메시지 큐(Kafka, SQS) 도입 전 단계적 전환이 필요할 때.

---

## 주의사항

- `@TransactionalEventListener`는 기본적으로 트랜잭션 커밋 후 동기 실행 — 비동기가 필요하면 `@Async`를 함께 붙여야 한다.
- 이벤트 발행 후 리스너 실패 시 재시도 전략을 별도로 설계해야 한다.
- 배치 리마인더(4건)는 이벤트 전환 대상에서 제외한다.

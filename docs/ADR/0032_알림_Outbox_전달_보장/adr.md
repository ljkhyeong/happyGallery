# ADR-0032: 알림 Outbox 전달 보장

**날짜**: 2026-07-04
**상태**: Accepted

---

## 컨텍스트

기존 알림은 `NotificationRequestedEvent`를 발행하고
`@TransactionalEventListener(AFTER_COMMIT) + @Async` 리스너가 즉시 `NotificationService`를 호출했다.
이 구조는 커밋 전 발송은 막지만, 도메인 트랜잭션 커밋 직후 프로세스가 종료되면 메모리 이벤트가 사라져
알림 요청 자체가 유실될 수 있다.

---

## 결정 사항

- 일반 알림 요청은 도메인 트랜잭션 안에서 `notification_outbox`에 먼저 저장한다.
- `NotificationEventListener`의 동기 `@EventListener`는 외부 채널을 호출하지 않고 outbox 저장과
  `NotificationOutboxEnqueuedEvent` 발행만 담당한다.
- 내부 outbox 저장 이벤트는 `@TransactionalEventListener(AFTER_COMMIT)`에서 받아
  `NotificationOutboxDispatcher`를 호출한다. 트랜잭션 밖 요청은 `fallbackExecution=true`로 즉시 dispatch한다.
- `NotificationOutboxDispatcher`는 `notificationExecutor`에서 pending outbox를 발송한다.
- `NotificationOutboxDispatcher#dispatchPending`은 활성 트랜잭션이 있으면 예외를 던져 외부 알림 호출 중 부모 트랜잭션 커넥션을 점유하지 않게 한다.
- outbox 예약과 결과 갱신은 짧은 `REQUIRES_NEW` 트랜잭션으로 처리하고, 발송 요청 조회는 `readOnly` 기본 전파를 사용한다.
- `NotificationOutboxScheduler`는 주기적으로 pending/stale processing outbox를 다시 dispatch해 즉시 dispatch 실패와 재시작 상황을 복구한다.
- 실제 채널 fallback 순서와 발송 결과 이력은 기존 `NotificationService`와 `notification_log`가 유지한다.
- 전화번호 평문은 outbox에 저장하지 않는다. outbox는 `guest_id` 또는 `user_id`만 저장하고, 발송 시점에 기존 조회/복호화 경로를 사용한다.
- aggregate가 명확한 일회성 알림은 `recipient + eventType + aggregateType + aggregateId` idempotency key로 outbox 중복 저장을 막는다.
- 같은 예약에서 여러 번 발생할 수 있는 `BOOKING_RESCHEDULED`는 요청 단위 idempotency key를 사용해 기존 반복 발송 의미를 보존한다.

---

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | 도메인 상태 변경과 알림 요청 저장이 같은 트랜잭션에 묶인다 |
| 장점 | 커밋 이후 프로세스 종료/재시작 상황에서도 pending outbox를 재처리할 수 있다 |
| 장점 | 기존 카카오 우선, SMS fallback, `notification_log` 성공/실패 이력 정책을 유지한다 |
| 단점 | 알림 발송은 최종적으로 비동기 처리되므로 사용자 응답 시점에는 아직 발송 전일 수 있다 |
| 대응 | 즉시 dispatch after-commit과 scheduled polling을 함께 둬 지연을 줄이고 복구 경로를 유지한다 |

---

## 구현 반영

- `notification_outbox` 테이블과 dispatch/unique 인덱스 추가
- `NotificationOutbox`, `NotificationOutboxStatus`, `NotificationRecipientType` 추가
- `NotificationOutboxService`, `NotificationOutboxDispatcher`, `NotificationOutboxTransactionService`, `NotificationOutboxScheduler` 추가
- `NotificationEventListener`를 outbox 저장용 동기 리스너와 내부 이벤트의 `AFTER_COMMIT` dispatch 리스너로 분리
- `NotificationRequestedEvent`에 aggregate/idempotency key 추가
- outbox 트랜잭션 보장 테스트:
  - `NotificationOutboxUseCaseIT`

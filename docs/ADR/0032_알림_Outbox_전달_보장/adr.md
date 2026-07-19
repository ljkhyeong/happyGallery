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
  `NotificationOutboxDispatcher`를 호출한다. 트랜잭션 밖 요청은 `fallbackExecution=true`로 즉시 비동기 dispatch를 요청한다.
- `NotificationEventListener#dispatchAfterCommit`은 `@Async("notificationExecutor")`로 비동기 실행하고,
  별도 빈인 `NotificationOutboxDispatcher`를 호출한다.
- `NotificationOutboxDispatcher#dispatchPending`은 `Propagation.NEVER`로 외부 알림 호출이 활성 트랜잭션 안에서
  실행되지 않도록 선언적으로 강제한다.
- outbox 예약과 결과 갱신은 짧은 `REQUIRES_NEW` 트랜잭션으로 처리하고, 발송 요청 조회는 `readOnly` 기본 전파를 사용한다.
- `NotificationOutboxScheduler`는 주기적으로 pending/stale processing outbox를 다시 dispatch해 즉시 dispatch 실패와 재시작 상황을 복구한다.
- 실제 채널 fallback 순서와 발송 결과 이력은 기존 `NotificationService`와 `notification_log`가 유지한다.
- 카카오·SMS sender의 `false` 결과도 채널별 CircuitBreaker 실패로 집계한다. timeout 보조 executor는 제한 큐와 즉시 거절 정책을 사용하며, 큐 포화는 해당 채널 실패로 반환해 다음 채널 fallback을 계속한다.
- `notificationTimeoutExecutor`의 queued/remaining task와 `happygallery.notification.executor.rejected`를 수집하고, 대기열 80% 지속 또는 거절 발생 시 운영 알림을 보낸다.
- 전화번호 평문은 outbox에 저장하지 않는다. outbox는 `guest_id` 또는 `user_id`만 저장하고, 발송 시점에 기존 조회/복호화 경로를 사용한다.
- outbox의 `recipient_type`과 수신자 ID는 DB CHECK로 일치시키고, outbox와 발송 로그 모두 회원·비회원 수신자 중 정확히 하나만 갖도록 강제한다.
- aggregate가 명확한 일회성 알림은 `recipient + eventType + aggregateType + aggregateId` idempotency key로 outbox 중복 저장을 막는다.
- 같은 예약에서 여러 번 발생할 수 있는 `BOOKING_RESCHEDULED`는 요청 단위 idempotency key를 사용해 기존 반복 발송 의미를 보존한다.
- 자동 재시도를 모두 소진한 outbox는 `FAILED`로 종결하고 `happygallery.notification.outbox.failed` 카운터를 올린다.
- 관리자는 실패 outbox를 최대 100건씩 조회하고, 원래 행을 `PENDING`으로 다시 열 수 있다. 새 outbox나 새 멱등키를 만들지 않으므로 동일 이벤트가 별도 요청으로 중복 발송되는 것을 막는다.
- 주문 결제와 8회권 구매도 주문/구매 트랜잭션 안에서 각각 `ORDER_PAID`, `PASS_PURCHASED` outbox를 저장한다.

---

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | 도메인 상태 변경과 알림 요청 저장이 같은 트랜잭션에 묶인다 |
| 장점 | 커밋 이후 프로세스 종료/재시작 상황에서도 pending outbox를 재처리할 수 있다 |
| 장점 | 기존 카카오 우선, SMS fallback, `notification_log` 성공/실패 이력 정책을 유지한다 |
| 장점 | 외부 채널 장애와 로컬 대기열 포화가 무제한 적재로 번지지 않고 서킷·메트릭에 반영된다 |
| 단점 | 알림 발송은 최종적으로 비동기 처리되므로 사용자 응답 시점에는 아직 발송 전일 수 있다 |
| 대응 | 즉시 dispatch after-commit과 scheduled polling을 함께 둬 지연을 줄이고 복구 경로를 유지한다 |

---

## 구현 반영

- `notification_outbox` 테이블과 dispatch/unique 인덱스 추가
- `NotificationOutbox`, `NotificationOutboxStatus`, `NotificationRecipientType` 추가
- `NotificationOutboxService`, `NotificationOutboxDispatcher`, `NotificationOutboxTransactionService`, `NotificationOutboxScheduler` 추가
- `NotificationEventListener`를 outbox 저장용 동기 리스너와 내부 이벤트의 `AFTER_COMMIT` 비동기 dispatch 리스너로 분리
- `NotificationOutboxDispatcher#dispatchPending`에 `Propagation.NEVER` 적용
- `NotificationResilienceConfig`에 boolean 실패 집계, 제한 큐 timeout executor와 대기열·거절 메트릭 적용
- `NotificationRequestedEvent`에 aggregate/idempotency key 추가
- 최종 실패 메트릭, 관리자 실패 목록·재처리 API와 화면 추가
- outbox 트랜잭션 보장 테스트:
  - `NotificationOutboxUseCaseIT`

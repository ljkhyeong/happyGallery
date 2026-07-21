# ADR-0032: 알림 Outbox 전달 보장

**날짜**: 2026-07-04
**최종 갱신**: 2026-07-21
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
- outbox 저장은 사전 `exists` 조회에 의존하지 않고 멱등키 UNIQUE 제약에 직접 insert한다. 같은 키의 동시 요청 중
  한 건만 저장되며, 동기 listener가 원 업무 트랜잭션에 참여하므로 저장 실패는 원 업무와 함께 롤백된다.
- `NotificationEventListener`의 동기 `@EventListener`는 외부 채널을 호출하지 않고 outbox 저장과
  `NotificationOutboxEnqueuedEvent` 발행만 담당한다.
- 내부 outbox 저장 이벤트는 `@TransactionalEventListener(AFTER_COMMIT)`에서 받아
  `NotificationOutboxDispatcher`를 호출한다. 트랜잭션 밖 요청은 `fallbackExecution=true`로 즉시 비동기 dispatch를 요청한다.
- `NotificationEventListener#dispatchAfterCommit`은 `@Async("notificationExecutor")`로 비동기 실행하고,
  별도 빈인 `NotificationOutboxDispatcher`를 호출한다.
- `NotificationOutboxDispatcher#dispatchPending`은 `Propagation.NEVER`로 외부 알림 호출이 활성 트랜잭션 안에서
  실행되지 않도록 선언적으로 강제한다.
- outbox 예약과 결과 갱신은 짧은 `REQUIRES_NEW` 트랜잭션으로 처리하고, 발송 요청 조회는 `readOnly` 기본 전파를 사용한다.
- outbox를 선점할 때마다 새 `processing_token`을 발급하고 요청 조회와 결과 반영에 함께 전달한다. `@Version`과 토큰이 오래된 실행의 성공·실패 결과가 최신 재선점 상태를 덮지 않게 한다.
- `PROCESSING`이 1분 넘게 유지된 outbox는 오래된 실행으로 보고 재선점한다. 정상 알림 transport 상한 5초보다 충분히 길고 NHN 멱등키의 10분 중복 차단 창보다 짧아, 중단된 실행의 복구를 중복 차단 창 안에서 시작한다.
- `NotificationOutboxScheduler`는 주기적으로 pending/오래된 processing outbox를 다시 dispatch해 즉시 dispatch 실패와 재시작 상황을 복구한다.
- 실제 채널 fallback 순서와 발송 결과 이력은 기존 `NotificationService`와 `notification_log`가 유지한다. 운영 1순위는 NHN Cloud Alimtalk v2.2, 2순위는 NHN Cloud SMS다.
- Alimtalk·SMS sender는 성공·영구 거절·일시 실패를 구분한다. 408·425·429·5xx·timeout·통신 예외와 큐 포화 같은
  일시 실패만 채널별 CircuitBreaker 장애율에 반영하고, 영구 거절도 기존처럼 다음 채널 fallback으로 넘긴다.
  모든 채널이 영구 거절하면 outbox를 즉시 `FAILED`로 종결하고, 하나라도 일시 실패면 최대 5회 백오프 재시도한다.
  인증 SMS도 같은 typed 결과를 resilience 경계까지 유지해 영구 거절이 공유 SMS CircuitBreaker를 열지 않게 한다.
  timeout 보조 executor는 제한 큐와 즉시 거절 정책을 사용한다.
- NHN transport의 acquire·connect·response timeout 합을 바깥 TimeLimiter보다 작게 두어, TimeLimiter가 끝난 뒤에도 blocking HTTP 호출이 남아 다음 채널과 겹치는 기본 설정을 허용하지 않는다.
- `notificationTimeoutExecutor`의 queued/remaining task와 `happygallery.notification.executor.rejected`를 수집하고, 대기열 80% 지속 또는 거절 발생 시 운영 알림을 보낸다.
- 전화번호 평문은 outbox에 저장하지 않는다. outbox는 `guest_id` 또는 `user_id`만 저장하고, 발송 시점에 기존 조회/복호화 경로를 사용한다.
- outbox의 `recipient_type`과 수신자 ID는 DB CHECK로 일치시키고, outbox와 발송 로그 모두 회원·비회원 수신자 중 정확히 하나만 갖도록 강제한다.
- aggregate가 명확한 일회성 알림은 `recipient + eventType + aggregateType + aggregateId` idempotency key로 outbox 중복 저장을 막는다.
- 관리자가 1:1 문의에 처음 답변하면 `DefaultInquiryService#replyAndGet`이 `NotificationRequestedEvent.ForUser`를 `INQUIRY_ANSWERED`, aggregate type `INQUIRY`, aggregate id `inquiryId`로 발행한다. 멱등키는 `USER:{userId}:INQUIRY_ANSWERED:INQUIRY:{inquiryId}`다.
- 관리자가 상품 Q&A에 처음 답변하면 `DefaultProductQnaService#replyAndGet`이 `NotificationRequestedEvent.ForUser`를 `PRODUCT_QNA_ANSWERED`, aggregate type `PRODUCT_QNA`, aggregate id `qnaId`로 발행한다. 멱등키는 `USER:{userId}:PRODUCT_QNA_ANSWERED:PRODUCT_QNA:{qnaId}`다.
- 문의·Q&A 답변 상태 저장과 이벤트 발행은 각 `replyAndGet`의 트랜잭션 안에서 일어난다. 동기 `NotificationEventListener#handle`과 기본 전파의 `NotificationOutboxService#enqueue`가 같은 트랜잭션에 참여하므로 outbox insert 실패 시 답변도 함께 롤백된다.
- 실제 외부 발송은 `NotificationOutboxEnqueuedEvent`의 `AFTER_COMMIT` 리스너가 요청한다. 답변 트랜잭션이 롤백되면 after-commit dispatch는 실행되지 않으며, 커밋 성공 뒤에는 즉시 dispatch와 scheduler 복구 경로를 함께 사용한다.
- 문의와 Q&A는 도메인에서 답변을 한 번만 등록할 수 있고, outbox UNIQUE 멱등키도 같은 게시글의 중복 알림 생성을 한 번 더 차단한다. 각 이벤트는 Alimtalk 템플릿과 SMS 문구에 모두 매핑한다.
- 픽업 마감 임박 알림은 수신자 단위 최근 발송 이력으로 후보를 제거하지 않고 `ORDER + orderId` 단위로 멱등 처리한다.
  같은 고객의 여러 픽업 주문은 각각 알리고 동일 주문의 반복 배치만 하나의 outbox로 합친다.
- 8회권 만료 임박 알림은 사용자 단위가 아니라 `PASS_PURCHASE + passId` 단위로 멱등 처리한다. 같은 회원의 여러 8회권은 각각 알리고, 같은 구매 건의 수동·정기 배치 중복 실행은 하나의 outbox로 합친다.
- 픽업·8회권 알림 배치는 원자적 outbox insert 결과를 성공 건수로 사용한다. 중복 키로 저장되지 않은 요청을 성공으로 집계하지 않는다.
- 같은 outbox idempotency key를 NHN Alimtalk의 `X-NC-API-IDEMPOTENCY-KEY`로 전달해 공식 10분 중복 요청 차단을 사용한다. 처리 토큰 재발급과 무관하게 외부 멱등키는 유지한다.
- 같은 예약에서 여러 번 발생할 수 있는 `BOOKING_RESCHEDULED`는 요청 단위 idempotency key를 사용해 기존 반복 발송 의미를 보존한다.
- 자동 재시도를 모두 소진한 outbox는 `FAILED`로 종결하고 `happygallery.notification.outbox.failed` 카운터를 올린다.
- `PENDING`·`PROCESSING`·`FAILED`별 backlog 건수와 처리 기준 시각을 DB에서 주기적으로 집계한다. `PROCESSING`은 현재 선점의 `locked_at`이 2분 넘은 경우, `PENDING`은 `next_attempt_at`이 1분 넘게 지난 경우를 정체로 보고, `FAILED`는 최종 실패가 확정된 `processed_at`부터 경과 시간을 계산한다. 미래 재시도 예정 시각의 age는 0이므로 최대 5회 지수 백오프 중인 정상 대기는 경보를 울리지 않는다.
- 관리자는 실패 outbox를 최대 100건씩 조회하고, 원래 행을 `PENDING`으로 다시 열 수 있다. 새 outbox나 새 멱등키를 만들지 않으므로 동일 이벤트가 별도 요청으로 중복 발송되는 것을 막는다.
- 주문 결제와 8회권 구매도 주문/구매 트랜잭션 안에서 각각 `ORDER_PAID`, `PASS_PURCHASED` outbox를 저장한다.
- 예약금·주문·8회권의 PG 환불 성공 처리도 `DEPOSIT_REFUNDED`, `ORDER_REFUNDED`, `PASS_REFUNDED` outbox 저장과 같은 `REQUIRES_NEW` 트랜잭션에 묶는다. 동기 outbox listener 예외를 삼키지 않으므로 저장 실패 시 로컬 환불 성공 반영이 롤백되고, 기존 PG 멱등키 복구가 다시 상태를 확정한다.
- 외부 채널 성공 뒤 `notification_log` 저장만 실패하면 성공한 메시지를 다시 보내지 않는다. outbox를 `SENT`로 끝내되 `last_error=AUDIT_LOG_PERSISTENCE_FAILED`와 `happygallery.notification.log.persistence_failed` 메트릭을 남긴다. 외부 성공 전 감사 로그 실패는 기존 전송 실패와 함께 outbox 재시도 대상으로 둔다.

### 전달 보장 한계

- 일반 알림은 **at-least-once** 전달이다. `processing_token`과 `@Version`은 오래된 로컬 결과가 최신 상태를 덮는 것을 막지만 외부 제공자의 실제 발송을 되돌리지는 못한다.
- NHN Alimtalk의 멱등키 중복 차단은 10분 동안만 유효하고, 공식 문서에는 중복 요청만 식별하는 전용 `resultCode`가 없다. 임의의 오류 문자열이나 결과 코드를 중복 성공으로 해석하지 않는다.
- 외부 발송 성공 직후 프로세스가 종료되어 로컬 `SENT` 저장이 유실되거나, 실행 중단이 10분을 넘겨 NHN 중복 차단 창이 끝난 뒤 재시도되면 Alimtalk이 중복 발송될 수 있다.
- NHN SMS API에는 이 outbox가 사용할 수 있는 동등한 멱등 계약이 없다. Alimtalk의 transport 결과가 불명인데 SMS fallback 또는 outbox 재시도가 이어지면 채널 간 중복이 발생할 수 있다.
- transport 결과 불명에서 즉시 SMS fallback만 선택적으로 막으려면 제공자 상태 조회나 영속적인 채널별 `UNKNOWN` 상태가 필요하다. 일시·영구 실패 분류만으로는 외부의 실제 성공 여부를 확정할 수 없어 근거 없는 분기를 추가하지 않는다.
- `notification_log`는 회원 알림함 목록·읽지 않은 건수·읽음 처리의 원본이고, terminal outbox의 UNIQUE 멱등키는 같은 도메인 이벤트의 재생성을 막는다. 현재 API는 알림함 조회 보존 기간을 제한하지 않고 outbox와 로그 사이에 삭제 가능성을 증명할 직접 참조도 없으므로 자동 삭제하지 않는다. 추후 조회 기간, 감사 보존 기간과 멱등키 tombstone 전략을 함께 정한 뒤 보존 배치를 도입한다.

---

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | 도메인 상태 변경과 알림 요청 저장이 같은 트랜잭션에 묶인다 |
| 장점 | 커밋 이후 프로세스 종료/재시작 상황에서도 pending outbox를 재처리할 수 있다 |
| 장점 | 기존 Alimtalk 우선, SMS fallback, `notification_log` 성공/실패 이력 정책을 유지한다 |
| 장점 | 외부 채널 장애와 로컬 대기열 포화가 무제한 적재로 번지지 않고 서킷·메트릭에 반영된다 |
| 장점 | 이미 성공한 외부 메시지는 감사 로그 장애 때문에 다시 보내지 않고 별도 경고로 추적한다 |
| 단점 | 알림 발송은 최종적으로 비동기 처리되므로 사용자 응답 시점에는 아직 발송 전일 수 있다 |
| 단점 | 제공자 멱등 TTL과 SMS 비멱등 계약 때문에 장애 구간에는 중복 알림이 발생할 수 있다 |
| 대응 | 즉시 dispatch after-commit과 scheduled polling을 함께 둬 지연을 줄이고 복구 경로를 유지한다 |

---

## 구현 반영

- `notification_outbox` 테이블과 dispatch/unique 인덱스 추가
- `NotificationOutbox`, `NotificationOutboxStatus`, `NotificationRecipientType` 추가
- `NotificationOutboxService`, `NotificationOutboxDispatcher`, `NotificationOutboxTransactionService`, `NotificationOutboxScheduler` 추가
- `NotificationEventListener`를 outbox 저장용 동기 리스너와 내부 이벤트의 `AFTER_COMMIT` 비동기 dispatch 리스너로 분리
- `NotificationOutboxDispatcher#dispatchPending`에 `Propagation.NEVER` 적용
- `NotificationResilienceConfig`에 일시 실패 집계, 제한 큐 timeout executor와 대기열·거절 메트릭 적용
- outbox 멱등키 UNIQUE 제약을 이용하는 원자적 insert-if-absent 어댑터 적용
- `NotificationRequestedEvent`에 aggregate/idempotency key 추가
- 최종 실패 메트릭, 관리자 실패 목록·재처리 API와 화면 추가
- outbox 상태별 backlog 건수·처리 기준 경과 시간과 DB 스냅샷 갱신 상태 메트릭, Prometheus 정체 경보와 Grafana 패널 추가
- 환불 성공 알림 outbox의 원자적 저장과 `notification_log` 저장 실패 메트릭·재발송 방지 처리 추가
- `notification_outbox.processing_token`, `version`과 NHN Alimtalk 멱등 헤더 적용
- 1:1 문의·상품 Q&A 답변 트랜잭션에 `INQUIRY_ANSWERED`·`PRODUCT_QNA_ANSWERED` 회원 outbox 요청 적용
- outbox 트랜잭션 보장 테스트:
  - `NotificationOutboxUseCaseIT`

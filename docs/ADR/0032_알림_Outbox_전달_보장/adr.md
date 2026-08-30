# ADR-0032: 알림 Outbox 전달 보장

**날짜**: 2026-07-04
**최종 갱신**: 2026-08-08
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
- 부모 트랜잭션 커밋 뒤 `notificationExecutor`가 포화 또는 종료 중이라 실행 신호를 거절해도
  이미 저장된 outbox는 유지한다. 거절은 executor 태그 counter와 경고로 노출하되 원 HTTP 요청에 예외를
  전파하지 않고, 다음 `NotificationOutboxScheduler` 주기가 같은 pending 행을 회수한다.
- `NotificationOutboxDispatcher#dispatchPending`은 `Propagation.NEVER`로 외부 알림 호출이 활성 트랜잭션 안에서
  실행되지 않도록 선언적으로 강제한다.
- outbox 예약, 발송 준비, 결과 갱신은 각각 짧은 `REQUIRES_NEW` 트랜잭션으로 처리한다.
  한 실행은 최대 50건을 처리하되 한 건씩 선점하고 결과를 확정한 뒤 다음 건을 선점한다. 아직 외부 호출 순서를 기다리는 행까지
  미리 `PROCESSING`으로 바꾸지 않아 같은 실행 안의 대기 시간 때문에 lease가 만료되는 일을 막는다.
- outbox를 선점할 때마다 새 `processing_token`을 발급하고 요청 조회와 결과 반영에 함께 전달한다. `@Version`과 토큰이 오래된 실행의 성공·실패 결과가 최신 재선점 상태를 덮지 않게 한다.
- 최초 선점은 `PENDING`, 재선점은 `locked_at < staleBefore`인 lease 만료 `PROCESSING`에만 적용한다. 도메인은 두 전이를 별도 메서드로 구분해 아직 활성인 실행권과 종료 상태인 `SENT`·`FAILED`를 다시 `PROCESSING`으로 되돌리는 호출을 `409 CONFLICT`로 거절한다. 최종 실패 재처리는 관리자 경로가 먼저 같은 행을 `PENDING`으로 연 뒤 새 실행권을 발급한다.
- `PROCESSING`이 1분 넘게 유지된 outbox는 오래된 실행으로 보고 재선점한다. 정상 알림 transport 상한 5초보다 충분히 길고 NHN 멱등키의 10분 중복 차단 창보다 짧아, 중단된 실행의 복구를 중복 차단 창 안에서 시작한다.
- `NotificationOutboxScheduler`는 주기적으로 pending/오래된 processing outbox를 다시 dispatch해 즉시 dispatch 실패와 재시작 상황을 복구한다.
- 실제 채널 fallback 순서와 발송 결과 이력은 기존 `NotificationService`와 `notification_log`가 유지한다. 운영 1순위는 NHN Cloud Alimtalk v2.2, 2순위는 NHN Cloud SMS다.
- NHN이 발송 요청을 접수한 응답은 전달 성공과 구분한다. `requestId`·`recipientSeq`를 outbox와 요청 감사 로그에 저장하고 `DELIVERY_PENDING`으로 전이한 뒤, 별도 scheduler가 단건 결과 API를 조회한다. 결과 조회 lease는 `DELIVERY_CHECKING`과 새 processing token으로 보호하고 1분 넘게 멈춘 실행만 재선점한다.
- Alimtalk `COMPLETED` 또는 SMS `msgStatus=3`·`resultCode=1000`을 확인한 뒤에만 감사 로그를 `SUCCESS`, outbox를 `SENT`로 확정한다. Alimtalk `FAILED/CANCEL`을 확인하면 기존 KAKAO 감사 로그를 실패로 끝내고 그때 SMS를 요청한다. SMS도 같은 최종 결과 확인을 거치며 최종 실패는 outbox를 `FAILED`로 종결한다.
- 예약 D-1·당일, 8회권 만료 임박, 픽업 마감 리마인드는 outbox 선점 뒤 `prepareDelivery(outboxId, processingToken)`의
  짧은 `REQUIRES_NEW` 트랜잭션에서 outbox 행을 `FOR UPDATE`로 잠근다. 현재 token을 확인한 뒤 aggregate별 SQL 한 번으로
  상태·시간 구간·현재 회원/비회원 소유자를 같은 DB snapshot에서 읽는다. 부적격이면 `OBSOLETE`로 종결하고, 적격이면 outbox의
  수신자와 `locked_at` lease를 갱신한 delivery request를 반환한다. dispatcher는 이 트랜잭션이 커밋된 뒤 그 request로 외부 채널을 호출한다.
- 이 aggregate 조회 snapshot과 `prepareDelivery` 커밋을 발송 결정의 선형화 지점으로 정의한다. 외부 호출 동안 aggregate나 outbox의
  DB 잠금을 유지하지 않는다. 따라서 커밋 직후 발생한 취소·상태 변경까지 절대 차단하는 프로토콜은 아니지만, 준비 시점 이전에 완료된
  claim·취소·소진·픽업 완료는 상태와 현재 수신자 중 일부만 섞이지 않고 한 snapshot으로 반영된다.
- 발송 로그와 `SENT` outbox 기반 알림함은 `prepareDelivery`가 갱신한 현재 소유자에게 귀속한다. 비회원 예약·주문이 발송 대기 중
  회원에게 귀속됐으면 과거 Guest 전화번호나 Guest 알림함으로 보내지 않는다.
- 현재 적격성 판단에는 `event_type + aggregate_type + aggregate_id`와 주입된 `Clock`을 사용한다.
  예약이 취소·완료·변경됐거나, 8회권이 만료·소진·환불됐거나, 주문이 픽업 완료·만료 상태가 된 경우처럼
  현재 의미가 사라진 요청은 sender와 `notification_log`를 호출하지 않고 `OBSOLETE`로 종결한다.
- `REMINDER_D1`은 발송 시점 기준 내일 `[tomorrowStart, dayAfterStart)`의 `BOOKED` 예약에만,
  `REMINDER_SAME_DAY`는 07:00 이후 `(now, tomorrowStart)`의 아직 시작하지 않은 오늘 `BOOKED` 예약에만 유효하다.
  즉 D-1의 날짜 시작 경계는 포함(`>=`)하고 당일의 현재 시각 경계는 제외(`>`)한다. 8회권은 `(now, now+7d]`에 만료되고 잔여 횟수가 있을 때,
  픽업은 `PICKUP_READY`이며 마감이 `(now, now+2h]`에 있을 때만 유효하다.
- `OBSOLETE` 전이도 `PROCESSING` 상태와 현재 `processing_token`을 확인하고 `processed_at`을 기록한다.
  관리자는 `FAILED`만 다시 열 수 있으므로, 의미가 사라진 리마인드를 수동 재처리해 뒤늦게 발송할 수 없다.
- 예약 재변경이나 픽업 마감 연장처럼 같은 aggregate가 미래 유효 구간에 다시 들어오면 정기 리마인드 후보 조회는
  `OBSOLETE` 행을 미발송 이력으로 보고 같은 멱등키 행을 잠근 뒤 `PENDING`으로 재활성화한다. 새 outbox를 만들지 않고
  현재 회원·비회원 수신자를 갱신하며, 이 자동 전이는 시간 의존 리마인드에만 허용한다.
- Alimtalk·SMS sender는 성공·영구 거절·일시 실패·전달 결과 불명을 구분한다. 408·425·429·5xx, NHN SMS
  `-9999` 시스템 오류와 `-2021` 발송 큐 저장 실패, DNS·라우팅·TCP 연결·TLS handshake/peer 검증·연결 풀 대기 실패처럼 제공자에 요청을 전달하기 전 확정된 실패는 다음 채널
  fallback 및 최대 5회 백오프 재시도 대상으로 둔다. 요청을 쓴 뒤 응답 대기 timeout처럼 제공자 수락 여부를 알 수
  없는 결과는 즉시 fallback과 자동 재시도를 중단하고,
  기존 outbox `FAILED` 상태에 `DELIVERY_RESULT_UNKNOWN`을 남겨 운영자가 확인한 뒤 재처리하게 한다.
  영구 거절은 서킷 장애율에 넣지 않고 기존처럼 다음 채널 fallback으로 넘긴다.
- Alimtalk, 일반 SMS, 휴대폰 인증 SMS는 각각 별도 제한 큐 executor와 CircuitBreaker를 사용한다. 한 채널의 대기열 포화나
  서킷 개방이 다른 채널의 실행 자원을 소진하지 않으며, 모든 timeout 보조 executor는 즉시 거절 정책을 사용한다.
- NHN transport의 acquire·connect·response timeout 합을 바깥 TimeLimiter보다 작게 두어, TimeLimiter가 끝난 뒤에도 blocking HTTP 호출이 남아 다음 채널과 겹치는 기본 설정을 허용하지 않는다.
- 채널별 executor의 queued/remaining task와
  `happygallery.notification.{alimtalk|sms|phone_verification}.executor.rejected`를 수집하고,
  어느 한 대기열이라도 80%가 지속되거나 거절이 발생하면 채널을 구분해 운영 알림을 보낸다.
- 전화번호 평문은 outbox에 저장하지 않는다. outbox는 `guest_id` 또는 `user_id`만 저장하고, 발송 시점에 기존 조회/복호화 경로를 사용한다.
- outbox의 `recipient_type`과 수신자 ID는 DB CHECK로 일치시키고, outbox와 발송 로그 모두 회원·비회원 수신자 중 정확히 하나만 갖도록 강제한다.
- 문의·Q&A처럼 수신자가 이벤트 정체성에 포함되는 aggregate 알림은 `recipient + eventType + aggregateType + aggregateId` idempotency key를 사용한다.
- 예약·픽업 리마인드처럼 회원 귀속 전후에도 aggregate당 한 번이어야 하는 알림은 수신자와 무관한 `eventType + aggregateType + aggregateId` key를 사용한다. 수신자는 전달 대상 필드에만 남기므로 비회원 기록이 회원에게 귀속돼도 같은 예약·주문을 다시 접수하지 않는다.
- 관리자가 1:1 문의에 처음 답변하면 `DefaultInquiryService#replyAndGet`이 `NotificationRequestedEvent.ForUser`를 `INQUIRY_ANSWERED`, aggregate type `INQUIRY`, aggregate id `inquiryId`로 발행한다. 멱등키는 `USER:{userId}:INQUIRY_ANSWERED:INQUIRY:{inquiryId}`다.
- 관리자가 상품 Q&A에 처음 답변하면 `DefaultProductQnaService#replyAndGet`이 `NotificationRequestedEvent.ForUser`를 `PRODUCT_QNA_ANSWERED`, aggregate type `PRODUCT_QNA`, aggregate id `qnaId`로 발행한다. 멱등키는 `USER:{userId}:PRODUCT_QNA_ANSWERED:PRODUCT_QNA:{qnaId}`다.
- 문의·Q&A 답변 상태 저장과 이벤트 발행은 각 `replyAndGet`의 트랜잭션 안에서 일어난다. 동기 `NotificationEventListener#handle`과 기본 전파의 `NotificationOutboxService#enqueue`가 같은 트랜잭션에 참여하므로 outbox insert 실패 시 답변도 함께 롤백된다.
- 실제 외부 발송은 `NotificationOutboxEnqueuedEvent`의 `AFTER_COMMIT` 리스너가 요청한다. 답변 트랜잭션이 롤백되면 after-commit dispatch는 실행되지 않으며, 커밋 성공 뒤에는 즉시 dispatch와 scheduler 복구 경로를 함께 사용한다.
- 문의와 Q&A는 도메인에서 답변을 한 번만 등록할 수 있고, outbox UNIQUE 멱등키도 같은 게시글의 중복 알림 생성을 한 번 더 차단한다. 각 이벤트는 Alimtalk 템플릿과 SMS 문구에 모두 매핑한다.
- 후기 요청은 배송·픽업 완료 주문에 작성 가능한 품목이 하나 이상 있거나 완료 예약에 후기가 없을 때 `REVIEW_REQUEST`를 주문·예약 aggregate당 한 번 저장한다. 완료된 비회원 이력이 회원에게 귀속될 때도 같은 멱등키를 사용한다.
- 숨김·재공개는 `REVIEW_HIDDEN`, `REVIEW_REPUBLISHED`를 `REVIEW_MODERATION_ACTION + actionId`로 저장한다. 발송 직전에 해당 action이 후기의 최신 action이고 현재 상태와 일치하는지 확인해 빠르게 반복된 중간 전환을 `OBSOLETE`로 종료한다.
- 공방이 후기에 처음 공식 답글을 저장하면 `REVIEW_OWNER_REPLIED`를 `REVIEW + reviewId`로 한 번 저장한다. 답글 수정·삭제·재작성은 새 알림을 만들지 않는다.
- 후기 알림도 발송 직전에 현재 회원 소유권·후기 작성 가능성·상태·답글 존재를 한 DB snapshot에서 재검증하고, 정상 삭제 후 재작성된 활성 후기가 있는 원천도 후기 요청을 보내지 않는다.
- 외부 Alimtalk 운영 전 `HG_REVIEW_REQUEST`, `HG_REVIEW_HIDDEN`, `HG_REVIEW_REOPENED`, `HG_REVIEW_REPLY` 템플릿을 공급자에 사전 등록해야 한다. 템플릿이 없으면 기존 채널 fallback 정책에 따라 SMS로 전환한다.
- 픽업 마감 임박 알림은 수신자 단위 최근 발송 이력으로 후보를 제거하지 않고 `ORDER + orderId` 단위로 멱등 처리한다.
  같은 고객의 여러 픽업 주문은 각각 알리고 동일 주문의 반복 배치만 하나의 outbox로 합친다.
- 8회권 만료 임박 알림은 사용자 단위가 아니라 `PASS_PURCHASE + passId` 단위로 멱등 처리한다. 같은 회원의 여러 8회권은 각각 알리고, 같은 구매 건의 수동·정기 배치 중복 실행은 하나의 outbox로 합친다.
- 예약·8회권·픽업 정기 리마인드 후보 조회는 `event_type + aggregate_type + aggregate_id`가 같은 outbox가 이미 존재하는 대상을 제외한다. 멱등키 문자열 형식에 의존하지 않으므로, 특히 예약·픽업은 구형 `USER:`/`GUEST:` 키와 현재 `AGGREGATE:` 키를 동일한 접수 이력으로 인식해 배포 전후 중복 알림을 막는다. 예약·8회권 후보는 ID 키셋으로 100건씩 끝까지 순회하며, 현재 멱등키의 UNIQUE 제약은 조회와 insert 사이 동시 실행을 막는 최종 방어선으로 유지한다.
- 픽업·8회권 알림 배치는 원자적 outbox insert 결과를 성공 건수로 사용한다. 중복 키로 저장되지 않은 요청을 성공으로 집계하지 않는다.
- 같은 outbox idempotency key를 NHN Alimtalk의 `X-NC-API-IDEMPOTENCY-KEY`로 전달해 공식 10분 중복 요청 차단을 사용한다. 처리 토큰 재발급과 무관하게 외부 멱등키는 유지한다.
- NHN SMS v3 계약에는 클라이언트 멱등키 필드나 헤더가 없다. 일반 SMS 요청에는 outbox 멱등키에서 만든
  불투명 상관관계 ID를 제공자 `userId`에 전달해 운영 대사에 사용하되, 이를 중복 차단 계약으로 해석하지 않는다.
  인증 SMS는 outbox를 사용하지 않으므로 이 상관관계 ID를 보내지 않는다.
- 같은 예약에서 여러 번 발생할 수 있는 `BOOKING_RESCHEDULED`는 요청 단위 idempotency key를 사용해 기존 반복 발송 의미를 보존한다.
- 자동 재시도를 모두 소진한 outbox는 `FAILED`로 종결하고 `happygallery.notification.outbox.failed` 카운터를 올린다.
- `PENDING`·`PROCESSING`·`DELIVERY_PENDING`·`DELIVERY_CHECKING`·`FAILED`별 backlog 건수와 처리 기준 시각을 DB에서 주기적으로 집계한다. 실행 상태는 현재 선점의 `locked_at`, 대기 상태는 `next_attempt_at`, `FAILED`는 `processed_at`부터 경과 시간을 계산한다. 실행 상태가 2분, 예정 시각이 지난 대기 상태가 1분을 넘으면 경보를 울린다. 미래 예정 시각의 age는 0으로 유지한다.
- 관리자는 실패 outbox를 최대 100건씩 조회하고, 원래 행을 `PENDING`으로 다시 열 수 있다. 새 outbox나 새 멱등키를 만들지 않으므로 동일 이벤트가 별도 요청으로 중복 발송되는 것을 막는다.
- 주문 결제와 8회권 구매도 주문/구매 트랜잭션 안에서 각각 `ORDER_PAID`, `PASS_PURCHASED` outbox를 저장한다.
- 예약금·주문·8회권의 PG 환불 성공 처리도 `DEPOSIT_REFUNDED`, `ORDER_REFUNDED`, `PASS_REFUNDED` outbox 저장과 같은 `REQUIRES_NEW` 트랜잭션에 묶는다. 동기 outbox listener 예외를 삼키지 않으므로 저장 실패 시 로컬 환불 성공 반영이 롤백되고, 기존 PG 멱등키 복구가 다시 상태를 확정한다.
- 외부 채널 성공 뒤 `notification_log` 저장만 실패하면 성공한 메시지를 다시 보내지 않는다. outbox를 `SENT`로 끝내되 `last_error=AUDIT_LOG_PERSISTENCE_FAILED`와 `happygallery.notification.log.persistence_failed` 메트릭을 남긴다. 전송 결과 불명과 감사 로그 실패가 겹치면 `FAILED + DELIVERY_RESULT_UNKNOWN:AUDIT_LOG_PERSISTENCE_FAILED`로 종결해 재발송하지 않고 두 원인을 함께 보존한다. 외부 성공 전 감사 로그 실패는 기존 전송 실패와 함께 outbox 재시도 대상으로 둔다.

### 전달 보장 한계

- 일반 알림은 **at-least-once** 전달이다. `processing_token`과 `@Version`은 오래된 로컬 결과가 최신 상태를 덮는 것을 막지만 외부 제공자의 실제 발송을 되돌리지는 못한다.
- NHN Alimtalk의 멱등키 중복 차단은 10분 동안만 유효하고, 공식 문서에는 중복 요청만 식별하는 전용 `resultCode`가 없다. 임의의 오류 문자열이나 결과 코드를 중복 성공으로 해석하지 않는다.
- 외부 발송 성공 직후 프로세스가 종료되어 로컬 `SENT` 저장이 유실되거나, 실행 중단이 10분을 넘겨 NHN 중복 차단 창이 끝난 뒤 재시도되면 Alimtalk이 중복 발송될 수 있다.
- NHN SMS API에는 이 outbox가 사용할 수 있는 동등한 멱등 계약이 없다. `userId` 상관관계 값은 조회·대사 보조값일 뿐
  동일 요청의 재발송을 막지 않는다.
- transport 결과 불명은 별도 outbox 상태를 늘리지 않고 `FAILED + DELIVERY_RESULT_UNKNOWN`으로 종결한다.
  자동 중복 가능성은 낮추지만, 실제 미발송이었다면 운영 확인 전까지 전달이 지연되는 가용성 비용을 감수한다.
- `notification_outbox`의 `SENT` 행은 회원 알림함 목록·읽지 않은 건수·읽음 처리의 원본이자 동일 도메인 이벤트의 멱등 기록이다. 알림함의 전달 시각은 도메인 이벤트 발생 시각이 아니라 외부 채널 전달 성공 후 로컬 `SENT`가 확정된 `processed_at`이다. `notification_log`는 카카오톡·SMS fallback을 포함한 채널별 감사 이력으로만 사용하므로 한 outbox에서 로그가 여러 건 생겨도 알림함은 중복되지 않는다.
- 알림함 원본 전환 migration 이전에 이미 `SENT`였던 outbox는 `read_at=processed_at`으로 이관해 과거 알림 전체가 새 미확인 알림으로 보이지 않게 한다.
- 발송 완료 `SENT`, 현재 의미가 사라진 `OBSOLETE`, 자동 재시도를 모두 소진한 최종 `FAILED` outbox는 처리 종료 시각인 `processed_at`부터 180일 보존한다. 채널 감사 로그도 180일 보존하며, 매일 보존 배치가 100건씩 짧게 삭제한다. 이 기간을 알림함 조회, 운영자 최종 실패 재처리와 동일 이벤트 멱등 보장 기간으로 공개한다. 아직 재시도 가능한 `PENDING`, 실행 중인 `PROCESSING`, 제공자 결과 대기·조회 상태인 `DELIVERY_PENDING`·`DELIVERY_CHECKING` outbox는 생성 시각이 오래돼도 자동 삭제하지 않는다.

---

## 결과

| 항목 | 내용 |
|------|------|
| 장점 | 도메인 상태 변경과 알림 요청 저장이 같은 트랜잭션에 묶인다 |
| 장점 | 커밋 이후 프로세스 종료/재시작 상황에서도 pending outbox를 재처리할 수 있다 |
| 장점 | 기존 Alimtalk 우선, SMS fallback, `notification_log` 성공/실패 이력 정책을 유지한다 |
| 장점 | 큐 지연·관리자 재시도 전에 도메인 상태가 바뀌어도 취소·소진·픽업 완료된 리마인드를 발송하지 않는다 |
| 장점 | 외부 채널 장애와 로컬 대기열 포화가 무제한 적재로 번지지 않고 서킷·메트릭에 반영된다 |
| 장점 | 이미 성공한 외부 메시지는 감사 로그 장애 때문에 다시 보내지 않고 별도 경고로 추적한다 |
| 단점 | 알림 발송은 최종적으로 비동기 처리되므로 사용자 응답 시점에는 아직 발송 전일 수 있다 |
| 단점 | 제공자 멱등 TTL과 SMS 비멱등 계약 때문에 장애 구간에는 중복 알림이 발생할 수 있다 |
| 대응 | 즉시 dispatch after-commit과 scheduled polling을 함께 둬 지연을 줄이고 복구 경로를 유지한다 |

---

## 구현 반영

- `notification_outbox` 테이블과 dispatch/unique 인덱스 추가
- `NotificationOutbox`, `NotificationOutboxStatus`, `NotificationRecipientType` 추가
- 제공자 요청 식별자, `DELIVERY_PENDING`·`DELIVERY_CHECKING`, `NotificationDeliveryResultReconciler`와 단건 결과 조회 어댑터 추가
- 시간 의존 리마인드의 발송 직전 적격성 조회와 `OBSOLETE` terminal 상태 추가
- `prepareDelivery`에서 token·현재 적격성·현재 수신자·lease를 한 트랜잭션으로 확정하고 커밋 뒤 외부 발송
- 미래 유효 구간에 다시 들어온 `OBSOLETE` 리마인드의 동일 행·멱등키 자동 재활성화 추가
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

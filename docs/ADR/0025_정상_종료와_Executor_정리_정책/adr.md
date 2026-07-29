# ADR-0025: 정상 종료와 실행기 정리 정책

**날짜**: 2026-03-19  
**최종 갱신**: 2026-07-29
**상태**: Accepted

---

## 컨텍스트

현재 애플리케이션은 다음과 같은 비동기/백그라운드 실행 경로를 가진다.

- Spring `ThreadPoolTaskExecutor` 기반 알림·환불 비동기 실행
- 결제·알림 외부 호출의 Resilience4j `TimeLimiter`를 위한 별도 제한 `ThreadPoolTaskExecutor`
- 웹 요청 종료와 별개로 drain이 필요한 Spring bean lifecycle

운영 중 애플리케이션이 갑자기 종료되면 다음 문제가 생길 수 있다.

- 이미 큐에 들어간 알림 작업이 중간에 유실된다.
- 종료 직전 돌고 있던 비동기 작업의 로그/MDC 문맥이 끊긴다.
- 외부 PG timeout 감시용 executor thread가 정리되지 않은 채 종료 순서가 불명확해진다.

반대로 종료 대기를 너무 길게 잡으면 deploy/rollback 시간이 늘어난다.
끝나지 않는 작업 때문에 인스턴스 교체가 늦어질 수도 있다.

따라서 "무조건 오래 기다린다"가 아니라, 애플리케이션과 executor마다 종료 대기 정책을 따로 문서화해야 한다.

---

## 결정 사항

### 1. 서버 종료 정책은 Spring graceful shutdown을 기본값으로 유지한다

- `server.shutdown=graceful`을 사용한다.
- `spring.lifecycle.timeout-per-shutdown-phase=30s`를 사용한다.
- 의미:
  - 새 요청은 더 이상 받지 않는다.
  - Spring lifecycle phase 안에 있는 bean들은 최대 30초까지 정상 종료를 시도한다.
  - 30초 안에 종료되지 않으면 다음 종료 단계로 넘어간다.

이 값은 짧은 비동기 후처리 작업은 마무리할 기회를 주되, 배포 파이프라인이 너무 오래 멈추지 않게 하려는 운영 기준이다.

### 2. 업무 후처리용 `ThreadPoolTaskExecutor`는 queued/running task drain을 우선한다

`notificationExecutor`와 `refundExecutor`는 다음 정책을 따른다.

- `spring.task.execution`의 core/max/queue와 종료 정책을 공통 원본으로 사용한다.
- 각 이름의 실행기는 Boot `ThreadPoolTaskExecutorBuilder`로 만들고 thread prefix만 분리한다.
- 종료 시 queued/running task 완료를 최대 30초 기다린다.
- 공용 `TaskDecorator`로 MDC와 Sentry scope를 함께 복사한다.

의미:

- shutdown 신호 후 executor는 새 작업을 받지 않는다.
- 이미 실행 중이거나 큐에 들어간 작업은 가능한 한 완료를 시도한다.
- 최대 대기 시간은 30초이며, 상위 Spring shutdown phase와 같은 값으로 맞춘다.
- 요청 스레드의 `MDC` 값을 비동기 스레드로 전달한다.

이 executor에 이 정책을 적용한 이유:

- 알림과 환불 실행은 요청 본문 성공 이후 후속 작업이며, 종료 시점 유실을 줄여야 한다.
- 현재 알림 작업은 장시간 CPU 작업이 아니라 비교적 짧은 외부 호출/후처리다.
- 30초 drain이 현실적인 기본값이다.
- 알림 작업 로그도 원 요청의 `requestId`와 함께 이어져야 장애 추적이 가능하다.
- 환불 요청 자체는 DB에 먼저 커밋되므로 drain 실패나 큐 거절이 발생해도 복구 배치가 다시 실행한다.

### 3. 모든 비동기 외부 경계는 `TaskDecorator`로 추적 문맥을 전파한다

`notificationExecutor`, `refundExecutor`와 `BoundedExecutorFactory`가 만든 외부 호출 실행기는
같은 합성 `TaskDecorator`를 사용한다.
이 decorator 빈은 `defaultCandidate=false`로 등록해 Boot 스케줄러가 자동 채택하지 않게 한다.
반복 `@Scheduled` 작업은 요청 문맥을 전달할 대상이 아니며, 주기 실행 future에 한 번 fork된
Sentry scope가 다음 실행에도 재사용되는 것을 피한다.

1. 제출 시점의 `MDC.getCopyOfContextMap()`으로 문맥을 복사한다.
2. Sentry scope를 fork해 비동기 작업의 오류 추적 문맥을 이어간다.
3. 작업 실행 직전에 MDC를 주입하고 종료 후 worker thread의 이전 문맥을 복원한다.

이 정책의 목적:

- request thread에서 생성한 `requestId`를 비동기 알림 로그에서도 그대로 유지한다.
- thread pool 재사용 환경에서 이전 작업의 MDC 값이 다음 작업에 누수되지 않도록 막는다.
- 종료 직전 drain되는 작업도 동일한 request trace로 추적 가능하게 유지한다.

새 executor를 추가할 때는 실행 방식이 Spring `@Async`인지 `CompletableFuture`인지와 무관하게
이 decorator를 주입한다. 스레드 생성 코드마다 MDC 복사 로직을 다시 작성하지 않는다.

### 4. 커밋 후 실행 신호 거절은 원 요청 실패로 전파하지 않는다

알림 outbox와 환불 요청은 부모 트랜잭션에서 먼저 DB에 저장된다. `AFTER_COMMIT` 이후
`notificationExecutor` 또는 `refundExecutor`에 제출하는 작업은 원본 업무가 아니라 빠른 실행을 요청하는 신호다.

- 큐 포화 또는 종료 중 거절은 `happygallery.async.executor.rejected{executor}`에 기록하고 WARN 로그를 남긴다.
- 거절 handler는 예외를 호출자에게 다시 던지거나 호출자 스레드에서 작업을 실행하지 않는다.
- 알림 outbox scheduler와 환불 복구 batch가 저장된 상태를 다시 조회해 후속 처리를 이어간다.
- Actuator가 이름별 queued/remaining task를 노출하고 Prometheus는 80% 지속을
  `DurableSignalExecutorQueueHigh`, 최근 거절을 `DurableSignalExecutorRejected`로 알린다.

`CallerRunsPolicy`는 커밋을 끝낸 요청 스레드가 외부 후속 처리를 대신 수행하게 해 응답 격리와 타임아웃을
깨므로 사용하지 않는다. 반대로 PG·알림 transport용 timeout executor는 호출 시작 전 포화를
상위 결과로 분류해야 하므로 아래의 `AbortPolicy`를 그대로 사용한다.

### 5. 외부 호출 timeout용 `ThreadPoolTaskExecutor`는 제한 큐와 빠른 정리를 사용한다

`BoundedExecutorFactory`는 PG와 알림 채널 timeout executor에 공통으로 필요한 생성 정책을 소유한다.

- Boot `ThreadPoolTaskExecutorBuilder` 기반 Spring bean lifecycle
- core/max pool 크기가 같은 고정 thread pool
- 고정 크기 `ArrayBlockingQueue`
- daemon platform thread
- 거절 횟수 counter와 Micrometer executor monitor 등록
- 큐 포화 시 호출 스레드에서 실행하지 않는 `AbortPolicy`

`PaymentResilienceConfig`와 `NotificationResilienceConfig`는 pool·queue 크기, thread/metric 이름과
각 executor의 Spring 빈 수명주기를 결정한다. 알림톡·일반 SMS·휴대폰 인증 SMS는 서로 다른 executor로
격리한다. 큐 포화는 호출 시작 전 실패이므로 즉시 재시도 가능한 결제 실패 또는 알림 채널 실패로 반환한다.

공통 제한 `ThreadPoolTaskExecutor`는 Spring이 빈 종료를 호출하면 다음 순서로 정리한다.

1. `executor.shutdown()`
2. 최대 2초 `awaitTermination`
3. 미종료 시 `shutdownNow()`
4. `InterruptedException` 발생 시 interrupt 복구 후 `shutdownNow()`

timeout executor는 업무 후처리용 `notificationExecutor`와 다르게, 진행 중 작업을 끝까지 보존하는 것보다 보호용 thread를 빨리 정리하는 쪽이 우선이다.

이유:

- 이 executor는 독립 비즈니스 큐가 아니라 `TimeLimiter` 보조 실행기에 가깝다.
- 외부 PG 호출은 이미 timeout/circuit-breaker 보호를 받는다.
- 종료 시점에 이 thread pool을 오래 붙잡아둘 운영 가치가 상대적으로 낮다.
- 결제 큐 거절은 DB 복구 대상 상태로 남고, 알림 채널 큐 거절은 `false`로 처리되어 다음 채널 fallback과 outbox 복구 정책을 따른다.

### 6. 종료 대기 시간은 계층별 역할에 따라 다르게 둔다

| 대상 | 정책 | 대기 시간 |
|------|------|------|
| Spring application lifecycle | graceful shutdown | 30초 |
| `notificationExecutor` | task completion 대기 | 30초 |
| `refundExecutor` | task completion 대기, 미실행 건은 DB 복구 | 30초 |
| Spring `taskScheduler` | 새 실행 거절·주기 작업 즉시 취소 | 대기 없음 |
| PG timeout executor | 제한 큐, 빠른 정리 후 강제 종료 허용 | 2초 |
| 알림 채널 timeout executor | 채널별 제한 큐, 빠른 정리 후 강제 종료 허용 | 2초 |

모든 executor에 같은 종료 정책을 쓰지 않는다.
종료 시 무엇을 보호해야 하는지에 따라 대기 시간을 다르게 둔다.
스케줄러 작업은 DB에서 대상을 다시 조회하는 주기성 복구 작업이므로 종료 시 drain하지 않는다.
다음 기동 또는 다음 주기가 미처리 상태를 다시 처리하며, Spring Session의 만료 정리 같은 주기 작업이
Redis 연결 종료 뒤 남아 shutdown을 지연하지 않게 한다.

---

## 구현 반영

- `bootstrap/src/main/resources/application.yml`
  - `server.shutdown: graceful`
  - `spring.lifecycle.timeout-per-shutdown-phase: 30s`
  - `spring.task.execution`의 pool·30초 drain과 `spring.task.scheduling`의 pool·즉시 종료 정책
- `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/AsyncConfig.java`
  - Boot builder로 알림·환불 실행기 구성
  - 합성 `TaskDecorator`로 MDC·Sentry scope 전파
  - 내구성 신호 거절 계측과 비전파 handler 적용
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/ResilientPaymentProvider.java`
  - 주입받은 보호 자원으로 PG 호출 실행과 결과 표준화
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/PaymentResilienceConfig.java`
  - 결제 executor 설정과 빈 수명주기 구성
  - CircuitBreaker, TimeLimiter, executor와 제공자 빈 조립
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/notification/NotificationResilienceConfig.java`
  - 채널별 알림 executor 설정과 빈 조립
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/resilience/BoundedExecutorFactory.java`
  - Boot builder 기반 timeout executor의 Spring 수명주기 구성
  - 제한 큐·daemon thread·즉시 거절·2초 종료·큐/거절 메트릭과 추적 문맥 전파 공용화

---

## 결과

### 장점

- 종료 시점 동작이 설정/코드/문서 기준으로 일치한다.
- 알림 작업은 불필요한 유실을 줄이고, 보호용 executor는 빠르게 정리할 수 있다.
- 커밋 후 신호 큐 포화가 이미 성공한 요청을 5xx로 바꾸지 않고 복구 경로와 경보로 이어진다.
- deploy/rollback 시 종료 대기 상한이 명확하다.

### 단점

- 30초를 넘는 알림 작업은 정상 종료를 보장하지 못한다.
- PG timeout executor는 종료 시점 일부 작업을 포기할 수 있다.
- 향후 비동기 작업 종류가 늘어나면 executor별 정책을 다시 분리해야 한다.

---

## 운영 메모

- 긴 작업을 `notificationExecutor`에 추가할 경우 현재 30초 drain 정책과 맞는지 먼저 검토한다.
- 비동기 실행기는 공용 TaskDecorator를 사용해 MDC와 Sentry scope를 함께 전달한다.
- 종료 보장이 중요한 신규 비동기 작업은 별도 executor와 별도 ADR 대상으로 다룬다.
- Kubernetes나 systemd 종료 유예 시간을 사용할 경우, 애플리케이션의 30초 graceful shutdown보다 짧지 않게 맞춘다.

---

## 참고 문서

- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0030_타임아웃_계층과_ingress_keep_alive_기준선/adr.md`
- `bootstrap/src/main/resources/application.yml`
- `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/AsyncConfig.java`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/PaymentResilienceConfig.java`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/ResilientPaymentProvider.java`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/resilience/BoundedExecutorFactory.java`

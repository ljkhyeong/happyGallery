# ADR-0025: 정상 종료와 실행기 정리 정책

**날짜**: 2026-03-19  
**상태**: Accepted

---

## 컨텍스트

현재 애플리케이션은 다음과 같은 비동기/백그라운드 실행 경로를 가진다.

- Spring `ThreadPoolTaskExecutor` 기반 알림·환불 비동기 실행
- 결제·알림 외부 호출의 Resilience4j `TimeLimiter`를 위한 별도 `ExecutorService`
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

- `setWaitForTasksToCompleteOnShutdown(true)`
- `setAwaitTerminationSeconds(30)`
- `setTaskDecorator(...)`로 MDC 문맥을 복사한다

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

### 3. 알림 비동기 작업은 `TaskDecorator`로 MDC를 전파한다

`notificationExecutor`와 `refundExecutor`는 `TaskDecorator`에서 다음 순서를 따른다.

1. 제출 시점의 `MDC.getCopyOfContextMap()`으로 문맥을 복사한다.
2. 작업 실행 직전에 `MDC.setContextMap(ctx)`로 비동기 스레드에 주입한다.
3. 작업 종료 후 `finally`에서 `MDC.clear()`로 스레드 로컬 문맥을 비운다.

이 정책의 목적:

- request thread에서 생성한 `requestId`를 비동기 알림 로그에서도 그대로 유지한다.
- thread pool 재사용 환경에서 이전 작업의 MDC 값이 다음 작업에 누수되지 않도록 막는다.
- 종료 직전 drain되는 작업도 동일한 request trace로 추적 가능하게 유지한다.

주의 사항:

- 현재 MDC 전파는 `notificationExecutor`와 `refundExecutor`에 적용한다.
- 다른 executor를 추가할 때 request 추적이 필요하면 같은 수준의 decorator 정책을 별도로 적용해야 한다.

### 4. 외부 호출 timeout용 `ExecutorService`는 제한 큐와 빠른 정리를 사용한다

`PaymentResilienceConfig`와 `NotificationResilienceConfig`는 각각 PG와 알림 채널 timeout executor를 Spring 빈으로 생성한다.
두 실행기는 고정 크기 `ArrayBlockingQueue`와 `AbortPolicy`를 사용해 대기 작업 수에 상한을 두고,
큐 포화는 호출 스레드에서 실행하지 않고 즉시 재시도 가능한 실패 또는 채널 실패로 반환한다.

결제의 `PaymentTimeoutExecutor.close()`는 빈 종료 시 다음 순서로 실행기를 정리한다.

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

### 5. 종료 대기 시간은 계층별 역할에 따라 다르게 둔다

| 대상 | 정책 | 대기 시간 |
|------|------|------|
| Spring application lifecycle | graceful shutdown | 30초 |
| `notificationExecutor` | task completion 대기 | 30초 |
| `refundExecutor` | task completion 대기, 미실행 건은 DB 복구 | 30초 |
| PG timeout executor | 빠른 정리 후 강제 종료 허용 | 2초 |
| 알림 채널 timeout executor | 제한 큐, Spring 종료 시 신규 작업 거절 | Spring bean 종료 단계 |

모든 executor에 같은 종료 정책을 쓰지 않는다.
종료 시 무엇을 보호해야 하는지에 따라 대기 시간을 다르게 둔다.

---

## 구현 반영

- `bootstrap/src/main/resources/application.yml`
  - `server.shutdown: graceful`
  - `spring.lifecycle.timeout-per-shutdown-phase: 30s`
- `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/AsyncConfig.java`
  - `notificationExecutor`에 shutdown drain 설정 적용
  - `refundExecutor`에 shutdown drain·MDC 전파 설정 적용
  - `TaskDecorator`로 MDC 복사/주입/정리 적용
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/ResilientPaymentProvider.java`
  - 주입받은 보호 자원으로 PG 호출 실행과 결과 표준화
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/PaymentResilienceConfig.java`
  - 제한 큐와 즉시 거절 정책을 적용한 executor 빈 생성
  - CircuitBreaker, TimeLimiter, executor와 제공자 빈 조립
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/PaymentTimeoutExecutor.java`
  - Spring 빈 종료 시 2초 대기 후 강제 종료하는 수명주기 구현
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/notification/NotificationResilienceConfig.java`
  - 알림 채널 timeout executor에 제한 큐·즉시 거절·큐/거절 메트릭 적용

---

## 결과

### 장점

- 종료 시점 동작이 설정/코드/문서 기준으로 일치한다.
- 알림 작업은 불필요한 유실을 줄이고, 보호용 executor는 빠르게 정리할 수 있다.
- deploy/rollback 시 종료 대기 상한이 명확하다.

### 단점

- 30초를 넘는 알림 작업은 정상 종료를 보장하지 못한다.
- PG timeout executor는 종료 시점 일부 작업을 포기할 수 있다.
- 향후 비동기 작업 종류가 늘어나면 executor별 정책을 다시 분리해야 한다.

---

## 운영 메모

- 긴 작업을 `notificationExecutor`에 추가할 경우 현재 30초 drain 정책과 맞는지 먼저 검토한다.
- 비동기 로그에 request 추적이 필요하면 executor 생성 시 MDC 전파 여부를 먼저 검토한다.
- 종료 보장이 중요한 신규 비동기 작업은 별도 executor와 별도 ADR 대상으로 다룬다.
- Kubernetes나 systemd 종료 유예 시간을 사용할 경우, 애플리케이션의 30초 graceful shutdown보다 짧지 않게 맞춘다.

---

## 참고 문서

- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/ADR/0030_타임아웃_계층과_ingress_keep_alive_기준선/adr.md`
- `bootstrap/src/main/resources/application.yml`
- `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/AsyncConfig.java`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/PaymentResilienceConfig.java`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/PaymentTimeoutExecutor.java`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/ResilientPaymentProvider.java`

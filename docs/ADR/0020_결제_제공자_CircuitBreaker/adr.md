# ADR-0020: 결제 환불 외부 호출 보호를 위한 CircuitBreaker 도입

**날짜**: 2026-03-06  
**최종 갱신**: 2026-07-29
**상태**: Accepted

---

## 컨텍스트

환불 처리 흐름은 외부 PG 호출에 의존한다.
외부 장애/지연 시 애플리케이션 스레드가 장시간 대기하면
요청 처리량 저하와 장애 전파가 발생할 수 있다.

단순 timeout만으로는 반복 실패 상황에서 호출 폭주를 막기 어렵다.

---

## 결정 사항

### 1. `PaymentProvider` 경계에 데코레이터를 적용한다

- `PaymentResilienceConfig`에서 `ResilientPaymentProvider`를 `@Primary` 빈으로 조립한다.
- 실제 호출 구현체는 `paymentProviderDelegate`로 분리해 주입한다.

### 2. `CircuitBreaker + TimeLimiter`를 조합한다

- 기본 타임아웃: 3초
- 실패율 임계치: 50%
- 슬라이딩 윈도우: 20
- 최소 호출 수: 10
- Open 유지 시간: 30초
- Half-open 허용 호출: 3

### 3. 환불 결과는 처리 가능성에 따라 구분한다

- PG 명시적 거절: `FINAL_FAILURE`
- 실행기 큐 거절·서킷 오픈·명시적 일시 오류: `RETRYABLE_FAILURE`
- 타임아웃·통신 단절·응답 해석 불가: `RECONCILIATION_REQUIRED`
- 결과 불명 상태는 실패로 확정하지 않고 최초 멱등키를 재사용해 원응답을 확인한다.
- 서킷 브레이커는 예외뿐 아니라 `RETRYABLE_FAILURE`와 `RECONCILIATION_REQUIRED` 결과도 장애 호출로 집계하고, 최종 업무 거절은 집계하지 않는다.

### 4. 결제 confirm도 같은 보호 경계를 통과한다

- `ResilientPaymentProvider.confirm(paymentKey, orderId, amount, idempotencyKey)`를 추가한다.
- 서킷 오픈, 타임아웃, 예외는 같은 멱등키로 재시도할 수 있도록
  `PaymentConfirmResult.retryableFailure`로 표준화한다.
- 애플리케이션은 최종 거절을 `PAYMENT_FAILED`, 같은 결제 정보로 재확인할 수 있는 일시 실패를
  `PAYMENT_CONFIRM_RETRYABLE`로 매핑하고 도메인 저장을 진행하지 않는다.

### 5. TimeLimiter 실행기는 제한 큐와 즉시 거절 정책을 사용한다

- 기본 스레드 수는 `4`, 대기열 크기는 `20`이며 환경변수로 조정한다.
- 대기열은 고정 크기 `ArrayBlockingQueue`, 거절 정책은 `AbortPolicy`를 사용한다.
- 거절된 호출은 실행되지 않았으므로 즉시 재시도 가능 결과로 반환한다.
- `CallerRunsPolicy`는 요청·환불 스레드에서 PG 호출을 직접 실행해 외부 호출 격리와 `TimeLimiter` 타임아웃 시작 경계를 무너뜨리므로 사용하지 않는다.
- `executor_queued_tasks`, `executor_queue_remaining_tasks`, `happygallery_payment_executor_rejected_total`을 수집하고 대기열 80% 지속 또는 거절 발생 시 알림을 보낸다.

### 6. 보호 자원 구성과 호출 실행 책임을 분리한다

- `PaymentResilienceConfig`가 `CircuitBreaker`, `TimeLimiter`, 제한 큐 executor의 생성과 메트릭 등록, Spring 빈 조립을 담당한다.
- `BoundedExecutorFactory`가 Boot `ThreadPoolTaskExecutorBuilder`로 제한 큐 executor를 만들고 Spring 종료 수명주기, 2초 대기 후 강제 종료, 거절·큐 메트릭을 공통 적용한다.
- `ResilientPaymentProvider`는 주입받은 보호 자원으로 PG 호출을 실행하고 결과를 표준화하는 역할만 담당한다.

### 7. Registry 기반 표준 메트릭으로 결제와 알림 서킷을 함께 관측한다

- 결제 `paymentProvider`, 알림 `alimtalkNotification`·`smsNotification` 서킷을 공용 `CircuitBreakerRegistry`에 등록한다.
- `resilience4j-micrometer`의 tagged metrics를 `/actuator/prometheus`에 노출해 상태, 실패율, 성공·실패·차단 호출 수를 표준 태그로 조회한다.
- 결제 서킷 `OPEN` 또는 최근 2분의 차단 호출은 결제·환불 중단이므로 즉시 critical로 평가한다. 알림 서킷도 같은 조건을 즉시 평가하되 채널 fallback과 outbox 재시도가 있으므로 warning으로 구분한다. 기본 `OPEN` 유지 시간과 같거나 긴 Prometheus `for`는 활성 트래픽에서 `HALF_OPEN` 전환을 놓칠 수 있어 두지 않는다.
- 어노테이션 기반 실행으로 전환하지 않고 기존 typed result의 실패 집계 규칙과 명시적 호출 경계를 유지한다.

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 외부 장애 시 빠른 실패로 내부 자원 보호 |
| 장점 | 외부 지연 시 대기 작업 수와 메모리 사용량에 상한이 생긴다 |
| 장점 | 환불 결과 불명을 최종 실패와 분리해 자동 복구할 수 있다 |
| 장점 | 실 PG 도입 전에도 보호 경계를 코드로 명시 |
| 단점 | Fake 어댑터 환경에서는 체감 효과가 제한적 |
| 대응 | 운영 전환 시 환경변수로 임계치 튜닝 |

---

## 구현 반영

- `adapter-out-external/.../payment/PaymentResilienceConfig`에서 결제 보호 자원과 `@Primary` 제공자 빈 구성
- `adapter-out-external/.../resilience/BoundedExecutorFactory`에서 제한 큐와 Spring executor 종료 수명주기 관리
- `adapter-out-external/.../payment/ResilientPaymentProvider`에서 보호된 PG 호출과 결과 표준화 수행
- `adapter-out-external/.../payment/FakePaymentProvider` 빈 이름 분리 (`paymentProviderDelegate`)
- `PaymentProvider.confirm` 경로도 `CircuitBreaker + TimeLimiter` 보호 적용
- PG timeout executor에 `ArrayBlockingQueue + AbortPolicy` 적용
- 실행기 대기열·거절 메트릭과 Prometheus 알림 추가
- 결제·알림 CircuitBreaker를 공용 Registry에 등록하고 Resilience4j Micrometer 상태·호출 결과 메트릭, Grafana 패널, `OPEN` 경보 추가
- `adapter-out-external/build.gradle`에 Resilience4j 의존성 추가
- `bootstrap/src/main/resources/application.yml`에 `app.external.payment.*` 설정 추가

# Idea 0026: 비동기 테스트 대기에는 Awaitility를 우선 사용

> **ADR 반영 완료** — [ADR-0027](../../ADR/0027_테스트_전략과_최소_테스트_세트_기준선/adr.md) 테스트 전략에 포함되었다. 이 문서는 배경 메모로만 유지한다.

## 배경

알림 발송은 `NotificationService`의 `@Async("notificationExecutor")`를 통해 비동기로 실행된다.
이 때문에 알림 배치/예약 취소/만료 알림 테스트는 도메인 동작 직후 `notification_log` 저장이 끝나지 않을 수 있다.

기존 `NotificationLogTestHelper`는 `Thread.sleep` 기반 polling으로 로그 개수를 기다렸다.
동작 자체는 단순했지만, timeout 시 마지막 조회 결과를 그대로 반환해 실패 원인이 테스트 본문으로 밀려났다.

## 현재 문제

### 1. 수동 polling 구현이 테스트 의도를 가린다

As-Is:
- 보조 유틸 안에서 deadline 계산, 반복 조회, `Thread.sleep`, 인터럽트 처리를 직접 구현한다.
- 실패 메시지는 "왜 기다렸는지"보다 최종 assertion 실패로만 드러난다.

To-Be:
- "비동기 저장이 끝날 때까지 기다린다"는 의도를 테스트 보조 유틸 이름만 봐도 바로 읽을 수 있어야 한다.

### 2. timeout 실패 원인이 덜 선명하다

As-Is:
- 보조 유틸 timeout 후 `repository.findAll()`을 반환한다.
- 테스트 본문에서 `hasSize`가 깨져도 실제 원인이 대기 timeout인지, 잘못된 이벤트 저장인지 한 번 더 해석해야 한다.

To-Be:
- timeout 자체가 명시적 예외로 드러나야 한다.

### 3. 같은 종류의 비동기 검증 기준이 필요하다

As-Is:
- 알림 로그처럼 비동기 부수 효과를 검증하는 테스트는 대기 보조 유틸을 쓰지만, 구현 방식은 라이브러리 도움 없이 직접 들고 간다.

To-Be:
- 비동기 부수 효과 완료 대기는 `Awaitility`를 공통 규칙으로 삼고, timeout 유도 자체가 목적인 테스트는 별도로 둔다.

## 제안

### Awaitility를 공통 보조 유틸 내부에 적용

- `NotificationLogTestHelper.awaitLogCount()`는 `Awaitility.await().untilAsserted(...)`로 교체한다.
- 보조 유틸 호출부는 그대로 두어 테스트 본문 가독성을 유지한다.
- polling 간격과 timeout은 기존 값(25ms / 2s)을 유지해 동작 특성만 보존한다.

예시:

```java
await()
        .atMost(2, SECONDS)
        .pollInterval(25, MILLISECONDS)
        .untilAsserted(() -> assertThat(repository.findAll()).hasSize(expectedCount));
```

## 적용 기준

### Awaitility를 쓰는 경우

- `@Async`, executor, event listener 등으로 인해 저장/발송이 호출 직후 완료되지 않을 수 있는 검증
- "언젠가 이 상태가 되어야 한다"를 기다리는 테스트

### Awaitility를 쓰지 않는 경우

- timeout 자체를 유도하려고 delegate 안에서 일부러 지연시키는 테스트
- 동기 코드 경로라서 결과가 즉시 결정돼야 하는 테스트
- 동시성 제어 자체를 검증하기 위한 `CountDownLatch` 기반 시작점 정렬 테스트

## 현재 판단

알림 로그 저장 완료를 기다리는 테스트는 Awaitility 전환이 이득이다.

이유:

- 보조 유틸 구현이 간결해진다.
- timeout 시 진단 메시지가 더 직접적이다.
- 기존 테스트 본문은 유지하면서 비동기 대기 정책만 공통화할 수 있다.

반면 `CircuitBreakerPaymentProviderTest`의 `Thread.sleep`은 외부 PG timeout을 일부러 만들기 위한 테스트용 지연 장치이므로 Awaitility로 바꾸지 않는다.

# 외부 호출 제한

- 결제 설정은 `PaymentResilienceConfig`, 알림 설정은 `NotificationResilienceConfig`가 소유한다. 공용 풀 생성은 `BoundedExecutorFactory`를 사용한다.
- Boot의 `ThreadPoolTaskExecutorBuilder`로 생명주기를 관리하고 제한된 `ArrayBlockingQueue`, daemon thread, `AbortPolicy`, 2초 강제 종료, 거절 횟수·executor 지표를 유지한다. 호출부는 `Executor`에 의존한다.
- 결제 TimeLimiter는 Toss의 pool 획득·TCP 연결·응답 timeout 합보다 반드시 길어야 한다. 이 관계를 시작 시 검증하고 Java 기본값·YAML·환경 예시·ADR-0030을 함께 맞춘다.
- 이미 DB에 저장한 알림 실행을 깨우는 executor와 외부 호출 timeout executor는 거절 의미가 다르다. 전자는 기록 후 scheduler가 복구하고, 후자는 호출 전 실패로 처리한다. 격리한 외부 호출에 `CallerRunsPolicy`를 사용하지 않는다.

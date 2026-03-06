# 외부 연동 CircuitBreaker 확장 적용 검토

**날짜**: 2026-03-06  
**상태**: In Progress

---

## 배경

외부 시스템(PG/알림) 장애 시 호출 스레드가 장시간 점유되면
애플리케이션 내부 자원까지 고갈되어 장애가 연쇄 전파될 수 있다.

이번 변경에서 결제 환불 경로(`PaymentProvider`)에는
`CircuitBreaker + TimeLimiter(기본 3초)`를 적용했다.

---

## 이번 적용 범위

1. `PaymentProvider.refund()` 외부 호출 경계 보호
2. 서킷 오픈 시 빠른 실패(Fast Fail)로 전환
3. 타임아웃/예외를 `RefundResult.failure(...)`로 표준화

---

## 다음 확장 후보

1. 알림 채널(`NotificationSender`)에도 동일 패턴 적용
2. 필요 시 `Bulkhead` 추가로 외부 연동 동시성 격리
3. 운영 메트릭 연동 (오픈 전환 횟수, 실패율, 타임아웃 건수)

---

## 운영 튜닝 포인트

- `PAYMENT_TIMEOUT_MILLIS` (기본 3000)
- `PAYMENT_CB_FAILURE_RATE_THRESHOLD` (기본 50)
- `PAYMENT_CB_SLIDING_WINDOW_SIZE` (기본 20)
- `PAYMENT_CB_MINIMUM_CALLS` (기본 10)
- `PAYMENT_CB_WAIT_OPEN_SECONDS` (기본 30)
- `PAYMENT_CB_HALF_OPEN_CALLS` (기본 3)

---

## 결론

현재 Fake 어댑터 환경에서도 보호 경계를 선반영해,
실 PG 연동 전환 시 설정 튜닝 중심으로 이행할 수 있도록 준비한다.

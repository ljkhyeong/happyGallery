# PaymentProvider CircuitBreaker 적용 POC

**날짜**: 2026-03-06  
**상태**: 적용 완료 (운영 임계치 튜닝만 후속)

---

## 가설

`PaymentProvider.refund()` 호출에 `CircuitBreaker + TimeLimiter`를 먼저 적용하면,
실 PG 연동 전에도 장애 전파를 줄일 수 있다. 운영 전환 시에는 코드 변경보다 설정 튜닝에 집중할 수 있다.

---

## 검증 방법

적용 범위:
- `PaymentProvider.refund()` 경계
- 환불 처리 흐름의 실패 표준화
- 설정값 환경 변수화

검증 포인트:
1. 외부 호출 실패가 `RefundResult.failure(...)`로 표준화되는지
2. 회로 오픈 시 빠른 실패로 전환되는지
3. 타임아웃/실패율/반개방 호출 수를 환경 변수로 조정 가능한지
4. 기존 주문/예약 환불 흐름의 API 계약을 깨지 않는지

---

## 결과

- 결제 환불 경계에 `CircuitBreaker + TimeLimiter`가 반영됐다.
- 타임아웃/실패율/슬라이딩 윈도우/오픈 대기 시간/반개방 호출 수를 환경 변수로 튜닝할 수 있게 정리됐다.
- 실패는 `RefundResult.failure(...)`로 수렴되어 호출부에서 일관되게 처리된다.
- Toss 결제·환불과 NHN 알림 경계에 같은 보호 구조를 적용했다. 운영 전환 시에는 구조 변경보다 실제 지표에 따른 설정 튜닝에 집중한다.

---

## 결론

이 실험은 유효했다. 결제 환불 호출 보호는 정식 채택 상태로 본다.

구현된 후속:
1. 알림 채널에도 CircuitBreaker, TimeLimiter와 제한된 전용 실행기를 적용했다.
2. 결제·알림 외부 호출은 bounded `ArrayBlockingQueue`와 `AbortPolicy`로 격리한다.
3. CircuitBreaker·대기열·거절 수를 Prometheus/Grafana에서 확인한다.

남은 후속은 실제 운영 트래픽과 외부 사업자 지연을 관찰해 타임아웃·실패율·큐 크기
임계치를 조정하는 일이다.

관련 문서:
- `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
- `docs/Idea/0002_Bulkhead_패턴_검토/idea.md`
- `plan.md`

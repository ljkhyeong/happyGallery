# ADR-0020: 결제 환불 외부 호출 보호를 위한 CircuitBreaker 도입

**날짜**: 2026-03-06  
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

- `CircuitBreakerPaymentProvider`를 `@Primary` 빈으로 등록한다.
- 실제 호출 구현체는 `paymentProviderDelegate`로 분리해 주입한다.

### 2. `CircuitBreaker + TimeLimiter`를 조합한다

- 기본 타임아웃: 3초
- 실패율 임계치: 50%
- 슬라이딩 윈도우: 20
- 최소 호출 수: 10
- Open 유지 시간: 30초
- Half-open 허용 호출: 3

### 3. 실패 응답은 도메인 계약(`RefundResult.failure`)으로 표준화한다

- 서킷 오픈: 즉시 실패 응답
- 타임아웃: 지연 실패 응답
- 기타 예외: 예외 메시지 기반 실패 응답

---

## 결과 (트레이드오프)

| 항목 | 내용 |
|------|------|
| 장점 | 외부 장애 시 빠른 실패로 내부 자원 보호 |
| 장점 | 실 PG 도입 전에도 보호 경계를 코드로 명시 |
| 단점 | Fake 어댑터 환경에서는 체감 효과가 제한적 |
| 대응 | 운영 전환 시 환경변수로 임계치 튜닝 |

---

## 구현 반영

- `infra/payment/CircuitBreakerPaymentProvider` 추가
- `infra/payment/FakePaymentProvider` 빈 이름 분리 (`paymentProviderDelegate`)
- `infra/build.gradle`에 Resilience4j 의존성 추가
- `application.yml`에 `app.external.payment.*` 설정 추가

# ADR-0008 결제 인터페이스 추상화 (§6.1)

- **날짜**: 2026-02-25
- **상태**: 결정됨

---

## 컨텍스트

§5.4 예약 취소에서 `Refund` 엔티티를 REQUESTED 상태로만 저장했다. 실제 PG 연동 없이도 환불 흐름을 테스트할 수 있어야 하고, 실패 시 레코드가 사라지지 않아야 한다.

---

## 결정 1 — `PaymentProvider` 인터페이스는 `infra` 모듈에 둔다

**선택**: `infra/payment/PaymentProvider.java`

**이유**: 모듈 의존성 방향이 `app → infra`이므로, 인터페이스를 `infra`에 두면 순환 없이 `app`이 사용 가능하다. PG는 외부 연동 구현체이므로 `infra` 패키지가 의미상으로도 적절하다.

**탈락 대안**: `common`에 두는 방식 — 결제는 도메인 공통 유틸이 아니어서 부적합.

---

## 결정 2 — `FakePaymentProvider`는 항상 성공

**선택**: `FakePaymentProvider.refund()` → `RefundResult.success("FAKE-REFUND-{UUID}")`

**이유**: 개발 환경에서 기본값을 실패로 설정하면 매 취소마다 수동 재시도가 필요해 개발 흐름을 방해한다. 실패 시나리오는 테스트에서 `@MockitoBean`으로 주입한다.

---

## 결정 3 — 환불 실패 시 `FAILED`로 저장하고 예외를 삼킨다

**선택**: `BookingCancelService`에서 `paymentProvider.refund()` 호출 후 실패/예외 시 `refund.markFailed(reason)`으로 상태 업데이트 후 저장. 예외를 밖으로 전파하지 않는다.

**이유**: 예약 취소 자체(`booking.cancel()`, 슬롯 반납)는 성공해야 한다. PG 환불 실패가 취소 트랜잭션을 롤백시키면 슬롯은 묶인 채 예약자는 취소할 수 없는 상태가 된다. 환불 실패는 FAILED 레코드로 남기고 운영자가 재시도한다.

**위험 포인트**: 예약은 취소됐으나 환불 금액이 실제로 지급되지 않은 상태가 존재. 운영자가 `GET /admin/refunds/failed`를 주기적으로 확인하거나 알림 배치(§배치 구현 시)를 추가해야 한다.

---

## 결정 4 — 운영자 재시도 API: `POST /admin/refunds/{id}/retry`

**선택**: `RefundRetryService.retry(refundId)` + `AdminRefundController`

**이유**: FAILED 레코드를 DB에서 직접 수정하는 것은 감사 추적을 남기지 않는다. API를 통해 재시도하면 성공/실패 상태가 다시 기록되어 추적 가능.

**현재 제약**: `/admin/**` 인증 미적용 (§11 이후 적용 예정, ADR-0007과 동일).

---

## 결과

| 파일 | 역할 |
|------|------|
| `infra/payment/PaymentProvider.java` | 포트 인터페이스 |
| `infra/payment/RefundResult.java` | 환불 결과 VO (success/pgRef/failReason) |
| `infra/payment/FakePaymentProvider.java` | 개발용 항상-성공 어댑터 |
| `domain/booking/Refund.java` | `markSucceeded()` / `markFailed()` 추가 |
| `app/booking/BookingCancelService.java` | Provider 호출, 실패 시 FAILED 저장 |
| `app/booking/RefundRetryService.java` | FAILED 재시도 서비스 |
| `app/web/admin/AdminRefundController.java` | `GET /admin/refunds/failed`, `POST /admin/refunds/{id}/retry` |

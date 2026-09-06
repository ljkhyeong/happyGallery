---
name: happygallery-payment-flows
description: happyGallery의 Toss prepare/confirm·PaymentAttempt·환불 실행·PG 장애·멱등성·보상·timeout을 변경할 때 사용한다. 주문·예약·이용권 환불 정책은 해당 도메인 스킬을 함께 사용한다.
---

# happyGallery 결제·환불 실행

## 결제 규칙

- `application/src/main/java/com/personal/happygallery/application/payment/`, `domain`의 `payment/`, 외부 adapter의 `payment/`를 확인한다. 주요 설계는 ADR-0018·0020·0030·0033에 있다.
- 서버 prepare가 orderId·가격을 계산하고 `PaymentAttempt`에 저장한다. 공개 `PaymentPayload`와 암호화해 저장하는 서버 `PreparedPaymentPayload`를 구분한다. 예약금은 수업료 10%, 이용권은 서버 `PASS_TOTAL_PRICE`를 사용한다.
- 회원 prepare는 회원 행을 잠가 활성 상태를 재확인한다. 비회원 prepare는 facade에서 전화번호 시도 제한을 먼저 검사하고, 코드 소비·약관 동의·attempt 저장을 같은 짧은 트랜잭션에서 처리한다. preparer에서 Redis를 호출하지 않는다.
- confirm은 짧은 claim 트랜잭션 → 트랜잭션 밖 PG 호출 → 승인·fulfillment 저장으로 나눈다. 금액·요청 key 검사는 `startProcessing`과 `requireMatchingConfirmRequest`에서 수행한다.
- 최초 claim과 오래된 작업 회수마다 새 `processing_token`을 저장한다. 현재 token 소유자만 PG 결과·실패·보상을 기록한다. 소유권을 잃은 요청은 PG를 다시 호출하지 않고 최신 상태에 따라 저장 결과·APPROVED 처리·진행 중 응답을 선택한다.
- PG 응답의 paymentKey·orderId가 요청과 다르면 `RECONCILIATION_REQUIRED`로 기록한다. confirm timeout은 같은 orderId로 재시도할 수 있지만 환불 timeout은 결과 미확인이다.
- 같은 CONFIRMED 요청은 actor·금액·paymentKey가 같을 때 저장된 context·도메인 ID·비회원 token을 반환한다. PG·fulfillment를 재실행하지 않는다. 도메인 ID는 필수, 비회원 token은 암호문으로만 저장한다.
- 금액이 있는 결제는 최종 주문·예약·이용권에 확정 paymentKey를 저장한다. 결제 없는 이용권 예약은 null을 유지한다.
- fulfillment 전에 회원 상태를 재확인한다. PG 승인 후 생성이 실패하거나 회원이 탈퇴했다면 도메인 데이터를 생성하지 말고 attempt에 연결한 보상 환불을 사용한다.

## 환불·외부 호출 규칙

- `Refund.status`가 환불 상태의 원본이다. 취소·거절·환불 요청과 PG 실행을 분리해 PG 실패가 필요한 로컬 상태 변경을 되돌리지 않게 한다. 재시도에도 저장한 환불 idempotency key를 사용한다.
- REQUESTED는 로컬 상태와 환불 요청 저장 완료를 뜻한다. 고객에게는 소유권 확인 후 금액·상태만 제공하고, refundId·실패 이유·재시도 정보는 관리자용으로 유지한다.
- 관리자는 환불 요청 응답의 refundId로 상태를 조회한다. REQUESTED·PROCESSING을 polling하고 조치가 필요한 상태는 실패 환불 처리로 연결한다. 고객의 자동 복구 가능 상태 조회는 더 느린 주기를 사용할 수 있다.
- `ResilientPaymentProvider`가 통신·차단·timeout·실행 거절을 명시적 결과로 변환한다. port가 null을 반환하면 계약 오류로 실패시키고 PROCESSING 복구를 남긴다.
- timeout과 executor 변경은 [외부 호출 제한](references/resilience.md)을 따른다.
- 운영은 Toss provider를 사용한다. paymentKey·인증 정보·PG 응답 원문·민감값을 포함할 수 있는 예외 객체를 로그에 남기지 않는다.

## 검증

- 상태·금액은 해당 policy test, prepare·confirm은 `PaymentPrepareUseCaseTest`·`PaymentConfirmUseCaseIT`, 환불은 해당 환불 use case를 선택한다.
- provider는 `ResilientPaymentProviderTest`·`TossPaymentsProviderTest`, timeout은 정상 설정과 잘못된 시간 관계의 시작 실패를 확인한다.
- HTTP·화면이 바뀌면 `api-contract`·`happygallery-frontend-flows`를 함께 적용한다.

# Payment Map

## Likely code locations

- `domain/src/main/java/com/personal/happygallery/domain/payment/`
- `application/src/main/java/com/personal/happygallery/application/payment/`
- `application/src/main/java/com/personal/happygallery/application/order/`
- `application/src/main/java/com/personal/happygallery/application/booking/`
- `application/src/main/java/com/personal/happygallery/application/pass/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/payment/`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/payment/`
- `frontend/src/features/payment/`
- `frontend/src/pages/OrderCreatePage.tsx`
- `frontend/src/pages/BookingCreatePage.tsx`
- `frontend/src/pages/PassPurchasePage.tsx`
- `frontend/src/pages/PaymentSuccessPage.tsx`
- `frontend/src/pages/PaymentFailPage.tsx`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/LocalRefundFailureController.java`
- `bootstrap/src/main/resources/db/migration/`

## Current Toss prepare/confirm pieces

- `PaymentPort.confirm(paymentKey, orderId, amount)`
- `PaymentConfirmResult`
- `PaymentAttempt`, `PaymentContext`, `PaymentAttemptStatus`
- `PaymentAttemptReaderPort`, `PaymentAttemptStorePort`
- `TossPaymentsProvider`, `TossPaymentsProperties`, `TossPaymentsRestClientConfig`
- `FakePaymentProvider` for non-prod
- `CircuitBreakerPaymentProvider` wrapping confirm and refund
- `LocalRefundFailureScript` and `LocalRefundFailureController` for local refund-failure smoke/E2E hooks
- `V32__add_payment_attempt.sql`
- `V33__add_payment_key_columns.sql`

## High-value tests

- PaymentAttempt state and amount guard policy tests
- Payment prepare/confirm use case tests for order, booking, and pass purchase
- Duplicate confirm and amount mismatch tests
- Confirm success records final `paymentKey` on the target aggregate
- Refund failure durability tests such as `RefundExecutionServiceUseCaseIT`
- Circuit breaker and timeout provider tests under `application/src/test/java/com/personal/happygallery/adapter/out/external/payment/` until provider tests move with the adapter module
- Web contract tests for new payment prepare/confirm endpoints
- Local refund failure hook tests such as `LocalRefundFailureControllerTest` when admin dev refund hooks change

## Doc sync checklist

- Payment, refund, deposit, and pass purchase behavior: `docs/PRD/0001_기준_스펙/spec.md`
- Payment request/response contracts: `docs/PRD/0004_API_계약/spec.md`
- Payment design changes: matching ADRs listed in `SKILL.md`
- Active payment phase and remaining work: `HANDOFF.md` and `plan.md`
- Runtime env vars: `README.md`, deployment docs, and `docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md` when deployment settings change
- Local-only dev hooks: `docs/PRD/0004_API_계약/spec.md` and `docs/Idea/0009_로컬_개발_지원_경계/idea.md`

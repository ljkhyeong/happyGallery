---
name: happygallery-payment-flows
description: Repository-specific workflow for payment provider integration, Toss Payments prepare/confirm, PaymentAttempt state, payment confirm/refund providers, PG failure handling, circuit breaker, timeout, payment method rules, and refund log persistence in the happyGallery backend. Use when the request mentions payment, Toss, PG, prepare, confirm, paymentKey, PaymentAttempt, PaymentContext, PaymentConfirmResult, refund provider, refund retry, failed refund, circuit breaker, timeout, time limiter, EASY_PAY, BANK_TRANSFER, or RefundResult in the happyGallery repo. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and payment ADRs, preserve amount-tamper protection and refund retryability guarantees, run the smallest valid payment-related test scope, and update affected docs.
---

# happyGallery Payment Flows

## Core references

- Read `HANDOFF.md` first. If the Phase 1 Toss plan in HANDOFF is newer than this skill, follow HANDOFF and update this skill.
- Use `docs/PRD/0001_기준_스펙/spec.md` for payment, deposit, pass price, and refund rules.
- Use `docs/PRD/0004_API_계약/spec.md` when changing payment API contracts.
- Read the needed ADRs:
  - `docs/ADR/0008_결제_제공자_추상화/adr.md`
  - `docs/ADR/0009_예약금_결제_정책/adr.md`
  - `docs/ADR/0018_환불_이력_트랜잭션_분리/adr.md`
  - `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
  - `docs/ADR/0025_정상_종료와_Executor_정리_정책/adr.md`
  - `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
  - `docs/ADR/0030_타임아웃_계층과_ingress_keep_alive_기준선/adr.md`
  - `docs/ADR/0033_결제_confirm_트랜잭션과_보상_경계/adr.md`

## Current payment direction

- The runtime PG is Toss Payments directly, not PortOne.
- Use a prepare/confirm flow: server creates the order id and amount, frontend completes Toss checkout, backend confirms with `paymentKey`, `orderId`, and `amount`.
- Persist prepare state in `PaymentAttempt` with `PaymentContext` and `PaymentAttemptStatus`.
- Claim confirm with a short transaction, call the PG outside DB transactions, then persist approval and fulfillment in short transactions.
- Keep amount tamper checks in `PaymentAttempt.requireConfirmable(expectedAmount)` or the equivalent domain guard.
- Store confirmed Toss `paymentKey` on the final order, booking, or pass purchase record.
- Do not keep old direct-create endpoints as aliases when the payment contract changes; backend and frontend should switch together. Treat documented exceptions such as `POST /api/v1/me/cart/checkout` as migration gaps to close, not as the new default.

## Implementation judgment

- Keep prepare, claim, PG call, approval/fulfillment, and compensation visibly separated.
- Prepare owns external shape and server price resolution; claim owns persisted payload, authenticated actor, and amount invariants; fulfillment consumes the validated snapshot.
- Let `PaymentAttempt` and `Refund` own transitions. Keep external calls outside DB transactions through declarative propagation and dedicated dispatchers.
- Let `PaymentResilienceConfig` own CircuitBreaker, TimeLimiter, bounded executor, metrics, and lifecycle.

## Non-negotiable invariants

- Never trust client-submitted payment amount for final creation. Recalculate amount server-side from product, slot class price, or pass price.
- Booking deposit is class price 10%, calculated server-side.
- Pass purchase amount uses `PASS_TOTAL_PRICE` defaulting to 240000 unless the spec changes.
- Preserve circuit-breaker and timeout protection around external payment calls, including confirm and refund.
- Confirm timeout remains retryable with the same `orderId`; refund timeout is result-unknown and enters `RECONCILIATION_REQUIRED`.
- Keep refund records durable even when PG calls fail.
- Reuse the prepare `orderId` as confirm idempotency key and each persisted refund idempotency key for every retry.
- If PG confirm succeeds but fulfillment fails, create a `payment_attempt_id` compensation refund through the existing recovery/admin flow.
- Do not let PG failures roll back booking cancellation or order rejection flows that must complete locally.
- Treat `Refund.status` as the single source of truth. Do not copy asynchronous PG state onto booking, order, or pass aggregates.
- A cancel, reject, or pass-refund response with `REQUESTED` means the local transition and refund request were committed, not that PG refund completed.
- Expose customer progress only through an ownership-checked booking or order detail projection (`amount`, `status`). Keep `refundId`, failure reasons, and retry metadata admin-only.
- For admin flows, return `refundId` from the initiating action and query `GET /api/v1/admin/refunds/{refundId}`. Poll only `REQUESTED` and `PROCESSING`; hand action-required states to notifications and the failed-refund workflow. Customer detail may continue auto-recoverable states at a slower interval.
- Keep `FakePaymentProvider` out of `prod`; prod should use Toss-backed provider.
- Keep Toss secret values in environment variables, not tracked config.
- Prefer this skill over order, booking, or pass skills when the main change is the payment boundary itself.
- Follow ADR-0027 when adjusting tests; keep high-value amount-tamper, state-transition, confirm/refund durability, timeout, retry, and contract checks.

## Module placement

- Domain state and guards: `domain/src/main/java/com/personal/happygallery/domain/payment/`
- Payment use cases and ports: `application/src/main/java/com/personal/happygallery/application/payment/`
- Payment persistence adapters: `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/payment/`
- Toss/Fake/CircuitBreaker providers: `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/`
- Payment HTTP API: `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/payment/`
- Flyway: `bootstrap/src/main/resources/db/migration/`
- Frontend checkout UI: `frontend/src/features/payment/` and route wiring under `frontend/src/app` / `frontend/src/pages`

## Verification workflow

- Pure payment domain guard/state changes: `./gradlew :application:policyTest`
- Payment provider boundary: target `ResilientPaymentProviderTest` and `TossPaymentsProviderTest`.
- Payment use case/transaction: start with `PaymentPrepareUseCaseTest` or `PaymentConfirmUseCaseIT`, then add the affected order, booking, pass, or refund class.
- Payment web contract changes: `./gradlew :adapter-in-web:test --tests "*Payment*"`
- Frontend checkout changes: `cd frontend && npm run build`; use Playwright for a full checkout browser path when available.
- Broad payment confidence: combine the smallest relevant backend command with `cd frontend && npm run build` when the user-visible checkout changes.

Read `references/payment-map.md` for main files, tests, and doc sync notes.

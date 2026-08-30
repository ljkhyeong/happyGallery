---
name: happygallery-batch-flows
description: Repository-specific workflow for scheduled jobs, BatchScheduler cron configuration, BatchExecutor templates, BatchResult reporting, batch logging, manual batch trigger endpoints, and multi-item background processing in the happyGallery backend. Use when the request mentions batch, scheduler, cron, scheduled job, BatchScheduler, BatchExecutor, BatchResult, batch logging, expire-pickups, auto refund batch, pass expiry batch, reminder batch, or manual admin batch trigger in the happyGallery repo. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and batch-related ADRs, preserve scheduler timing and batch result contracts, run the smallest valid batch-related test scope, and update affected docs.
---

# happyGallery Batch Flows

## Core references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for time-boundary and admin batch trigger behavior.
- Read the needed ADRs:
  - `docs/ADR/0010_이용권_구매_만료_결정/adr.md`
  - `docs/ADR/0013_주문_승인_모델/adr.md`
  - `docs/ADR/0018_환불_이력_트랜잭션_분리/adr.md`
  - `docs/ADR/0027_테스트_전략과_최소_테스트_세트_기준선/adr.md`
  - `docs/ADR/0032_알림_Outbox_전달_보장/adr.md`
- Check `application/src/main/java/com/personal/happygallery/application/batch/` for the current scheduler and batch logging conventions.
- Check `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/SchedulingConfig.java` for scheduler wiring.

## Implementation judgment

- Keep candidate selection, per-item processing, retry/idempotency, and result aggregation visible.
- Use `BatchExecutor` only for candidate list → independent item processing → failure isolation → `BatchResult`.
- Avoid one wide transaction. Use short per-item transactions and transaction-free dispatchers for external calls.

## Non-negotiable invariants

- Preserve cron timing and `zone = "Asia/Seoul"` unless the spec changes.
- Keep `BatchResult(successCount, failureCount, failureReasons)` contracts stable.
- Treat `NotificationOutboxScheduler` as a separate polling path from `BatchScheduler`/`@BatchJob` unless their contracts are deliberately unified.
- Keep manual admin batch triggers aligned with scheduled behavior.
- Prefer booking, order, pass, or notification skills when the main change is domain policy rather than shared batch orchestration.
- Follow ADR-0027 for tests; prefer schedule, idempotency, failure-reporting, and batch-contract coverage over low-value batch plumbing tests.

## Verification workflow

- Target the owning test first: `OrderApprovalUseCaseIT`, `PickupExpireBatchUseCaseIT`, `PassPurchaseUseCaseIT`, `BookingReminderBatchUseCaseIT`, `PassExpiryNotificationUseCaseIT`, or `NotificationOutboxUseCaseIT`.
- Broad scheduled-flow confidence: `./gradlew --no-daemon :application:useCaseTest`

Read `references/batch-map.md` for main files, tests, and doc sync notes.

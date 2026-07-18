---
name: happygallery-notification-flows
description: Repository-specific workflow for notification delivery, notification sender selection, Kakao fallback to SMS, SMS verification sending, notification logs, async notification execution, and notification event handling in the happyGallery backend. Use when the request mentions notification, notifyGuest, notifyByGuestId, kakao, SMS, email receipt, push, notification sender, notification log, fallback order, duplicate notification prevention, or notification event types in the happyGallery repo. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and notification-related rules, preserve channel fallback and notification-log guarantees, run the smallest valid notification-related test scope, and update affected docs.
---

# happyGallery Notification Flows

## Core references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for notification policy and channel priority.
- Read the needed ADRs and code comments:
  - `docs/ADR/0010_이용권_구매_만료_결정/adr.md`
  - `docs/ADR/0013_주문_승인_모델/adr.md`
  - `docs/ADR/0025_정상_종료와_Executor_정리_정책/adr.md`
  - `docs/ADR/0028_배포_준비_알림_연동_로그_마스킹/adr.md`
  - `docs/ADR/0029_외부_HTTP_클라이언트_풀링_기준선/adr.md`
  - `docs/ADR/0032_알림_Outbox_전달_보장/adr.md`
- Check `application/src/main/java/com/personal/happygallery/application/notification/NotificationService.java` for the current sender ordering contract.

## Current delivery boundary

1. A domain transaction publishes `NotificationRequestedEvent`.
2. A synchronous listener stores the outbox in that transaction.
3. After commit, `notificationExecutor` invokes a transaction-free dispatcher.
4. Short transactions claim work and record results; `NotificationService` performs Kakao-first/SMS-fallback delivery.

## Non-negotiable invariants

- Preserve Kakao-first, SMS-fallback delivery order unless the spec changes.
- Keep notification-log persistence for success and failure outcomes.
- Keep outbox storage synchronous with the domain transaction; only dispatch is async through `notificationExecutor`.
- Store recipient IDs, not plaintext phone numbers, in outbox rows; resolve/decrypt at delivery time.
- Record a missing recipient as SYSTEM/FAILED and complete the outbox item instead of retrying forever.
- Preserve duplicate-notification prevention where batch or expiry reminders depend on notification logs.
- Phone verification SMS uses a dedicated `PhoneVerificationSender` path, not the general `NotificationEventType` chain; coordinate ownership rules with `happygallery-identity-flows`.
- Prefer booking, pass, order, or batch skills when the main change is domain policy rather than notification delivery.
- Follow ADR-0027 when touching tests; keep payload/contract, fallback-order, and duplicate-prevention checks, not low-value sender boilerplate tests.

## Verification workflow

- Outbox boundary: target `NotificationOutboxUseCaseIT`; fallback/log behavior: target `NotificationServiceTest`; external payload: target `NotificationSenderContractTest`.
- Wide confidence check: `./gradlew --no-daemon :application:useCaseTest`

Read `references/notification-map.md` for main files, tests, and doc sync notes.

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
  - `docs/ADR/0008_결제_제공자_추상화/adr.md`
- Check `application/src/main/java/com/personal/happygallery/application/notification/NotificationService.java` for the current sender ordering contract.

## Non-negotiable invariants

- Preserve Kakao-first, SMS-fallback delivery order unless the spec changes.
- Keep notification-log persistence for success and failure outcomes.
- Do not break async execution via `notificationExecutor`.
- Preserve duplicate-notification prevention where batch or expiry reminders depend on notification logs.
- Phone verification SMS uses a dedicated `PhoneVerificationSender` path, not the general `NotificationEventType` chain; coordinate ownership rules with `happygallery-identity-flows`.
- Prefer booking, pass, order, or batch skills when the main change is domain policy rather than notification delivery.
- Follow ADR-0027 when touching tests; keep payload/contract, fallback-order, and duplicate-prevention checks, not low-value sender boilerplate tests.

## Verification workflow

- Notification boundary changes: `./gradlew --no-daemon :application:useCaseTest --tests "*BookingReminderBatchUseCaseIT" --tests "*PassExpiryNotificationUseCaseIT"`
- Broader notification-impacting flows: `./gradlew --no-daemon :application:useCaseTest --tests "*BookingReminder*" --tests "*PassExpiryNotification*" --tests "*Order*"`
- Wide confidence check: `./gradlew --no-daemon :application:useCaseTest`

Read `references/notification-map.md` for main files, tests, and doc sync notes.

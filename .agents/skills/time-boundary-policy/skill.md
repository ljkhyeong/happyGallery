---
name: time-boundary-policy
description: Time-boundary and Clock workflow for happyGallery. Use when changing booking refund/reschedule cutoffs, pickup or approval expiry, pass expiry/reminders, retry timing, cron zones, TimeBoundary, Clocks, fixed-time tests, or direct now() calls in business code.
---

# happyGallery Time Boundary Policy

## Canonical references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for active cutoff and expiry rules.
- Read the affected booking/order/pass/payment ADR. Do not assume one global cutoff applies to every domain.

## Current rules

- Booking refund cutoff: D-1 00:00 Asia/Seoul.
- Same-day booking change cutoff: one hour before slot start.
- Pass expiry: purchase date plus 90 days; reminder: seven days before expiry.
- Scheduled jobs declare `zone = "Asia/Seoul"`.
- Refund/payment retry timing belongs to persisted retry state and scheduler configuration, not booking `TimeBoundary`.

## Implementation rules

- Use the injected `Clock`; do not call zero-argument `LocalDateTime.now()`, `Instant.now()`, or `ZonedDateTime.now()` in business logic.
- Use `domain/src/main/java/com/personal/happygallery/domain/time/TimeBoundary.java` for shared cutoff calculations and `Clocks` for controlled clock access.
- Production wiring lives in `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/ClockConfig.java`.
- Keep Asia/Seoul explicit where calendar-day semantics matter; do not rely on machine default timezone.
- Cover boundary instants immediately before, at, and after a cutoff with fixed clocks when the rule is risky.

## Verification

- Pure time rule: `./gradlew :application:policyTest`
- DB-backed booking/order/pass timing flow: targeted `./gradlew --no-daemon :application:useCaseTest --tests "*ClassName*"`
- Scheduler wiring: target `BatchSchedulerTest` or the affected batch integration test.
- Update PRD and the owning ADR whenever the actual cutoff or timezone contract changes.

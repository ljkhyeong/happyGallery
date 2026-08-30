---
name: happygallery-booking-flows
description: Repository-specific workflow for slot, booking, guest booking, reschedule, cancel, and booking time-boundary changes in the happyGallery backend. Use when the request mentions booking, reservation, guest booking, slot, booked_count, capacity, buffer, reschedule, cancel booking, access token, or booking history in the happyGallery repo. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and booking ADRs, preserve slot locking and refund/change cutoff rules, run the smallest valid booking test scope with --no-daemon for Testcontainers flows, and update affected docs.
---

# happyGallery Booking Flows

## Core references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for booking, slot, buffer, refund, and change rules.
- Read only the booking-related ADRs you need:
  - `docs/ADR/0003_슬롯_동시성_전략/adr.md`
  - `docs/ADR/0004_슬롯_관리_구현_결정/adr.md`
  - `docs/ADR/0005_비회원_예약_구현_결정/adr.md`
  - `docs/ADR/0006_예약_변경_결정/adr.md`
  - `docs/ADR/0007_예약_취소_결정/adr.md`
  - `docs/ADR/0018_환불_이력_트랜잭션_분리/adr.md`
  - `docs/ADR/0022_시스템_경계_상태_스키마_기준선/adr.md`
  - `docs/ADR/0032_알림_Outbox_전달_보장/adr.md`

## Implementation judgment

- Keep booking creation, slot capacity, history, and notification steps visible in the use case.
- Validate time/state in domain helpers, ownership/authentication in application services, and request shape in web DTOs.
- A fast pre-lock availability check followed by a locked recheck is intentional concurrency protection.

## Non-negotiable invariants

- Keep slot capacity updates and booking persistence inside the same transaction when the flow requires both.
- Preserve `SELECT ... FOR UPDATE` slot locking behavior around capacity confirmation.
- Slots have capacity 8. `bookedCount` is occupancy, `bufferBlockCount` counts overlapping blockers, and `adminActive` is operator controlled.
- Block buffer slots on the first booking and release them only when the last overlapping booking disappears.
- Keep refund cutoff at D-1 00:00 Asia/Seoul and same-day change cutoff at 1 hour before slot start.
- Do not break guest access-token flows or duplicate-booking prevention.
- Keep reminder and booking-history behavior aligned with the spec and ADRs.
- Publish booking notifications inside the domain transaction so outbox storage rolls back with it; delivery happens after commit.
- For paid booking creation, do not trust client-submitted deposit amounts; calculate deposit server-side as class price 10% and coordinate with `happygallery-payment-flows` for prepare/confirm.
- Do not persist final bookings before payment confirm in the Toss payment flow unless the active plan explicitly says otherwise.
- Add or keep only high-value booking tests per ADR-0027; prefer time-boundary, concurrency, and token-contract coverage over low-value mappings.

## Verification workflow

- Pure time-boundary or domain-rule changes: `./gradlew :application:policyTest`
- Target the affected class such as `GuestBookingUseCaseIT`, `BookingRescheduleUseCaseIT`, `BookingCancelUseCaseIT`, or `SlotBookingCapacityUseCaseIT`.

Read `references/booking-map.md` for the main files, tests, and document sync checklist.

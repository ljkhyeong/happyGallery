---
name: happygallery-pass-flows
description: Repository-specific workflow for pass purchase, pass credit, expiry, pass refund, no-show, and future-pass-booking cancellation changes in the happyGallery backend. Use when the request mentions pass, 8회권, credit, remaining credits, expires_at, pass refund, pass ledger, no-show, monthly course, or member pass purchase endpoints in the happyGallery repo. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and pass ADRs, preserve member-only pass ownership, 90-day expiry, and credit-consumption rules, run the smallest valid pass or booking test scope with --no-daemon for Testcontainers flows, and update affected docs.
---

# happyGallery Pass Flows

## Core references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for 8회권 purchase, expiry, credit use, refund, and future booking rules.
- Read the needed ADRs:
  - `docs/ADR/0010_이용권_구매_만료_결정/adr.md`
  - `docs/ADR/0011_이용권_사용_소모_환불_결정/adr.md`
  - `docs/ADR/0018_환불_이력_트랜잭션_분리/adr.md`

## Non-negotiable invariants

- Preserve the 90-day expiry calculation and 7-day expiry reminder timing.
- Keep pass purchase member-only; do not reintroduce guest-owned pass flows or guest pass endpoints.
- In the Toss prepare/confirm flow, create the paid pass only after payment confirm and use server-side `PASS_TOTAL_PRICE` for the amount.
- Keep remaining-credit updates and ledger entries consistent.
- Keep refund behavior aligned with remaining credits and automatic cancellation of future pass bookings.
- Do not accidentally turn booking cancel/no-show flows into double-consumption or double-refund paths.
- When adding or updating tests, follow ADR-0027 and keep only high-value pass, booking-cancel, or contract checks.

## Verification workflow

- Pure pass calculation or time-boundary changes: `./gradlew :application:policyTest`
- Pass use case changes: `./gradlew --no-daemon :application:useCaseTest --tests "*Pass*"`
- Pass changes that affect booking cancellation or no-show behavior: `./gradlew --no-daemon :application:useCaseTest --tests "*Pass*" --tests "*Booking*"`

Read `references/pass-map.md` for the main files, tests, and document sync checklist.

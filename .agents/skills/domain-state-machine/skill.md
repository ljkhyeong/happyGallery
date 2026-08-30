---
name: domain-state-machine
description: State-transition and domain-enum workflow for happyGallery. Use when changing OrderStatus, BookingStatus, PaymentAttemptStatus, RefundStatus, NotificationOutboxStatus, product/pass enums, transition guards, illegal-transition errors, or service code that duplicates domain state checks. Use with the owning domain skill for cross-layer changes.
---

# happyGallery Domain State Machine

## Canonical references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for allowed transitions and user-visible meaning.
- Read ADR-0002 for guard ownership, ADR-0013/0014 for order transitions, ADR-0018/0033 for refund/payment states, and ADR-0022 for the persisted state baseline.

## Rules

- Let domain entities or policy enums own legal transitions and mutation. Application services orchestrate authorization, transactions, other aggregates, and side effects.
- Prefer named guards and transition methods over scattered inline status comparisons. Keep an inline comparison when it is only query classification and not a transition rule.
- When adding or removing an enum value, search all switches, persistence queries, MyBatis mappings, JSON contracts, frontend types/badges, metrics labels, and migrations.
- Do not copy asynchronous refund state onto booking/order/pass aggregates. `Refund.status` remains the source of truth.
- Keep API identifiers in English and user-facing explanations in Korean.

## Main locations

- Order: `domain/src/main/java/com/personal/happygallery/domain/order/`
- Booking: `domain/src/main/java/com/personal/happygallery/domain/booking/`
- Payment/refund: `domain/src/main/java/com/personal/happygallery/domain/payment/`
- Pass/product/notification: their packages under `domain/src/main/java/com/personal/happygallery/domain/`
- Policy tests: `application/src/test/java/com/personal/happygallery/policy/`

## Verification

- Pure guard/transition rules: `./gradlew :application:policyTest`
- DB-backed transition flows: targeted class in `./gradlew --no-daemon :application:useCaseTest --tests "*ClassName*"`
- Exposed status contract: `./gradlew --no-daemon :adapter-in-web:restDocsTest`
- Update PRD, affected ADR, API contract, and frontend status mapping together when externally visible states change.

---
name: happygallery-order-flows
description: Repository-specific workflow for order state transitions, cart checkout, order approval, auto refund, refund execution, pickup expiry, made-to-order production, inventory deduction/restore, and fulfillment changes in the happyGallery backend. Use when the request mentions order, cart, cart checkout, approval pending, auto refund, pickup, production, made-to-order, fulfillment, inventory, reject order, approve order, expected ship date, delay request, or refund execution in the happyGallery repo. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and order ADRs, preserve approval timeout and production/pickup invariants, run the smallest valid order test scope with --no-daemon for Testcontainers flows, and update affected docs.
---

# happyGallery Order Flows

## Core references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for order, cart, product, inventory, pickup, production, and refund rules.
- Read only the needed ADRs:
  - `docs/ADR/0012_상품_재고_결정/adr.md`
  - `docs/ADR/0013_주문_승인_모델/adr.md`
  - `docs/ADR/0014_예약_제작_주문_결정/adr.md`
  - `docs/ADR/0018_환불_이력_트랜잭션_분리/adr.md`
  - `docs/ADR/0020_결제_제공자_CircuitBreaker/adr.md`
  - `docs/ADR/0022_시스템_경계_상태_스키마_기준선/adr.md`
  - `docs/ADR/0033_결제_confirm_트랜잭션과_보상_경계/adr.md`

## Implementation judgment

- Keep price resolution, inventory mutation, order creation, transition, refund request, and notification steps explicit.
- Validate exactly-one owner in factories/DB constraints, authorization in application services, and price/amount in payment prepare/confirm.

## Non-negotiable invariants

- Preserve the order state machine and guard methods instead of scattering inline status checks.
- Keep approval timeout auto-refund behavior and inventory restoration behavior consistent.
- Do not allow cancellation after production start when the spec/ADR forbids it.
- Keep pickup-expiry auto-refund behavior for ready stock and exclude made-to-order pickup from that path.
- Keep external payment calls retryable and isolated from core state transitions.
- In the Toss prepare/confirm flow, do not finalize paid orders from client amount alone; payment prepare/confirm should own amount validation and final order fulfillment.
- `OrderItemRequest` carries the server-confirmed unit-price snapshot. Do not silently reread a later product price during fulfillment.
- Treat `POST /api/v1/me/cart/checkout` as the current cart-to-order path and a documented payment-bypass gap until `plan.md` closes or reclassifies it.
- Add or keep only high-value order tests per ADR-0027; prefer state transition, timeout, refund, and inventory restoration coverage over low-value controller boilerplate tests.

## Verification workflow

- Pure order state or inventory rule changes: `./gradlew :application:policyTest`
- Target `OrderApprovalUseCaseIT`, `OrderProductionUseCaseIT`, `PickupExpireBatchUseCaseIT`, `ProductInventoryUseCaseIT`, or `ConcurrentOrderUseCaseIT` according to the changed risk.

Read `references/order-map.md` for the main files, tests, and document sync checklist.

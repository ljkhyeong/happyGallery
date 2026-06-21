---
name: happygallery-product-flows
description: Repository-specific workflow for product catalog, product status, inventory create/deduct/restore, product query, product Q&A, product admin APIs, and product-inventory persistence changes in the happyGallery backend. Use when the request mentions product, catalog, inventory, stock, quantity, ACTIVE products, made-to-order product setup, product detail response, product Q&A, product query service, or admin product registration in the happyGallery repo. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and product ADRs, preserve product and inventory invariants, run the smallest valid product-related test scope, and update affected docs.
---

# happyGallery Product Flows

## Core references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for product, inventory, catalog, and product Q&A behavior.
- Read the needed ADRs:
  - `docs/ADR/0012_상품_재고_결정/adr.md`
  - `docs/ADR/0013_주문_승인_모델/adr.md`
  - `docs/ADR/0014_예약_제작_주문_결정/adr.md`

## Non-negotiable invariants

- Keep product creation and inventory creation coordinated through the intended service boundary.
- Preserve inventory deduction and restore rules and the lock-based repository access pattern where needed.
- Keep product type handling consistent between ready stock and made-to-order paths.
- Keep product Q&A create, verify, answer, and visibility behavior aligned between public, member, and admin APIs.
- Prefer the order skill when the main change is order state or fulfillment, even if inventory is touched indirectly.
- Paid product order creation should coordinate with `happygallery-payment-flows`; inventory deduction and final order creation must remain consistent with payment confirm.
- Follow ADR-0027 when touching tests; keep only high-value inventory rule, product registration, and externally visible contract checks.

## Verification workflow

- Product or inventory rule changes: `./gradlew :application:policyTest`
- Product/inventory use case changes: `./gradlew --no-daemon :application:useCaseTest --tests "*ProductInventory*" --tests "*Order*"`
- Broad product confidence: `./gradlew --no-daemon :application:useCaseTest`

Read `references/product-map.md` for main files, tests, and doc sync notes.

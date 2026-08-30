---
name: happygallery-admin-flows
description: Repository-specific workflow for backend admin work in the happyGallery Spring app. Use when the request primarily changes admin controllers, admin APIs, admin authentication, admin filters, admin Bearer session handling, refund retry endpoints, notice/inquiry/product-Q&A moderation, slot administration endpoints, rate limiting for admin routes, local-only admin dev hooks, or other operator-only backend flows under `adapter-in-web/`, `application/`, `domain/`, `adapter-out-persistence/`, or `adapter-out-external/`. Do not use this skill for frontend-only admin page or component work under `frontend/`; use `happygallery-frontend-flows` instead. Read HANDOFF.md first, align changes with docs/PRD/0001_기준_스펙/spec.md and admin-related ADRs, preserve admin endpoint contracts and filter order, run the smallest valid admin-related test scope, and update affected docs.
---

# happyGallery Admin Flows

## Core references

- Read `HANDOFF.md` first.
- Use `docs/PRD/0001_기준_스펙/spec.md` for admin API contracts.
- Read the needed ADRs:
  - `docs/ADR/0016_API_버전_전략/adr.md`
  - `docs/ADR/0017_Filter_처리율_제한/adr.md`
  - `docs/ADR/0023_관리자_회원_인증_세션_기준선/adr.md`
- Read the booking, order, pass, product, or payment ADR only when the admin endpoint changes that domain behavior.

## Implementation judgment

- Keep controller validation and DTO conversion thin; keep operator workflow and domain decisions in the owning application/domain service.
- Prefer explicit auth-source and endpoint behavior over helpers that hide 401, 404, audit, or rate-limit semantics.
- Reuse high-value auth, setup, filter, and endpoint contract tests; do not add a test for every trivial mapping.

## Non-negotiable invariants

- Preserve `RequestIdFilter -> RateLimitFilter -> admin SecurityFilterChain` ordering, with `AdminAuthenticationFilter` before `AnonymousAuthenticationFilter` inside the chain.
- Keep admin endpoint paths and response DTO contracts aligned with the PRD.
- Preserve Redis-backed Bearer session behavior, `AdminPrincipal` propagation through `SecurityContext`, and 401 handling.
- Keep `Authorization: Bearer` as the primary contract. `X-Admin-Key` is an explicitly enabled local/test fallback, not an admin identity header.
- Keep one-time setup guarded by configuration, an empty admin table, and a constant-time token check; return 404 when setup is unavailable.
- Do not weaken admin protection or accidentally expose broader public routes.
- If the change only touches admin UI components or pages under `frontend/`, use `happygallery-frontend-flows` instead.
- When a change is domain-specific and not about admin concerns, prefer the narrower booking/order/pass/product/payment skill.
- Follow ADR-0027 when changing tests; keep auth/filter/runtime-contract checks that protect operator risk, and avoid padding coverage with low-value admin endpoint boilerplate.

## Verification workflow

- Filter or auth changes: `./gradlew :adapter-in-web:test --tests "*SecurityBoundaryUseCaseIT" --tests "*AdminLoginUseCaseIT" --tests "*RateLimitFilterTest"`
- Setup changes: target `AdminSetupControllerTest` and `DefaultAdminSetupServiceTest` in their owning modules.
- Admin endpoint changes: target the affected `:adapter-in-web:test --tests "*Admin...*"`; add application tests only when its service changed.

Read `references/admin-map.md` for main files, tests, and doc sync notes.

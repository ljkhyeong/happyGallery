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
  - `docs/ADR/0008_결제_제공자_추상화/adr.md`
  - `docs/ADR/0010_이용권_구매_만료_결정/adr.md`
  - `docs/ADR/0011_이용권_사용_소모_환불_결정/adr.md`
  - `docs/ADR/0013_주문_승인_모델/adr.md`
  - `docs/ADR/0014_예약_제작_주문_결정/adr.md`

## Non-negotiable invariants

- Preserve `RequestIdFilter -> RateLimitFilter -> AdminAuthFilter` ordering where applicable.
- Keep admin endpoint paths and response DTO contracts aligned with the PRD.
- Preserve Redis-backed Bearer session behavior, admin identity propagation, and 401 handling.
- Treat legacy `X-Admin-Id` compatibility and persisted admin history fields consistently where older flows still depend on them, but do not treat the header as the primary runtime contract.
- Do not weaken admin protection or accidentally expose broader public routes.
- If the change only touches admin UI components or pages under `frontend/`, use `happygallery-frontend-flows` instead.
- When a change is domain-specific and not about admin concerns, prefer the narrower booking/order/pass/product/payment skill.
- Follow ADR-0027 when changing tests; keep auth/filter/runtime-contract checks that protect operator risk, and avoid padding coverage with low-value admin endpoint boilerplate.

## Verification workflow

- Filter or auth changes: `./gradlew :adapter-in-web:test --tests "*AdminAuthFilterTest" --tests "*RateLimitFilterTest"`
- Admin integration endpoint changes: `./gradlew --no-daemon :application:useCaseTest --tests "*Admin*"`
- Broad admin confidence: `./gradlew --no-daemon :application:useCaseTest`

Read `references/admin-map.md` for main files, tests, and doc sync notes.

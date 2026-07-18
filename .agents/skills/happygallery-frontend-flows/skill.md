---
name: happygallery-frontend-flows
description: Repository-specific workflow for frontend-only work in the happyGallery Vite + React app. Use when the request primarily changes files under `frontend/`, including pages, React components, Vite routes, form validation, error UX, responsive polish, browser flow verification, sessionStorage admin token handling, React Query usage, Toss checkout UI, phone verification UI, or shared frontend styling in the happyGallery repo. Do not use this skill when backend API contracts, controller behavior, auth filters, DB schema, or domain rules are the main change; use the matching happyGallery backend skill instead. Read HANDOFF.md first, keep frontend behavior aligned with README.md and the PRD/API docs, preserve shared admin auth and error-handling patterns, run the right frontend verification for the kind of UI change, and update affected docs when routes or user-visible behavior change.
---

# happyGallery Frontend Flows

## Session bootstrap

- Read `HANDOFF.md` before changing frontend code.
- Use `HANDOFF.md` only for active work and remaining actions, not as route or runtime authority.
- Use `README.md` for the current route and documentation index overview.
- Use `docs/PRD/0001_기준_스펙/spec.md` for user-facing policy wording and route behavior.
- Use `docs/PRD/0004_API_계약/spec.md` when the change depends on request or response contracts.

## Scope and ownership

- This skill owns changes under `frontend/` for frontend-only work.
- Keep route-level wiring in `frontend/src/pages` and `frontend/src/app`.
- Keep domain UI, forms, queries, and mutations in `frontend/src/features/<feature>`.
- Keep shared HTTP, types, utility mapping, and reusable UI in `frontend/src/shared`.
- If the request also needs backend work, coordinate with the matching happyGallery backend skill and verify both sides of the contract.

## Non-negotiable frontend patterns

- Preserve the existing Vite + React + TypeScript + Bootstrap stack unless the user explicitly asks to change it.
- Reuse shared primitives such as `ErrorAlert`, `LoadingSpinner`, `EmptyState`, `StatusBadge`, and toast helpers instead of creating one-off variants.
- Keep server state in React Query and go through the shared API client and shared error model.
- Preserve admin authentication flow:
  - admin token stays in `sessionStorage` via `useAdminKey()`
  - the storage key is `hg_admin_token`
  - `AdminPage` passes `onAuthError` down to admin feature components
  - 401 handling clears the stored token and pushes the user back through the admin gate
- Keep route paths aligned with the PRD and executable registry in `frontend/src/app/App.tsx`.
- Treat the route list in `references/frontend-map.md` as a navigation aid, not a second route registry.
- For Toss checkout work, keep `VITE_TOSS_CLIENT_KEY` client-side only and coordinate backend prepare/confirm contracts with `happygallery-payment-flows`.
- For phone verification UI, do not expose verification codes in production UI; coordinate ownership rules with `happygallery-identity-flows`.
- When editing styles, preserve the current visual language based on Pretendard, Bootstrap variables, and shared global styles unless the request is a deliberate redesign.

## Change workflow

1. Map the request to the affected routes, pages, feature modules, and API contracts.
2. Confirm that the work is frontend-only. If backend work is required, hand off to the matching backend skill instead of stretching this one.
3. Modify the narrowest feature and shared modules that can own the change.
4. If user-visible behavior, routes, or API usage changed, update the relevant docs in the repo.
5. Run the smallest valid verification command before finishing.

## Verification workflow

- Static UI, component, styling, or query-state changes: `cd frontend && npm run build`
- Responsive or visual work: build, then inspect mobile and desktop with Playwright or the in-app browser.
- Payment, identity, or admin flows: build, then choose `npm run e2e:payment`, `e2e:identity`, or `e2e:admin`; reserve `e2e:full` for broad shared changes.
- Client-side monitoring or production telemetry work: `cd frontend && npm run build`, then combine with `sentry`.
- If the request turns out to require backend API changes, stop and switch to the matching backend skill rather than adding Gradle verification from this workflow.

## References

- Read `references/frontend-map.md` for the main file map, recurring frontend patterns, route list, and doc sync checklist.

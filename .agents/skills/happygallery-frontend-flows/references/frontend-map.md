# Frontend Map

## Core docs

- `HANDOFF.md`
- `README.md`
- `docs/PRD/0001_기준_스펙/spec.md`
- `docs/PRD/0004_API_계약/spec.md`

## Main code map

- `frontend/src/app/App.tsx`
  Route registration and top-level providers.
- `frontend/src/pages/admin/AdminPage.tsx`
  Admin shell, auth gate handoff, and cross-feature `onAuthError` wiring.
- `frontend/src/pages`
  Route-level page shells for public, guest, member, and admin flows.
- `frontend/src/features`
  Domain-specific UI, forms, queries, and mutations.
- `frontend/src/shared/api`
  Shared API client, query client, and error handling.
- `frontend/src/shared/ui`
  Shared alerts, loading, empty states, toasts, badges, and layout.
- `frontend/src/shared/lib`
  Shared helpers such as user-facing error message mapping.
- `frontend/src/shared/types`
  Shared DTO and API response typing.
- `frontend/src/styles`
  Global styling and Bootstrap variable overrides.

## Frequently reused files

- `frontend/src/shared/api/client.ts`
  Shared fetch client and `ApiError` entry point.
- `frontend/src/shared/lib/errorMessages.ts`
  User-facing error code to message mapping.
- `frontend/src/shared/ui/ErrorAlert.tsx`
  Standard request failure rendering.
- `frontend/src/shared/ui/Layout.tsx`
  Top-level shell and navigation.
- `frontend/src/features/admin-product/useAdminKey.ts`
  `hg_admin_token` session storage handling.
- `frontend/src/features/customer-auth/useCustomerAuth.ts`
  Customer auth bootstrap and post-login global state sync.
- `frontend/src/shared/api/queryClient.ts`
  React Query defaults.
- `frontend/src/features/payment/`
  Toss prepare/confirm client helpers, SDK loading, and redirect hint storage.
- `frontend/src/pages/PaymentSuccessPage.tsx` and `frontend/src/pages/PaymentFailPage.tsx`
  Toss return routes and confirm result handling.
- `frontend/src/features/monitoring/api.ts`
  Best-effort client event telemetry for guest/member conversion flows.
- `frontend/src/features/refund/RefundProgressAlert.tsx`
  Customer-safe refund progress rendering for booking and order details.
- `frontend/src/features/admin-refund/useAdminRefundPolling.ts`
  Admin polling for refund requests initiated from order and pass actions.

## Stable frontend conventions

- Use React Query for server state instead of ad hoc fetch state in each page.
- Reuse the shared API client and `ApiError` model for HTTP requests.
- Show request failures through `ErrorAlert` unless the interaction is toast-only.
- Use shared toast helpers for success and short-lived feedback.
- Keep page-level layout in page components and detailed form logic in feature components.
- Do not present an asynchronous refund request as completed. Poll `REQUESTED` and `PROCESSING` quickly, back off for stuck or auto-recoverable states, and use the server `Refund` projection as the source of truth.
- Preserve the Pretendard plus Bootstrap styling direction already in `frontend/src/styles`.

## Admin UI rules

- Admin token storage lives in `frontend/src/features/admin-product/useAdminKey.ts`.
- The session storage key name is `hg_admin_token`.
- `frontend/src/pages/admin/AdminPage.tsx` owns admin auth state and passes `onAuthError` to child feature components.
- When an admin API returns 401, clear the stored token and return the user to the admin gate.

## Current user-facing routes

- `/`
- `/notices/:id`
- `/products`
- `/products/:id`
- `/bookings/new`
- `/guest`
- `/guest/bookings`
- `/passes/purchase`
- `/cart`
- `/orders/new`
- `/guest/orders`
- `/my`
- `/my/orders`
- `/my/orders/:id`
- `/my/bookings`
- `/my/bookings/:id`
- `/my/passes`
- `/my/inquiries`
- `/my/inquiries/new`
- `/auth/callback/google`
- `/login`
- `/signup`
- `/admin`
- `/payments/success`
- `/payments/fail`

## Verification and doc sync

- Default frontend verification: `cd frontend && npm run build`
- Responsive or visual QA work should also use `screenshot`.
- Multi-step browser flow changes should also use `playwright` or `playwright-interactive`.
- Client-side observability work should also use `sentry`.
- If backend API contracts changed, this is no longer a frontend-only task. Switch to the matching backend skill instead of extending this workflow.
- Update docs when any of these change:
  - public or admin routes
  - request or response contracts
  - admin auth behavior
  - major UX states called out in the PRD or `HANDOFF.md`

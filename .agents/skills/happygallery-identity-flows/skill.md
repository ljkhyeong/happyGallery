---
name: happygallery-identity-flows
description: Repository-specific workflow for customer identity, signup/login, Google social login, phone verification, SMS verification ownership, guest history claim, PhoneVerification, VerifiedGuestResolver, PhoneOwnershipVerificationUseCase, LocalPhoneVerificationController, and customer authentication changes in the happyGallery repo. Use when the request mentions 회원가입, 로그인, 휴대폰 인증, SMS 인증, phone verification, phone ownership, guest claim, 비회원 이력 가져오기, Google OAuth, SocialAuth, CustomerAuth, VerifiedGuestResolver, or PhoneOwnershipVerificationUseCase. Read HANDOFF.md first, preserve phone ownership guarantees and local-only verification helpers, run the smallest valid identity-related test scope, and update affected docs.
---

# happyGallery Identity Flows

## Core references

- Read `HANDOFF.md` first, especially the money/identity restoration plan.
- Use `docs/PRD/0001_기준_스펙/spec.md` for member, guest, and phone verification behavior.
- Use `docs/PRD/0004_API_계약/spec.md` when changing auth or verification contracts.
- Read the needed ADRs:
  - `docs/ADR/0005_비회원_예약_구현_결정/adr.md`
  - `docs/ADR/0023_관리자_회원_인증_세션_기준선/adr.md`
  - `docs/ADR/0024_비회원_토큰_강화/adr.md`

## Scope vs `happygallery-member-flows`

- Use **this skill** when the change touches signup, login, Google OAuth, phone ownership verification, or the SMS verification boundary itself (sending codes, validating codes, rate limits, ownership rules).
- Use `happygallery-member-flows` when the change touches the member-only `/api/v1/me/**` API surface, MeController endpoints, MyPage UI, or guest-to-member claim mechanics built on top of an already-verified identity.
- For SMS delivery infrastructure, coordinate with `happygallery-notification-flows` but keep ownership rules here.

## Non-negotiable invariants

- Do not store or trust a member phone number at signup without ownership verification once Phase 3 is implemented.
- Do not reuse `VerifiedGuestResolver` for signup ownership if it would also upsert guest history; use a dedicated ownership verification use case.
- Keep local/dev verification helpers under `local` profile only.
- Keep verification codes out of production API responses.
- Preserve guest access-token flows and guest-to-member claim behavior.
- Keep rate limits for phone verification and auth endpoints aligned with `application.yml` and filter tests.
- If SMS delivery is the main change, coordinate with `happygallery-notification-flows`, but keep phone ownership rules here.

## Module placement

- Customer auth use cases and ports: `application/src/main/java/com/personal/happygallery/application/customer/`
- Phone verification domain object: `domain/src/main/java/com/personal/happygallery/domain/booking/PhoneVerification.java`
- Phone verification persistence: `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/PhoneVerificationRepository.java`
- Auth and local verification HTTP APIs: `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/`
- Google OAuth adapters and properties: `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/oauth/`
- Frontend auth and verification UI: `frontend/src/features/customer-auth/`, `frontend/src/features/booking-create/`, and claim-related frontend modules

## Verification workflow

- Customer auth web changes: `./gradlew :adapter-in-web:test --tests "*CustomerAuth*" --tests "*RateLimit*"`
- Guest claim or phone verification use case changes: `./gradlew --no-daemon :application:useCaseTest --tests "*GuestClaim*" --tests "*GuestBooking*" --tests "*CustomerAuth*"`
- Frontend auth or phone verification UI changes: `cd frontend && npm run build`; use Playwright for multi-step auth/claim flows when needed.
- Broad identity confidence: combine the smallest backend identity test scope with `cd frontend && npm run build` when frontend behavior changes.

Read `references/identity-map.md` for main files, tests, and doc sync notes.

# Identity Map

## Likely code locations

- `application/src/main/java/com/personal/happygallery/application/customer/`
- `application/src/main/java/com/personal/happygallery/application/customer/port/in/`
- `application/src/main/java/com/personal/happygallery/application/customer/port/out/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/oauth/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/booking/BookingController.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/LocalPhoneVerificationController.java`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/PhoneVerificationRepository.java`
- `domain/src/main/java/com/personal/happygallery/domain/booking/PhoneVerification.java`
- `domain/src/main/java/com/personal/happygallery/domain/error/PhoneVerification*.java`
- `frontend/src/features/customer-auth/`
- `frontend/src/features/customer-claim/`
- `frontend/src/features/booking-create/PhoneVerificationStep.tsx`

## High-value tests

- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/customer/CustomerAuthUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/customer/CustomerGuestClaimUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/booking/GuestBookingUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/RateLimitFilterTest.java`

## Doc sync checklist

- Customer and guest behavior: `docs/PRD/0001_기준_스펙/spec.md`
- Auth, signup, Google/Naver OAuth, verification, and claim API contracts: `docs/PRD/0004_API_계약/spec.md`
- Guest token and admin/member auth decisions: matching ADRs listed in `SKILL.md`
- Active Phase 2/3 status: `HANDOFF.md` and `plan.md`

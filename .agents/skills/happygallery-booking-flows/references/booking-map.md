# Booking Map

## Likely code locations

- `application/src/main/java/com/personal/happygallery/application/booking/`
- `application/src/main/java/com/personal/happygallery/application/pass/`
- `application/src/main/java/com/personal/happygallery/application/batch/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/booking/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/`
- `domain/src/main/java/com/personal/happygallery/domain/booking/`
- `domain/src/main/java/com/personal/happygallery/domain/pass/`
- `domain/src/main/java/com/personal/happygallery/domain/time/TimeBoundary.java`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/pass/`

## High-value tests

- `application/src/test/java/com/personal/happygallery/policy/TimeBoundaryPolicyTest.java`
- `application/src/test/java/com/personal/happygallery/application/booking/GuestBookingUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/booking/BookingRescheduleUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/booking/BookingCancelUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/booking/ConcurrentBookingUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/booking/BookingReminderBatchUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassPurchaseUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassCreditUsageUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassExpiryNotificationUseCaseIT.java`

## Doc sync checklist

- Booking, pass, slot, and time-boundary behavior: `docs/PRD/0001_기준_스펙/spec.md`
- Session state and remaining work: `HANDOFF.md`
- Booking/pass design changes: the matching ADR listed in `SKILL.md`

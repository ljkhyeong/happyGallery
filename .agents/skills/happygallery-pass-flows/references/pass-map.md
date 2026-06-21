# Pass Map

## Likely code locations

- `application/src/main/java/com/personal/happygallery/application/pass/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/`
- `application/src/main/java/com/personal/happygallery/application/booking/`
- `application/src/main/java/com/personal/happygallery/application/batch/`
- `domain/src/main/java/com/personal/happygallery/domain/pass/`
- `domain/src/main/java/com/personal/happygallery/domain/booking/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/pass/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/`
- `domain/src/main/java/com/personal/happygallery/domain/time/TimeBoundary.java`

## High-value tests

- `application/src/test/java/com/personal/happygallery/policy/TimeBoundaryPolicyTest.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassPurchaseUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassCreditUsageUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassExpiryNotificationUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/booking/BookingCancelUseCaseIT.java`

## Doc sync checklist

- Pass purchase, expiry, credit, refund, and future-booking behavior: `docs/PRD/0001_기준_스펙/spec.md`
- Session state and remaining work: `HANDOFF.md`
- Pass design changes: the matching ADR listed in `SKILL.md`

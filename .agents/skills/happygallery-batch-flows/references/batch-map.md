# Batch Map

## Likely code locations

- `application/src/main/java/com/personal/happygallery/application/batch/`
- `application/src/main/java/com/personal/happygallery/application/order/DefaultOrderAutoRefundBatchService.java`
- `application/src/main/java/com/personal/happygallery/application/order/DefaultPickupExpireBatchService.java`
- `application/src/main/java/com/personal/happygallery/application/booking/DefaultBookingReminderBatchService.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/`
- `application/src/main/java/com/personal/happygallery/application/order/port/in/PickupExpireBatchUseCase.java`
- `application/src/main/java/com/personal/happygallery/application/pass/port/in/PassExpiryBatchUseCase.java`

## High-value tests

- `application/src/test/java/com/personal/happygallery/application/booking/BookingReminderBatchUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/order/PickupExpireBatchUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassExpiryNotificationUseCaseIT.java`

## Doc sync checklist

- Scheduled behavior and admin batch trigger behavior: `docs/PRD/0001_기준_스펙/spec.md`
- Batch conventions and expiry/auto-refund decisions: matching ADRs listed in `SKILL.md`
- Session status and remaining work: `HANDOFF.md`

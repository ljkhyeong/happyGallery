# Notification Map

## Likely code locations

- `application/src/main/java/com/personal/happygallery/application/notification/`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/notification/`
- `application/src/main/java/com/personal/happygallery/application/booking/BookingSupport.java`
- `application/src/main/java/com/personal/happygallery/application/order/DefaultOrderApprovalService.java`
- `application/src/main/java/com/personal/happygallery/application/order/OrderAutoRefundProcessor.java`
- `application/src/main/java/com/personal/happygallery/application/order/PickupExpireProcessor.java`
- `application/src/main/java/com/personal/happygallery/application/pass/DefaultPassExpiryBatchService.java`
- `application/src/main/java/com/personal/happygallery/application/pass/port/in/PassExpiryBatchUseCase.java`

## High-value tests

- `application/src/test/java/com/personal/happygallery/application/booking/BookingReminderBatchUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/pass/PassExpiryNotificationUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/order/OrderApprovalUseCaseIT.java`

## Doc sync checklist

- Notification policy and channel order: `docs/PRD/0001_기준_스펙/spec.md`
- Batch reminder duplication rules and expiry notification behavior: matching ADRs listed in `SKILL.md`
- Session status and remaining work: `HANDOFF.md`

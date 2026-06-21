# Order Map

## Likely code locations

- `application/src/main/java/com/personal/happygallery/application/order/`
- `application/src/main/java/com/personal/happygallery/application/product/`
- `application/src/main/java/com/personal/happygallery/application/cart/`
- `application/src/main/java/com/personal/happygallery/application/batch/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/MeCartController.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/`
- `domain/src/main/java/com/personal/happygallery/domain/product/`
- `domain/src/main/java/com/personal/happygallery/domain/cart/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/order/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/product/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/cart/`
- `adapter-out-external/src/main/java/com/personal/happygallery/adapter/out/external/payment/`

## High-value tests

- `application/src/test/java/com/personal/happygallery/policy/OrderStatusTransitionPolicyTest.java`
- `application/src/test/java/com/personal/happygallery/policy/InventoryPolicyTest.java`
- `application/src/test/java/com/personal/happygallery/application/order/OrderApprovalUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/order/OrderProductionUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/order/PickupExpireBatchUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/order/ConcurrentOrderUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/product/ProductInventoryUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/adapter/out/external/payment/CircuitBreakerPaymentProviderTest.java`

## Doc sync checklist

- Order, cart checkout, pickup, production, refund, and inventory behavior: `docs/PRD/0001_기준_스펙/spec.md`
- Session state and remaining work: `HANDOFF.md`
- Order/inventory design changes: the matching ADR listed in `SKILL.md`

# Product Map

## Likely code locations

- `application/src/main/java/com/personal/happygallery/application/product/`
- `application/src/main/java/com/personal/happygallery/application/qna/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/product/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/customer/MeProductQnaController.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/AdminProductQnaController.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/`
- `domain/src/main/java/com/personal/happygallery/domain/product/`
- `domain/src/main/java/com/personal/happygallery/domain/qna/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/product/`
- `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/qna/`

## High-value tests

- `application/src/test/java/com/personal/happygallery/policy/InventoryPolicyTest.java`
- `application/src/test/java/com/personal/happygallery/application/product/ProductInventoryUseCaseIT.java`
- `application/src/test/java/com/personal/happygallery/application/order/OrderApprovalUseCaseIT.java`

## Doc sync checklist

- Product, inventory, Q&A, and made-to-order behavior: `docs/PRD/0001_기준_스펙/spec.md`
- Product design changes: matching ADRs listed in `SKILL.md`
- Session status and remaining work: `HANDOFF.md`

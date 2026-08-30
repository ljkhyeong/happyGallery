# Admin Map

## Likely code locations

- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/config/SecurityConfig.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/security/admin/`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/admin/`
- `application/src/main/java/com/personal/happygallery/application/batch/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/admin/LocalRefundFailureController.java` for local-only refund failure hooks

## Out of scope

- If the change is limited to `frontend/src/pages/admin/` or `frontend/src/features/admin-*`, use `happygallery-frontend-flows`.

## High-value tests

- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/RateLimitFilterTest.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/SecurityBoundaryUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/admin/AdminLoginUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/admin/AdminSetupControllerTest.java`
- `application/src/test/java/com/personal/happygallery/application/admin/DefaultAdminSetupServiceTest.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/admin/AdminSlotUseCaseIT.java`

## Doc sync checklist

- Admin endpoint contracts and security notes: `docs/PRD/0001_기준_스펙/spec.md`
- API versioning, filters, and admin route policy: matching ADRs listed in `SKILL.md`
- Local-only admin dev APIs: `docs/PRD/0004_API_계약/spec.md` and `docs/Idea/0009_로컬_개발_지원_경계/idea.md`
- Session status and remaining work: `HANDOFF.md`

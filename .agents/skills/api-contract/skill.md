---
name: api-contract
description: API contract workflow for happyGallery. Use when adding or changing endpoints, request/response DTOs, error codes, HTTP statuses, `/api/v1` paths, `ErrorResponse`, `GlobalExceptionHandler`, REST Docs contracts, security exposure, pagination parameters, or compatibility rules. Use with the owning domain skill when behavior also changes.
---

# happyGallery API Contract

## Canonical references

- Read `HANDOFF.md` first for active work only.
- Use `docs/PRD/0004_API_계약/spec.md` as the detailed HTTP contract.
- Use `docs/PRD/0001_기준_스펙/spec.md` for user behavior and policy.
- Read ADR-0016 for versioning, ADR-0017 for rate-limit boundaries, ADR-0023 for authentication/CSRF, and the owning domain ADR when relevant.

## Contract rules

- Keep application routes under `/api/v1`; do not restore removed legacy aliases.
- Return named DTOs directly for success. Do not return raw `Map<String, ?>` or declare request/response types inside controllers.
- Keep error responses as `{ "code": "UPPER_SNAKE_CASE", "message": "..." }` and map authentication to 401, authorization/CSRF to 403, missing resources to 404, invalid input to 400, and business conflicts to 409 only when the current exception policy specifies it.
- Treat response field removal, rename, type change, enum value change, and pagination semantic change as compatibility work, not a local refactor.
- Keep admin endpoints behind the admin Spring Security chain and `/api/v1/me/**` behind `ROLE_CUSTOMER`.
- Preserve SPA CSRF for non-admin state changes and Bearer/API-key header authentication for admin APIs.
- Validate request shape in web DTOs; keep ownership, state, amount, and cross-aggregate rules in application/domain layers.

## Main locations

- Controllers and DTOs: `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/`
- Error mapping: `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/GlobalExceptionHandler.java`
- Security: `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/config/SecurityConfig.java`
- Domain errors: `domain/src/main/java/com/personal/happygallery/domain/error/`
- REST Docs tests: `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/restdocs/`

## Verification

- HTTP contract: `./gradlew --no-daemon :adapter-in-web:restDocsTest`
- Controller/filter/error behavior: targeted `:adapter-in-web:test --tests "*ClassName*"`
- Security boundary: target `SecurityBoundaryUseCaseIT` and the affected auth/rate-limit test.
- Update PRD-0004 first, then PRD-0001 or ADR only when behavior or a durable decision changed.

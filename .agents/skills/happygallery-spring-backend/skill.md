---
name: happygallery-spring-backend
description: Repository-specific fallback workflow for general happyGallery backend work that spans multiple domains or does not fit a narrower happyGallery skill. Use when the request touches several modules at once, asks for a broad repository review, changes shared Gradle or module structure, updates common config, or needs the repository-wide layering rules, Flyway conventions, test-selection rules, and documentation sync requirements. Read HANDOFF.md first, align changes to docs/PRD/0001_기준_스펙/spec.md and relevant ADRs including ADR-0027 for minimal high-value tests, run the smallest valid Gradle test target, keep Testcontainers runs on --no-daemon, and update affected docs with the implementation.
---

# happyGallery Workflow

## Session bootstrap

- Read `HANDOFF.md` at repository root before changing code.
- Treat `HANDOFF.md` only as active transfer state. Use code, PRD, ADR, and API contract docs for durable facts.
- Use PRD-0001 for behavior/policy and PRD-0004 for HTTP request/response contracts.
- Read only the ADRs that touch the change. Common ones are under `docs/ADR/`.

## Respect module boundaries

- Put Spring Boot entrypoints and runtime config in `bootstrap/`, HTTP controllers and filters in `adapter-in-web/`, application services and ports in `application/`, persistence adapters in `adapter-out-persistence/`, external HTTP adapters in `adapter-out-external/`, and core rules in `domain/`.
- Put business entities and policies in `domain/`.
- Put JPA/MyBatis repositories in `adapter-out-persistence/` and external integrations in `adapter-out-external/`.
- Put shared domain exceptions and time utilities in `domain/`.
- Do not implement business rules directly in controllers.
- Do not move domain policy into adapter modules.

## Change workflow

1. Map the request to current code, PRD, API contract, and relevant ADR.
2. Search for the same pattern and change all occurrences that share the same reason.
3. Pick the narrowest owning module and layer.
4. For schema changes, check SQL and Java migrations before selecting the next version.
5. Keep secrets in environment variables.
6. Update only docs whose maintained behavior, contract, or durable decision changed.

## Verification workflow

- Choose the smallest valid Gradle command first.
- Use targeted `:application:test` or `:adapter-in-web:test` for ordinary unit, adapter, filter, and controller changes.
- Use `./gradlew :application:policyTest` for policy and domain-rule changes.
- Use `./gradlew --no-daemon :application:useCaseTest` for Spring context, Flyway, Testcontainers, DB, transaction, or external integration flows.
- Use `./gradlew --no-daemon :adapter-in-web:restDocsTest` when an exposed HTTP contract changes.
- Use `./gradlew test` or `./gradlew build` only for broader stability checks.
- Keep every modified test method annotated with `@DisplayName` using a Korean sentence.
- Follow ADR-0027: add only high-value use case, domain policy, or serialization tests; do not pad coverage with low-value framework or mapping tests.

Read `references/test-and-doc-matrix.md` when you need the exact test selection rules or document update checklist.
## Engineering judgment

- Search the implementation and analogous patterns with `rg` before choosing a design.
- Optimize for domain consistency and failure boundaries, then explicit flow, readability, and reuse.
- Validate once at the owning boundary: HTTP shape in DTOs, invariants in domain methods, and auth/ownership/cross-aggregate/server-confirmed money in application services.
- Prevent N+1 reads with a projection, fetch join, or bulk query. Build maps only when correlation, duplicate aggregation, or deduplication requires them.
- Use streams for pure transformations and explicit loops for mutation, branching, partial failure, or transaction effects.

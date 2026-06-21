---
name: happygallery-spring-backend
description: Repository-specific fallback workflow for general happyGallery backend work that spans multiple domains or does not fit a narrower happyGallery skill. Use when the request touches several modules at once, asks for a broad repository review, changes shared Gradle or module structure, updates common config, or needs the repository-wide layering rules, Flyway conventions, test-selection rules, and documentation sync requirements. Read HANDOFF.md first, align changes to docs/PRD/0001_기준_스펙/spec.md and relevant ADRs including ADR-0027 for minimal high-value tests, run the smallest valid Gradle test target, keep Testcontainers runs on --no-daemon, and update affected docs with the implementation.
---

# happyGallery Workflow

## Session bootstrap

- Read `HANDOFF.md` at repository root before changing code.
- If `HANDOFF.md` disagrees with the implementation, update it immediately to match the code.
- Use `docs/PRD/0001_기준_스펙/spec.md` as the product source of truth for API contracts, state transitions, time boundaries, and policy behavior.
- Read only the ADRs that touch the change. Common ones are under `docs/ADR/`.

## Respect module boundaries

- Put Spring Boot entrypoints and runtime config in `bootstrap/`, HTTP controllers and filters in `adapter-in-web/`, application services and ports in `application/`, persistence adapters in `adapter-out-persistence/`, external HTTP adapters in `adapter-out-external/`, and core rules in `domain/`.
- Put business entities and policies in `domain/`.
- Put JPA/MyBatis repositories in `adapter-out-persistence/` and external integrations in `adapter-out-external/`.
- Put shared domain exceptions and time utilities in `domain/`.
- Do not implement business rules directly in controllers.
- Do not move domain policy into adapter modules.

## Change workflow

1. Map the request to the spec and, if needed, the matching ADR.
2. Pick the narrowest module and layer that can own the change.
3. If the schema changes, add a Flyway script under `bootstrap/src/main/resources/db/migration` with the existing `V{n}__description.sql` pattern.
4. Keep secrets in environment variables, never in tracked config.
5. After code changes, update the affected documents in the repository, not just the implementation.

## Verification workflow

- Choose the smallest valid Gradle command first.
- Use `./gradlew :application:policyTest` for policy and domain-rule changes.
- Use `./gradlew --no-daemon :application:useCaseTest` for Spring context, Flyway, Testcontainers, DB, transaction, or external integration flows.
- Use `./gradlew test` or `./gradlew build` only for broader stability checks.
- Keep every modified test method annotated with `@DisplayName` using a Korean sentence.
- Follow ADR-0027: add only high-value use case, domain policy, or serialization tests; do not pad coverage with low-value framework or mapping tests.

Read `references/test-and-doc-matrix.md` when you need the exact test selection rules or document update checklist.

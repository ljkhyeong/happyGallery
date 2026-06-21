---
name: happygallery-test-refactor
description: Repository-specific workflow for test-only refactors in the happyGallery backend. Use when reducing duplication, extracting fixtures/helpers, reorganizing support classes, or improving readability in the happyGallery test suite without changing behavior. Read HANDOFF.md first, preserve current behavior and assertions, follow ADR-0027 so low-value tests are not added just for coverage, keep every test method annotated with @DisplayName in Korean, choose the smallest valid Gradle verification command, keep Testcontainers runs on --no-daemon, and update HANDOFF.md or related docs when the testing strategy or remaining work changes.
---

# happyGallery Test Refactor

## Scope rules

- Treat this skill as refactoring only unless the user explicitly asks for behavior changes.
- Prefer extracting shared fixtures, builders, and assertion helpers over rewriting scenarios.
- Keep tests readable at the scenario level; do not hide critical business rules inside opaque helpers.
- Preserve `@DisplayName` on every test method and write it as a Korean sentence.
- Keep ADR-0027 in force: preserve high-value use case, policy, and serialization coverage, but do not add low-value tests or helper indirection just to increase test count.
- Use `@ParameterizedTest` only for the same rule with multiple inputs such as boundary values; keep distinct business scenarios as separate `@Test` methods with Korean `@DisplayName`.

## Main targets in this repository

- `application/src/test/java/com/personal/happygallery/application/order/`
- `application/src/test/java/com/personal/happygallery/application/booking/`
- `application/src/test/java/com/personal/happygallery/application/pass/`
- `application/src/testFixtures/java/com/personal/happygallery/support/`

## Verification workflow

- Small policy-test-only cleanup: `./gradlew :application:policyTest`
- Integration-test helper or fixture refactor: `./gradlew --no-daemon :application:useCaseTest`
- Only use `./gradlew test` or `./gradlew build` when the refactor spans many areas.

Read `references/test-refactor-checklist.md` for duplication heuristics and the current repo-specific refactor target from HANDOFF.

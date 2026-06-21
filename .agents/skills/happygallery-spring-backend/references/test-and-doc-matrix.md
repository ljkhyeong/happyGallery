# Test And Documentation Matrix

## Test selection

Use the smallest command that covers the change.

- Policy or domain rule changes: `./gradlew :application:policyTest`
- Use case flow changes, DB changes, transaction boundaries, Flyway, Testcontainers, or integration behavior: `./gradlew --no-daemon :application:useCaseTest`
- Whole-project confidence checks or broad refactors: `./gradlew test` or `./gradlew build`
- Local API run: `./gradlew :bootstrap:bootRun`
- Local dependencies: `docker compose up -d`

## Test writing rules

- Follow `docs/ADR/0027_테스트_전략과_최소_테스트_세트_기준선/adr.md`.
- Keep the absolute quantity of tests small; every added test should justify its long-term maintenance cost.
- Prefer one of three buckets: high-value `@UseCaseIT` flow tests, `policyTest` domain-rule tests, or serialization/contract tests for externally visible payloads.
- Do not add tests just to raise coverage numbers.
- Use JUnit 5.
- Add `@DisplayName` to every test method.
- Write display names as Korean sentences.
- Put policy tests under the `policy` tag and use case integration tests under `@UseCaseIT` / `usecase`.

## Documentation update checklist

Check and update the affected documents whenever implementation changes.

- Product behavior, API contract, state values, time boundaries: `docs/PRD/0001_기준_스펙/spec.md`
- Detailed request and response contracts: `docs/PRD/0004_API_계약/spec.md`
- Technical or product design decisions: matching file under `docs/ADR/`
- Large but not-yet-adopted design or migration notes: matching file under `docs/Idea/`
- Active execution tracking and small cleanup ideas: `plan.md`, `simple-idea.md`
- Session state, current branch, recent decisions, verification status, remaining work: `HANDOFF.md`

## Repository constraints

- Keep package structure under `com.personal.happygallery.<layer>.<feature>`.
- Use Java 21 and Gradle toolchains already configured by the repo.
- Keep DB schema changes in Flyway migrations only.
- Inject secrets such as `ADMIN_API_KEY` and DB credentials from environment variables.

# Test Refactor Checklist

## Duplication heuristics

- Extract repeated entity creation into fixture methods or support builders.
- Extract repeated cleanup logic into shared support utilities.
- Keep one test focused on one business rule or scenario.
- Avoid helpers that hide essential assertions or state transitions.
- If a helper couples many unrelated domains, split it by booking, order, pass, product, or admin concern.

## Current repository-specific guidance

- Use `HANDOFF.md` to find the latest unfinished testing work before refactoring shared fixtures.
- Follow `docs/ADR/0027_테스트_전략과_최소_테스트_세트_기준선/adr.md`: reduce long-term maintenance cost, keep test quantity minimal, and prefer preserving only high-value use case, policy, and serialization checks.
- If `HANDOFF.md` no longer calls out a specific hotspot, inspect recent duplication in:
  - `application/src/test/java/com/personal/happygallery/application/order/`
  - `application/src/test/java/com/personal/happygallery/application/booking/`
  - `application/src/test/java/com/personal/happygallery/application/pass/`
  - `application/src/testFixtures/java/com/personal/happygallery/support/`
- For shared integration fixture refactors, the minimum validation command is usually `./gradlew --no-daemon :application:useCaseTest`.

## Doc sync checklist

- Update `HANDOFF.md` if the refactor changes progress, verification status, or remaining work.
- If the refactor exposes a broader follow-up, record it in `simple-idea.md` for small cleanup or `docs/Idea/` for a larger design note.

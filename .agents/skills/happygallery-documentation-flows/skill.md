---
name: happygallery-documentation-flows
description: Repository-specific workflow for documentation-only work in the happyGallery repo. Use when the request primarily updates README, HANDOFF, PRD, ADR, Idea, Retrospective, plan.md, simple-idea.md, or documentation structure/governance without requiring production code changes. Read HANDOFF.md first, keep docs aligned with the current implementation and repository document rules, promote implemented decisions into ADRs when appropriate, and avoid leaving stale references behind.
---

# happyGallery Documentation Flows

## Session bootstrap

- Read `HANDOFF.md` first.
- Use `README.md` as the top-level document index and project summary.
- Use `docs/PRD/0001_기준_스펙/spec.md` as the main product source of truth.
- Use `docs/PRD/0004_API_계약/spec.md` when API contracts are involved.
- Treat PRD 0002 as background transition material and PRD 0003 as explicitly out of scope unless reclassified.
- Read only the ADRs, Idea notes, retrospectives, or plans that the requested documentation change touches.

## Scope and ownership

- This skill owns documentation-only work under the repository root and `docs/`.
- Typical targets are `README.md`, `HANDOFF.md`, `docs/ADR/`, `docs/Idea/`, `docs/PRD/`, `docs/Retrospective/`, `plan.md`, and `simple-idea.md`.
- If the request requires implementation changes to make the documents true, switch to the matching happyGallery product or backend/frontend skill instead of papering over the drift.

## Documentation rules for this repository

- Treat implemented and long-lived architectural decisions as ADR material.
- Do not create an ADR for every implementation change; require a durable architecture, contract, ownership, failure-boundary, or trade-off decision.
- Keep `Idea` documents for exploration, alternatives, or not-yet-adopted directions.
- Keep `simple-idea.md` for small cleanup or refactor notes only.
- Keep active execution tracking in `plan.md`, not in `docs/1Pager/`.
- Remove or update stale references when the document structure changes.
- When README claims a document as the source of truth, make sure that document actually contains the decision.
- Write documentation with concrete, reader-friendly wording. Prefer user-visible or implementation-shaped terms over abstract or internal jargon, and make the current state and before/after changes easy to scan in short sentences.
- Keep volatile route lists, test counts, branches, and runtime status in executable sources or live tools.
- Keep `HANDOFF.md` limited to active work, remaining actions, next entry files/skills, and session-only decisions.

## Change workflow

1. Identify the canonical document for the topic before editing secondary summaries.
2. Update the source-of-truth document first, then sync only affected summaries or cross-links.
3. When a previously explored Idea is now implemented, reflect the adopted decision in ADR and mark the Idea as background-only if needed.
4. Keep wording concise and implementation-matching; do not preserve stale historical phrasing just because it already exists.
5. After edits, scan for broken references, outdated counts, and terminology drift.

## Verification workflow

- Documentation-only updates: verify with targeted `rg`, `sed`, or file reads.
- If the docs describe code paths, compare them against the current implementation before finishing.
- Run tests only when the documentation work is coupled to code changes.

## References

- Read `references/doc-map.md` for the current document roles, promotion rules, and sync checklist.

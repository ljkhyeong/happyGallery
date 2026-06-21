# Documentation Map

## Core docs

- `README.md`
  Top-level project summary, setup, architecture overview, library summary, and document index.
- `HANDOFF.md`
  Current branch context, recent implementation status, verification notes, and remaining work.
- `docs/PRD/0001_기준_스펙/spec.md`
  Main product and behavior spec.
- `docs/PRD/0004_API_계약/spec.md`
  API contract detail.

## Decision and note categories

- `docs/ADR/`
  Adopted architectural and technical decisions that should remain true over time.
- `docs/Idea/`
  Exploration, alternatives, migration considerations, or larger not-yet-adopted notes.
- `simple-idea.md`
  Small cleanup, refactor, and maintenance ideas only.
- `plan.md`
  Active execution plan for the current stream of work.
- `docs/Retrospective/`
  Lessons learned after implementation.
- `docs/POC/`
  Proof-of-concept or experimental implementation records when they need long-term retention.

## Promotion rules

- If a change is implemented and becomes a maintained architecture rule, update or add an ADR.
- If a note is still evaluating options or preserves the decision background, keep it in `docs/Idea/`.
- If a note is a one-line cleanup idea, keep it in `simple-idea.md` instead of creating a large doc.
- If README points to ADR as the detailed source, verify the detailed content exists in ADR before closing the task.

## Doc sync checklist

- README summary and indexes
- HANDOFF current status and recent work bullets
- PRD or API contract when behavior or contract text changed
- ADR or Idea links and numbering when documents are added, promoted, or deprecated
- `plan.md` and `simple-idea.md` when the request changes active work tracking or small follow-up items

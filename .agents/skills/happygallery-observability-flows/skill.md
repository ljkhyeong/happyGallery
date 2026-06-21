---
name: happygallery-observability-flows
description: Repository-specific workflow for observability, runtime monitoring, request tracing, metrics, dashboards, alert rules, Sentry wiring, and client monitoring changes in the happyGallery repo. Use when the request mentions observability, monitoring, requestId, Prometheus, Grafana, Alertmanager, Sentry, actuator, metrics, dashboard, alert rule, client-monitoring, funnel metric, or telemetry in the happyGallery repo. Read HANDOFF.md first, align changes with observability ADRs and monitoring docs, preserve requestId tracing and metric contracts, run the smallest valid verification, and update affected docs.
---

# happyGallery Observability Flows

## Session bootstrap

- Read `HANDOFF.md` first.
- Use `README.md` for the current monitoring stack and runtime setup overview.
- Use `docs/PRD/0001_기준_스펙/spec.md` and `docs/PRD/0004_API_계약/spec.md` when telemetry affects public error or API contracts.
- Read the needed ADRs:
  - `docs/ADR/0015_Observability_로깅과_비즈니스_예외/adr.md`
  - `docs/ADR/0023_관리자_회원_인증_세션_기준선/adr.md`
  - `docs/ADR/0025_정상_종료와_Executor_정리_정책/adr.md`

## Scope and ownership

- This skill owns observability changes across:
  - `monitoring/`
  - `docker-compose.yml`
  - backend request tracing, metrics, `RequestIdFilter`, client monitoring, and Sentry wiring
  - frontend Sentry and telemetry hooks when the change is primarily observability-driven
- If the main request is a product/domain rule change that incidentally emits telemetry, use the domain skill first and apply this skill only for the observability slice.

## Non-negotiable invariants

- Preserve `requestId` propagation through logs, error responses, and batch executions.
- Keep `[client-monitoring]` event names and `happygallery.funnel.*` metric contracts aligned with the docs.
- Do not silently break `/actuator/prometheus`, dashboard JSON expectations, or alert rule labels.
- Preserve backend/frontend Sentry tagging patterns where `requestId` can be attached.
- Treat observability config as long-lived operational contract: update README, HANDOFF, ADR, or Idea notes together when behavior changes.

## Verification workflow

- Backend-only tracing or metric code changes: `./gradlew --no-daemon :application:useCaseTest --tests "*Monitoring*" --tests "*RequestId*"`
- Monitoring config or compose wiring changes: `docker compose config`
- Frontend-only telemetry or Sentry wiring changes: `cd frontend && npm run build`
- Broad observability confidence touching backend and frontend: combine the smallest relevant backend test with `cd frontend && npm run build`

## References

- Read `references/observability-map.md` for the main files, contracts, and doc sync checklist.

# Observability Map

## Likely code locations

- `monitoring/`
- `docker-compose.yml`
- `application/src/main/java/com/personal/happygallery/application/monitoring/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/RequestIdFilter.java`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/monitoring/`
- `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/GlobalExceptionHandler.java`
- `frontend/src/features/monitoring/`
- `frontend/src/shared/api/` and Sentry bootstrap files when frontend telemetry is involved

## High-value verification targets

- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/RequestIdFilterUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/monitoring/ClientMonitoringUseCaseIT.java`
- `adapter-in-web/src/test/java/com/personal/happygallery/adapter/in/web/GlobalExceptionHandlerTest.java`

## Doc sync checklist

- Runtime tracing, requestId, error response policy: `docs/ADR/0015_Observability_로깅과_비즈니스_예외/adr.md`
- Runtime operations and requestId baseline: `docs/ADR/0023_관리자_회원_인증_세션_기준선/adr.md`
- Shutdown and async drain behavior affecting observability: `docs/ADR/0025_정상_종료와_Executor_정리_정책/adr.md`
- Monitoring stack overview and local run notes: `README.md`, `HANDOFF.md`
- Client-monitoring event scope or telemetry rollouts: `docs/PRD/0002_회원_스토어_전환/spec.md` and API contract details in `docs/PRD/0004_API_계약/spec.md`

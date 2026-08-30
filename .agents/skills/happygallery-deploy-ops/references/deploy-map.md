# Deploy Map

## Source-of-truth files

- Target topology and invariants: `docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md`
- Current state and local commands: `README.md`
- Local integration stack: `docker-compose.yml`, `nginx/nginx.conf`
- Container builds: `Dockerfile`, `Dockerfile.deploy`
- Application runtime configuration: `bootstrap/src/main/resources/application.yml`, `bootstrap/src/main/resources/application-prod.yml`
- Graceful shutdown and HTTP limits: ADR-0025 and ADR-0030
- Forwarded-header and rate-limit behavior: ADR-0017 and ADR-0028
- Protected data and key requirements: ADR-0036

AWS Idea-0028, Idea-0029 and Idea-0039 are historical records. There is no current production deploy workflow or Kubernetes manifest. Recheck with `rg --files` before making deployment assumptions.

## Target topology

```text
client
  -> DNS / router / firewall
  -> single-node k3s Ingress (TLS)
       -> frontend static content and SPA fallback
       -> /api/* -> Spring Boot Service -> internal MySQL and Redis Services
```

Docker Compose remains a development and recovery-diagnosis tool. Its `local` profile, default credentials, and host-published MySQL/Redis/app ports are not a production baseline.

## Kubernetes artifacts still required

- Namespace and resource naming convention
- Frontend, app, MySQL, and Redis Deployments or StatefulSets and Services
- Ingress, certificate issuer/certificate, DNS and router/firewall procedure
- PVC/storage class selection and disk-capacity alerts
- Secret creation, rotation, recovery-key storage, and access restrictions
- Immutable image build, local registry or k3s import, rollout, and rollback procedure
- MySQL backup schedule, off-device retention, integrity check, and restore drill
- Startup/readiness/liveness probes and termination grace
- Monitoring access and alert delivery without publishing management ports

## Runtime environment variables to remember

- Profile and network: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`
- Protected data: `ENCRYPT_KEY`, `HMAC_KEY`; preserve recoverable copies outside the laptop
- Admin bootstrap: `ADMIN_SETUP_TOKEN` only during first setup
- Payment: `TOSS_SECRET_KEY`, `VITE_TOSS_CLIENT_KEY`, `PASS_TOTAL_PRICE`, and optional `TOSS_*` HTTP tuning
- OAuth and notifications: Google/Naver, Kakao/SMS credentials and sender values
- Observability: Sentry, Grafana and alert delivery values
- Rate limiting: keep forwarded-header trust aligned with the controlled ingress chain

Do not place production values in Git, images, Compose defaults, or plain Kubernetes manifests. Kubernetes Secret data is only encoded unless an additional encryption mechanism is configured.

## Pre-deployment checklist

1. Confirm the laptop architecture, free disk, memory, power and network state.
2. Confirm the exact Git commit, immutable image tag/digest, image delivery method, and previous rollback image.
3. Validate manifests and confirm no secret values are committed or rendered into logs.
4. Confirm a recent off-device backup, backup integrity, and recovery access to encryption/HMAC keys.
5. Confirm node readiness, PVC binding, probes, termination grace, Services, Ingress and certificate state.
6. Confirm only ingress HTTP/HTTPS is externally reachable and the app/data/management ports are private.
7. Apply the rollout, wait for readiness, inspect events/logs, then smoke test SPA routes and API JSON responses.
8. Verify the real client IP and HTTPS scheme through ingress, session cookies, CSRF, rate limits, monitoring and rollback readiness.

## Common troubleshooting order

1. Check host power, disk, memory, network and k3s service/node health.
2. Check Deployment/StatefulSet rollout status, pod events, probe failures and application logs.
3. Check whether the expected image digest reached k3s and whether the pod architecture matches the laptop.
4. For ingress failures, separate DNS/router/firewall, certificate, Ingress rule, Service endpoint and pod-readiness problems.
5. If API returns HTML, fix SPA fallback so `/api/*` errors remain JSON.
6. For incorrect client IP or redirect scheme, inspect each trusted proxy hop and ingress header overwrite behavior before changing the app.
7. For MySQL/Redis failures, inspect Service endpoints, credentials, MySQL PVC state and disk capacity. Redis recreation invalidates sessions and resets rate-limit counters.
8. If a rollout fails after Flyway starts, evaluate schema compatibility and backup restore before application rollback.

## Doc sync checklist

- Durable topology or operational invariant: ADR-0037, or a new ADR when the decision changes
- Current implementation and operator entrypoint: `README.md`
- Reverse proxy and forwarded-header contract: ADR-0017 and ADR-0028
- Active unfinished work only: `HANDOFF.md`; do not duplicate durable deployment guidance there
- Historical AWS context: preserve Idea-0028, Idea-0029, and Idea-0039 as history rather than rewriting them

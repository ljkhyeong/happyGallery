---
name: happygallery-deploy-ops
description: Repository-specific workflow for happyGallery self-hosted deployment and operations. Use when the request mentions a local laptop server, Docker Compose, k3s, Kubernetes, Ingress, TLS, DNS, router or firewall exposure, PVC or local storage, MySQL/Redis persistence, backup and restore, Kubernetes Secret, local registry or image import, rollout and rollback, health probes, forwarded headers, or deployment troubleshooting. Read HANDOFF.md and ADR-0037 first, distinguish the k3s production target from the current development-only Compose setup, and update deployment docs when infrastructure behavior changes.
---

# happyGallery Deploy Ops

## Core references

- Read `HANDOFF.md` first for active work only. Do not use it as the durable deployment contract.
- Read `docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md` for the target topology and invariants.
- Use `README.md` for the high-level current state and local commands.
- Read `docs/ADR/0028_배포_준비_알림_연동_로그_마스킹/adr.md` for the reverse-proxy baseline.
- Read ADR-0017 for rate-limit proxy trust, ADR-0025 for graceful shutdown, ADR-0030 for HTTP timeout, and ADR-0036 for secret and HMAC requirements.
- Treat AWS Idea-0028, Idea-0029, and Idea-0039 as historical records, not current instructions.

## Target and current state

- Target production runtime: single-node k3s on one owned laptop.
- Target entrypoint: TLS Kubernetes Ingress serving the React SPA and routing `/api/*` to Spring Boot on the same origin.
- Target stateful services: cluster-internal MySQL with a persistent volume and off-device backup; Redis persistence is optional because its session and rate-limit state is recreatable.
- Current implementation: development Docker Compose with Nginx, `local` Spring profile, development defaults, and host-published data ports.
- Missing today: Kubernetes manifests, production ingress/TLS, PVCs, secret injection, image delivery, rollout/rollback, backup/restore, and a public production URL.
- There is no current automatic production deployment workflow.

## Non-negotiable invariants

- Do not describe the current Compose stack as production. It exposes development ports and runs the `local` profile.
- Expose only ingress HTTP/HTTPS ports. Keep the app, MySQL, Redis, Actuator, Prometheus, Alertmanager, and Grafana private unless an explicit access policy is documented.
- Make ingress overwrite or normalize forwarded headers. Enable forwarded-header trust only when direct app access is blocked behind the controlled ingress.
- Keep MySQL on a persistent volume. Store encrypted MySQL backups and required recovery keys outside the laptop, and verify restore procedures. Redis loss may invalidate sessions and reset rate-limit buckets, so do not add a Redis backup without a new durable-data requirement.
- Keep runtime secrets out of Git, images, plain manifests, and Compose defaults. Kubernetes Secret alone is not encryption; restrict host and cluster access.
- Deploy images by commit SHA or digest, retain the previous image and manifest, and document rollout and rollback. Do not rely on `latest` alone.
- Check Flyway compatibility before rollback because image rollback does not roll back the database.
- Define startup/readiness/liveness probes and keep Kubernetes termination grace at least 30 seconds.
- Preserve same-origin frontend/API routing and ensure SPA fallback never rewrites API errors to `index.html`.
- State the single-node availability limitation plainly; a laptop, disk, power, network, or k3s failure can stop the whole service.

## Verification workflow

- Start by checking whether Kubernetes manifests actually exist; do not infer implementation from ADR-0037.
- For Compose changes, run `docker compose config` and only use the stack for local integration or recovery diagnosis.
- For image changes, build the exact image, record its SHA/digest, verify architecture compatibility with the laptop, and confirm how k3s receives it.
- For Kubernetes changes, inspect node readiness, events, pod status/logs, PVC binding, Services, Ingress, certificate state, probes, and rollout history.
- Verify frontend routes and `/api/*` separately so SPA fallback cannot hide API failures.
- Verify direct access to app/MySQL/Redis is blocked and test client IP, HTTPS scheme, cookies, CSRF, and rate-limit behavior through the real ingress chain.
- Before stateful changes, verify a recent off-device backup and the matching `ENCRYPT_KEY`/`HMAC_KEY` recovery path.
- After deployment, check health, key browser flows, monitoring, disk capacity, restart behavior, and the documented rollback command.

Read `references/deploy-map.md` for config surfaces and the troubleshooting checklist.

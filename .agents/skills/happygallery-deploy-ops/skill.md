---
name: happygallery-deploy-ops
description: Repository-specific workflow for happyGallery AWS deployment and operations. Use when the request mentions AWS, ECS, Fargate, ALB, target group, health check, CloudFront, S3, OAC, ECR, GitHub Actions OIDC, task definition, ARM64 image build, RDS, ElastiCache/Valkey/Redis, security groups, production environment variables, or deployment troubleshooting. Read HANDOFF.md first, preserve CloudFront/S3/ALB/ECS routing assumptions, prefer AWS CLI verification when credentials are available, and update deployment docs when infrastructure behavior changes.
---

# happyGallery Deploy Ops

## Core references

- Read `HANDOFF.md` first for current branch, production URL, and known deployment state.
- Use `README.md` for the high-level production architecture.
- Use deployment notes:
  - `docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md`
  - `docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md`
  - `docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md`
- Use `.github/workflows/deploy.yml` for the actual CI/CD behavior.

## Current production shape

- Browser entrypoint: CloudFront.
- Static frontend: S3 private bucket via OAC.
- API path: CloudFront `/api/*` -> ALB -> ECS Fargate Spring Boot app.
- Runtime data: RDS MySQL and ElastiCache Redis/Valkey in private networking.
- Backend image: ECR, built as `linux/arm64`.
- GitHub Actions deploy: OIDC role, ECR push, task definition render/register, ECS service deploy, CloudFront invalidation for frontend.

## Non-negotiable invariants

- Keep default CloudFront behavior on S3 and `/api/*` behavior on ALB.
- Do not re-enable broad public S3 access; use OAC.
- Do not rely on `latest` alone for backend deployment when task definition revision can pin a SHA image.
- Keep ECS app tasks in private subnets unless the architecture document changes.
- ALB should health check `/actuator/health` on the application port configured for ECS.
- RDS inbound should be limited to the ECS app security group or explicitly temporary diagnostic sources.
- Redis/Valkey inbound should be limited to the ECS app security group.
- Runtime secrets belong in ECS/SSM/Secrets Manager or environment injection, not GitHub repo files.
- When CloudFront SPA fallback changes, do not mask API JSON errors with global `403/404 -> index.html` behavior.

## Verification workflow

- GitHub Actions failures: use `gh run view`, `gh pr checks`, or the GitHub app where available.
- ECS health issues: check ECS service events, current task definition revision, stopped task reason, target group health reason, and CloudWatch app logs.
- CloudFront/S3 routing issues: verify origins, behaviors, default root object, OAC bucket policy, and cache invalidation state.
- Backend deployment changes: verify ARM64 image build, ECR pushed tag/digest, registered task definition image URI, ECS service stability, and target group health.
- Frontend deployment changes: `cd frontend && npm run build`, S3 sync target, CloudFront invalidation, and production URL smoke test.

Read `references/deploy-map.md` for resource names, config surfaces, and troubleshooting checklist.

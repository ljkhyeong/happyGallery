# Deploy Map

## Main files

- `.github/workflows/deploy.yml`
- `Dockerfile.deploy`
- `bootstrap/build.gradle`
- `frontend/package.json`
- `README.md`
- `HANDOFF.md`
- `docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md`
- `docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md`
- `docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md`

## GitHub Actions variables and secrets

- Secrets: `AWS_ROLE_TO_ASSUME`, `VITE_SENTRY_DSN`, `VITE_TOSS_CLIENT_KEY`
- Variables: `ECR_REPOSITORY`, `ECS_CLUSTER`, `ECS_SERVICE`, `S3_BUCKET`, `CLOUDFRONT_DISTRIBUTION_ID`
- Frontend build env is injected from GitHub Actions secrets for Sentry and Toss.

## Runtime environment variables to remember

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Redis/Valkey: `REDIS_HOST`, `REDIS_PORT`, `SPRING_DATA_REDIS_SSL_ENABLED=true`
- App health: `MANAGEMENT_PORT=8080`
- Admin bootstrap: `ADMIN_SETUP_TOKEN` only during first setup
- Payment: `TOSS_SECRET_KEY`, `PASS_TOTAL_PRICE`, and optional `TOSS_*` HTTP tuning (`TOSS_BASE_URL`, `TOSS_TIMEOUT_MILLIS`, `TOSS_CONNECT_TIMEOUT_MILLIS`, `TOSS_ACQUIRE_TIMEOUT_MILLIS`, `TOSS_MAX_CONNECTIONS`, `TOSS_KEEP_ALIVE_MILLIS`)
- Notification: Kakao/SMS keys and sender values

## Common troubleshooting order

1. Check whether the new image/task definition actually reached ECS.
2. Check stopped task reason and application logs before changing ALB/CloudFront.
3. If target group says timeout, distinguish app startup failure from ALB-to-task connectivity and health endpoint latency.
4. If CloudFront returns 504, verify origin protocol policy and ALB listener ports.
5. If CloudFront returns S3 `AccessDenied`, verify S3 objects, default root object, OAC, and bucket policy.
6. If API returns HTML/index instead of JSON, remove global error masking and use API-excluding SPA rewrite behavior.
7. For DB/Redis timeout, verify actual task ENI security group, target service security group, VPC/subnet, and NACLs.

## Doc sync checklist

- Architecture or deployment flow: `README.md` and `docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md`
- CloudFront/S3/ALB topology: `docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md`
- CI/CD workflow behavior: `docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md`
- Current production state and known issues: `HANDOFF.md`

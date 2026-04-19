# GitHub Actions 기반 배포 파이프라인 메모

**상태**: 운영 반영 완료

> 현재 CI는 `pull_request -> main`, 배포는 `push -> main` 기준으로 동작한다. 프론트엔드는 S3 + CloudFront, 백엔드는 ECR + ECS Fargate로 배포한다.

## 현재 흐름

```text
pull_request -> main
  -> CI
       -> backend: ./gradlew build
       -> frontend: npm ci && npm run build

push -> main
  -> Deploy
       -> frontend: S3 sync -> CloudFront invalidation
       -> backend: :bootstrap:bootJar -> Docker build/push -> ECS update-service
```

## CI

### 백엔드

- JDK 21
- `./gradlew build`
- 실패 시 테스트 리포트 업로드

### 프론트엔드

- Node 22
- `frontend`에서 `npm ci`
- `npm run build`

CI와 배포를 분리해 PR 단계에서는 AWS 자격증명 없이 검증만 수행한다.

## 배포

### 프론트엔드

1. `frontend`에서 `npm ci && npm run build`
2. `aws s3 sync frontend/dist/ s3://$S3_BUCKET --delete`
3. `aws cloudfront create-invalidation --distribution-id $CLOUDFRONT_DISTRIBUTION_ID --paths "/*"`

### 백엔드

1. `./gradlew :bootstrap:bootJar -x test`
2. `Dockerfile.deploy`로 이미지 빌드
3. ECR에 `latest`, `${github.sha}` 태그로 push
4. `aws ecs update-service --force-new-deployment`

백엔드는 ECS 롤링 배포 방식으로 교체된다.

## 필요한 AWS 리소스

- ECR 리포지토리
- ECS 클러스터와 Fargate 서비스
- ALB
- S3 버킷
- CloudFront 배포
- RDS MySQL
- Redis
- GitHub Actions OIDC용 IAM 역할

## GitHub Secrets와 Variables

- `AWS_ROLE_TO_ASSUME`
- `ECR_REPOSITORY`
- `ECS_CLUSTER`
- `ECS_SERVICE`
- `S3_BUCKET`
- `CLOUDFRONT_DISTRIBUTION_ID`

## 운영 메모

- AWS 인증은 Access Key보다 OIDC를 우선 사용한다.
- ECS 헬스 체크는 `/actuator/health`를 기준으로 둔다.
- Flyway는 앱 시작 시 실행되므로 배포 설정에서 동시에 너무 많은 태스크가 뜨지 않게 본다.

## 관련 문서

- `docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md`
- `docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md`

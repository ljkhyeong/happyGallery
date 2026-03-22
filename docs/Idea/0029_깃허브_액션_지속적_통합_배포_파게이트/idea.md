# Idea 0029: GitHub Actions CI/CD — ECS Fargate 배포 파이프라인

## 배경

현재 CI 파이프라인(`ci.yml`)은 PR 대상으로 Gradle 빌드만 수행한다.
운영 배포 토폴로지는 `0028_클라우드프론트_에스쓰리_로드밸런서_배포_구조`에서 CloudFront + S3 + ALB 구성으로 방향을 잡았다.
백엔드 런타임은 EC2 대신 **ECS Fargate**를 사용해 컨테이너 관리 부담을 줄이기로 한다.

이 Idea에서는 GitHub Actions 기반 CI/CD 파이프라인의 전체 설계를 정리한다.

## 전체 흐름

```
PR → main
  ├── CI: backend build & test + frontend build
  └── (merge 시) CD:
        ├── Frontend: npm build → S3 sync → CloudFront invalidation
        └── Backend:  bootJar → Docker build → ECR push → ECS service update
```

## 워크플로우 분리

| 파일 | 트리거 | 역할 |
|------|--------|------|
| `ci.yml` | `pull_request → main` | 빌드·테스트 검증 (AWS 자격증명 불필요) |
| `deploy.yml` | `push → main` | 프론트 S3 배포 + 백엔드 ECS 배포 |

CI와 CD를 분리하면:

- PR 단계에서는 시크릿 노출 없이 검증만 수행한다.
- main 머지 후에만 AWS 자격증명을 사용한다.
- 각 워크플로우의 실행 시간과 실패 원인을 독립적으로 추적할 수 있다.

## CI 상세 (`ci.yml`)

### backend job

- JDK 21 (Temurin) + Gradle 캐싱 (`gradle/actions/setup-gradle@v4`)
- `./gradlew build` — 컴파일 + 전체 테스트
- 실패 시 테스트 리포트 아티팩트 업로드

### frontend job

- Node 22 + npm 캐싱
- `npm ci && npm run build` — TypeScript 체크 + Vite 번들

두 job은 의존관계가 없으므로 병렬 실행한다.

### concurrency

- `group: ci-${{ github.head_ref }}`, `cancel-in-progress: true`
- 같은 PR에서 새 커밋이 푸시되면 이전 CI를 취소해 러너 낭비를 줄인다.

## CD 상세 (`deploy.yml`)

### 공통

- 트리거: `push → main` (PR 머지 시 자동)
- AWS 인증: `aws-actions/configure-aws-credentials@v4` + OIDC (GitHub → AWS IAM Role)
- 리전: `ap-northeast-2`

### frontend job

1. `npm ci && npm run build`
2. `aws s3 sync frontend/dist/ s3://$BUCKET --delete`
3. `aws cloudfront create-invalidation --distribution-id $DIST_ID --paths "/*"`

`--delete` 옵션으로 이전 빌드 잔여 파일을 제거한다.
CloudFront 무효화는 전체 경로(`/*`)로 수행한다.
Vite 빌드는 파일명에 content hash를 포함하므로 캐시 히트/미스가 자연스럽게 관리된다.

### backend job

1. `./gradlew :app:bootJar -x test` — 테스트는 CI에서 이미 통과
2. Docker 빌드 + ECR 푸시 (태그: `latest` + git SHA)
3. ECS 서비스 강제 새 배포: `aws ecs update-service --force-new-deployment`

ECS가 새 태스크를 띄우고 기존 태스크를 드레인하는 **롤링 배포** 방식이다.
별도 task definition JSON 관리 없이, ECR 이미지 태그를 `latest`로 유지하고 서비스만 갱신한다.

### 변경 감지 (선택)

향후 `paths` 필터를 추가해 프론트만 변경되었을 때 백엔드 배포를 건너뛸 수 있다.
현재는 단순성을 위해 main 머지 시 양쪽 모두 배포한다.

## 필요한 AWS 리소스 (사전 준비)

| 리소스 | 용도 |
|--------|------|
| ECR 리포지토리 | Docker 이미지 저장소 |
| ECS 클러스터 + Fargate 서비스 | 백엔드 컨테이너 실행 |
| ECS Task Definition | 컨테이너 스펙 (CPU, 메모리, 환경변수, 포트) |
| ALB + Target Group | ECS 서비스 앞단 로드밸런서 |
| S3 버킷 | 프론트 정적 파일 호스팅 |
| CloudFront 배포 | CDN + S3/ALB 라우팅 |
| RDS (MySQL 8) | 데이터베이스 |
| IAM Role (OIDC) | GitHub Actions → AWS 인증 |

## GitHub Secrets / Variables

| 이름 | 용도 |
|------|------|
| `AWS_ROLE_TO_ASSUME` | OIDC 기반 IAM Role ARN |
| `AWS_ACCOUNT_ID` | ECR 주소 구성 |
| `ECR_REPOSITORY` | ECR 리포 이름 |
| `ECS_CLUSTER` | ECS 클러스터 이름 |
| `ECS_SERVICE` | ECS 서비스 이름 |
| `S3_BUCKET` | 프론트 배포 버킷 |
| `CLOUDFRONT_DISTRIBUTION_ID` | 캐시 무효화 대상 |

## OIDC vs Access Key

GitHub Actions에서 AWS에 접근할 때 OIDC를 권장한다:

- 장기 Access Key를 시크릿에 저장하지 않아 유출 위험이 없다.
- IAM Role의 trust policy로 특정 repo/branch만 허용할 수 있다.
- AWS에서도 공식 권장하는 방식이다.

## Dockerfile 최적화

현재 Dockerfile은 멀티스테이지로 잘 구성되어 있다.
CD 파이프라인에서는 `bootJar`를 GitHub Actions 러너에서 먼저 빌드하고, 런타임 이미지만 만드는 방식도 가능하다.
이렇게 하면 Docker 빌드 시 Gradle 다운로드/컴파일을 건너뛰어 이미지 빌드가 빨라진다.

```dockerfile
# deploy용 경량 Dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 주의할 점

### 1. ECS 환경변수 관리

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_API_KEY` 등 민감 정보는 ECS Task Definition의 환경변수 또는 AWS Secrets Manager/SSM Parameter Store에서 주입한다.
GitHub Actions 시크릿에 DB 비밀번호를 넣는 것은 불필요하다.

### 2. health check

ECS 서비스의 롤링 배포가 정상 동작하려면 ALB Target Group의 health check가 정확해야 한다.
Spring Boot Actuator의 `/actuator/health` 엔드포인트를 사용한다.

### 3. Flyway 마이그레이션

ECS 태스크가 시작될 때 Flyway가 자동으로 마이그레이션을 실행한다.
여러 태스크가 동시에 뜨면 Flyway 락이 경합할 수 있으므로, 배포 시 `minimumHealthyPercent`를 적절히 설정한다.

## 재검토 조건

- ECS 대신 다른 런타임(EKS, Lambda 등)으로 전환할 때
- 스테이징 환경이 추가될 때 (환경별 deploy workflow 분기 필요)
- GitHub Environments + 수동 승인 게이트가 필요할 때

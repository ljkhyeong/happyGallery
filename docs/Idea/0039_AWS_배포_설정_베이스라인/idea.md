# Idea 0039: AWS 배포 설정 베이스라인

## 배경

운영 배포를 준비하면서 ECR, ECS, S3, CloudFront, GitHub Actions OIDC 설정이 대화와 개별 문서에 흩어지기 시작했다.

이 문서는 현재 확정한 AWS 배포 설정을 한곳에 모아 두는 기준 문서다.
앞으로 운영 배포 설정을 추가할 때도 이 문서에 같은 형식으로 누적한다.

## 현재 범위

- AWS 리소스 생성 권장 순서
- ECR 리포지토리 기본값
- ECR lifecycle policy
- S3 버킷 기본값
- CloudFront 기본값
- VPC / subnet 기본값
- RDS MySQL 기본값
- Redis 기본값
- GitHub Actions가 사용하는 Secrets / Variables
- GitHub Actions OIDC IAM Role trust policy
- GitHub Actions OIDC IAM Role permission policy

## 1. AWS 리소스 생성 권장 순서

현재 기준 권장 순서는 아래와 같다.

1. ECR 생성
2. S3 버킷 생성
3. CloudFront 생성
4. VPC / subnet 생성
5. RDS MySQL 생성
6. Redis 생성
7. ECS cluster / service / ALB 생성
8. GitHub Actions OIDC IAM Role permission policy 완성
9. GitHub Secrets / Variables 입력

### 메모

- OIDC provider와 IAM Role의 trust policy는 미리 만들어도 된다.
- 하지만 permission policy 안의 `S3_BUCKET`, `CLOUDFRONT_DISTRIBUTION_ID`, `ECS_CLUSTER`, `ECS_SERVICE`는 실제 리소스가 만들어진 뒤에만 확정할 수 있다.
- 그래서 OIDC role은 보통 `trust policy 먼저`, `permission policy는 리소스 생성 후 최종 반영` 순서로 진행한다.

## 2. ECR 리포지토리 기본값

현재 GitHub Actions 배포 워크플로우는 `ap-northeast-2` 리전에서 backend 이미지를 ECR로 push한다.

- 타입: Private repository
- 리전: `ap-northeast-2`
- 권장 이름: `happygallery-backend`
- tag immutability: `MUTABLE`
- scan on push: `enabled`
- encryption: 기본 `AES-256`

이 프로젝트의 deploy workflow는 같은 이미지에 commit SHA 태그와 `latest` 태그를 함께 push한다.
그래서 `latest`를 계속 갱신할 수 있도록 tag immutability는 `MUTABLE`을 사용한다.

## 3. ECR lifecycle policy 기준선

### 현재 기준

- `untagged` 이미지는 `1일` 후 삭제
- 태그가 있는 이미지는 최근 `30개`만 유지

### 이유

- 현재 deploy workflow는 `latest`와 `${github.sha}` 태그를 함께 push한다.
- 배포 후 오래된 SHA 이미지는 롤백 창만 남기면 충분하다.
- untagged 이미지는 레이어 정리용 임시 찌꺼기일 가능성이 높으므로 빠르게 정리한다.

### Lifecycle policy JSON 예시

```json
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Expire untagged images after 1 day",
      "selection": {
        "tagStatus": "untagged",
        "countType": "sinceImagePushed",
        "countUnit": "days",
        "countNumber": 1
      },
      "action": {
        "type": "expire"
      }
    },
    {
      "rulePriority": 2,
      "description": "Keep only the most recent 30 images",
      "selection": {
        "tagStatus": "any",
        "countType": "imageCountMoreThan",
        "countNumber": 30
      },
      "action": {
        "type": "expire"
      }
    }
  ]
}
```

### CLI 적용 예시

```bash
aws ecr put-lifecycle-policy \
  --repository-name happygallery-backend \
  --lifecycle-policy-text file://lifecycle-policy.json \
  --region ap-northeast-2
```

## 4. S3 버킷 기본값

운영 프론트 정적 파일은 `S3 + CloudFront` 조합을 기준으로 한다.
상세 배포 토폴로지 방향은 `0028_CloudFront_S3_ALB_배포_구조` 문서를 따른다.

### 현재 기준

- 리전: `ap-northeast-2`
- 권장 bucket 이름 예시: `happygallery-frontend-prod`
- 용도: frontend `dist/` 정적 파일 저장
- public access: `모두 차단`
- static website hosting: `사용 안 함`
- versioning: `선택`
- encryption: `SSE-S3` 또는 기본 `AES-256`

### 이유

- 현재 운영 권장안은 `S3 website endpoint`가 아니라 `CloudFront + OAC`로 private bucket을 origin으로 쓰는 방식이다.
- 그래서 bucket 자체를 public으로 열지 않는다.
- SPA fallback도 S3 website hosting보다 CloudFront 쪽 정책으로 관리하는 방향이 더 맞다.

### 생성할 때 체크할 항목

1. Bucket name을 전역에서 유일한 이름으로 만든다.
2. `Block all public access`를 유지한다.
3. 필요하면 versioning을 켠다.
4. bucket 생성 후 CloudFront origin으로 연결한다.
5. GitHub Actions variable에는 bucket 이름만 `S3_BUCKET`으로 넣는다.

### 지금 단계에서 아직 안 정해지는 값

- `CLOUDFRONT_DISTRIBUTION_ID`
- CloudFront OAC ID
- `/api/*` behavior routing
- custom domain / ACM certificate

## 5. CloudFront 기본값

운영 공개 진입점은 `CloudFront` 1개를 두고, path 기반으로 정적 파일과 API를 나누는 방식을 기준으로 한다.

### 현재 기준

- 기본 origin: private S3 bucket
- S3 접근 방식: `OAC (Origin Access Control)`
- 추가 origin: `ALB` (`/api/*` 용)
- viewer protocol policy: `Redirect HTTP to HTTPS`
- allowed methods
  - 기본 정적 behavior: `GET, HEAD, OPTIONS`
  - `/api/*` behavior: `GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE`
- cache policy
  - 기본 정적 behavior: `CachingOptimized` 계열
  - `/api/*` behavior: 캐시 비활성 또는 매우 짧은 TTL
- compression: `enabled`
- default root object: `index.html`

### path pattern 기준

- 기본 behavior `*` -> S3 origin
- ordered behavior `/api/*` -> ALB origin

이 방식이면 브라우저 기준으로 frontend와 API가 같은 origin 아래에 있으므로, 현재 앱의 세션/쿠키/CORS 가정과 가장 잘 맞는다.

### SPA fallback 기준

현재 프론트는 React Router 기반 SPA이므로, 존재하지 않는 정적 경로도 `index.html`로 되돌려야 한다.

CloudFront에서는 아래 방식 중 하나로 처리한다.

1. Custom error response
   - `403 -> /index.html -> 200`
   - `404 -> /index.html -> 200`
2. CloudFront Function / Lambda@Edge
   - 브라우저 라우트만 `index.html`로 rewrite

현재 기준선은 단순한 `custom error response`부터 시작한다.

### OAC 기준

- S3 bucket은 public으로 열지 않는다.
- CloudFront distribution 생성 시 OAC를 새로 만든다.
- bucket policy는 OAC가 붙은 CloudFront distribution만 읽을 수 있게 제한한다.

bucket policy 예시:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontRead",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::happygallery-frontend-prod/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::<AWS_ACCOUNT_ID>:distribution/<CLOUDFRONT_DISTRIBUTION_ID>"
        }
      }
    }
  ]
}
```

### 지금 단계에서 할 일

1. S3 bucket을 origin으로 둔 CloudFront distribution 생성
2. OAC 생성 및 연결
3. `default root object = index.html` 설정
4. `403`, `404`를 `/index.html`로 돌리는 custom error response 설정
5. distribution 생성 후 `CLOUDFRONT_DISTRIBUTION_ID` 확보
6. 그 값을 GitHub Actions variable에 입력

### 지금 단계에서 아직 안 정해지는 값

- `/api/*` origin으로 붙일 실제 ALB 도메인
- custom domain 이름
- ACM certificate ARN
- WAF 연결 여부

## 6. VPC / subnet 기본값

운영 배포는 `default VPC` 대신 별도 VPC를 생성해서 사용하는 것을 기준으로 한다.

### 이유

- public ingress와 private app/data 계층을 분리하기 쉽다.
- ECS, RDS, Redis를 private subnet에 배치하는 구조가 더 자연스럽다.
- 나중에 보안 그룹, NAT, 라우팅, 멀티 AZ 확장이 쉬워진다.

### 현재 기준

- VPC 이름 예시: `vpc-happygallery-prod`
- 리전: `ap-northeast-2`
- AZ는 최소 2개 사용
- public subnet 2개
- private subnet 2개
- Internet Gateway 1개
- NAT Gateway 1개 이상

### 권장 CIDR 예시

- VPC: `10.0.0.0/16`
- public subnet A: `10.0.1.0/24`
- public subnet B: `10.0.2.0/24`
- private subnet A: `10.0.11.0/24`
- private subnet B: `10.0.12.0/24`

### 배치 원칙

- ALB: public subnet
- ECS app: private subnet
- RDS: private subnet
- Redis: private subnet

### route table 기준

- public route table
  - `0.0.0.0/0 -> Internet Gateway`
- private route table
  - `0.0.0.0/0 -> NAT Gateway`

### 지금 단계에서 할 일

1. 새 VPC 생성
2. public/private subnet을 AZ 2개에 걸쳐 생성
3. Internet Gateway 연결
4. NAT Gateway 생성
5. public/private route table 연결

### 메모

- 당장 PoC만 띄우는 목적이면 default VPC도 가능하지만, 운영 기준선으로는 채택하지 않는다.
- RDS를 만들기 전에 VPC 구조를 먼저 확정하는 편이 맞다.

## 7. RDS MySQL 기본값

운영 DB는 ECS 내부 컨테이너보다 `RDS MySQL 8`을 기준으로 한다.

### 현재 기준

- 엔진: `MySQL 8.0`
- 리전: `ap-northeast-2`
- DB 이름: `happygallery`
- 권장 identifier 예시: `happygallery-prod-mysql`
- Public access: `No`
- 배치 위치: ECS/ALB와 같은 VPC, private subnet
- storage encryption: `enabled`
- automated backup: `7일 이상`
- deletion protection: `enabled`

### 인스턴스 시작점

- 비용 우선 시작: `db.t4g.micro` 또는 `db.t4g.small`
- 안정성 우선: `db.t4g.small` 이상
- 가용성
  - 초기 비용 절감: `Single-AZ`
  - 운영 안정성 우선: `Multi-AZ`

### 연결 문자열 기준

애플리케이션은 아래 형태의 JDBC URL을 사용한다.

```text
jdbc:mysql://<RDS_ENDPOINT>:3306/happygallery?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
```

### 지금 단계에서 확보해야 하는 값

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- RDS endpoint
- DB port (`3306`)

### 콘솔에서 생성할 때 체크할 항목

1. `RDS -> Create database`
2. Engine: `MySQL`
3. Version: `8.0`
4. Template: 처음이면 `Dev/Test`, 운영 안정성 우선이면 `Production`
5. DB instance identifier: `happygallery-prod-mysql`
6. Master username 설정
7. Credential management는 가능하면 Secrets Manager 연동 검토
8. DB name: `happygallery`
9. Public access: `No`
10. VPC는 ECS와 같은 VPC 선택
11. DB subnet group은 private subnet 기준 선택
12. Backup retention과 deletion protection 활성화

### 보안 그룹 기준

#### 권장 구조

- `sg-happygallery-alb`
  - 80/443 inbound from internet
- `sg-happygallery-app`
  - app container inbound from `sg-happygallery-alb`
- `sg-happygallery-rds`
  - `3306 inbound from sg-happygallery-app`

#### 핵심 원칙

- RDS inbound를 `0.0.0.0/0`로 열지 않는다.
- RDS inbound는 ECS app security group에서만 받는다.
- ALB security group이 직접 RDS에 붙지 않게 한다.

#### RDS security group 예시

- inbound
  - type: `MySQL/Aurora`
  - port: `3306`
  - source: `sg-happygallery-app`
- outbound
  - 기본 허용 유지 가능

### 메모

- 이 앱은 Flyway를 startup 시 자동 실행하므로, ECS task가 RDS에 바로 붙을 수 있어야 한다.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 GitHub Secrets가 아니라 ECS task runtime 또는 Secrets Manager/SSM에서 주입한다.

## 8. Redis 기본값

이 프로젝트는 Redis를 회원 세션, 관리자 Bearer 세션, rate limit 저장소로 사용한다.
운영 Redis는 ECS 내부 컨테이너보다 `ElastiCache for Redis`를 기준으로 한다.

### 현재 기준

- 서비스: `ElastiCache for Redis`
- 리전: `ap-northeast-2`
- 배치 위치: ECS와 같은 VPC, private subnet
- Public access: 없음
- 권장 identifier 예시: `happygallery-prod-redis`
- 권장 포트: `6379`

### 시작점

- 비용 우선 시작: `cache.t4g.micro`
- 운영 안정성 우선: `cache.t4g.small` 이상
- 초기에는 단일 노드 가능
- 안정성 우선이면 replica 포함 구성과 multi-AZ를 추후 검토

### 지금 단계에서 확보해야 하는 값

- Redis primary endpoint
- `REDIS_HOST`
- `REDIS_PORT`

### 콘솔에서 생성할 때 체크할 항목

1. `ElastiCache -> Redis OSS` 선택
2. Cluster mode는 초기엔 `disabled`로 단순하게 시작 가능
3. 이름: `happygallery-prod-redis`
4. Node type: `cache.t4g.micro` 또는 `cache.t4g.small`
5. Port: `6379`
6. VPC는 ECS/RDS와 같은 VPC 선택
7. subnet group은 private subnet 기준으로 생성
8. security group은 Redis 전용 SG 선택
9. 필요 시 at-rest / in-transit encryption 여부 검토

### subnet group 기준

- private subnet A
- private subnet B

즉, Redis도 public subnet에 두지 않는다.

### 보안 그룹 기준

#### 권장 구조

- `sg-happygallery-app`
- `sg-happygallery-redis`

#### Redis security group inbound

- type: `Custom TCP`
- port: `6379`
- source: `sg-happygallery-app`

#### 핵심 원칙

- Redis inbound를 `0.0.0.0/0`로 열지 않는다.
- Redis inbound는 ECS app security group에서만 받는다.

### 메모

- 이 앱은 `REDIS_HOST`, `REDIS_PORT` 환경변수 기준으로 연결한다.
- Redis 인증이나 TLS를 켜면 ECS task 환경변수와 클라이언트 설정을 함께 맞춰야 한다.
- 운영 비밀값은 GitHub Secrets 대신 ECS runtime 또는 Secrets Manager/SSM에서 주입한다.

## 9. ECS Task Definition 기본값

운영 앱 실행은 `ECS Fargate` 기준으로 한다.

### 현재 기준

- launch type: `Fargate`
- OS: `Linux`
- task definition family 예시: `happygallery-prod-app`
- container port: `8080`
- 시작 리소스 권장값
  - CPU: `512`
  - Memory: `1024`

### ALB / service 배치 기준

- ALB: public subnet
- ECS service: private subnet
- ECS task public IP: `disabled`
- target group target type: `ip`

### 필수 일반 환경변수

| 이름 | 용도 |
|------|------|
| `SPRING_PROFILES_ACTIVE=prod` | 운영 프로필 활성화 |
| `DB_URL` | RDS JDBC URL |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `REDIS_HOST` | Redis endpoint |
| `REDIS_PORT` | Redis 포트 |
| `MANAGEMENT_PORT=8080` | 초기 ALB health check 단순화를 위해 actuator를 앱 포트와 동일화 |
| `RATE_LIMIT_TRUST_FORWARDED=true` | ALB/CloudFront 뒤 실제 IP 기준 rate limit 유지 |
| `SENTRY_ENVIRONMENT=production` | backend Sentry 환경값 |
| `SENTRY_RELEASE` | backend 배포 버전/commit 추적 |

### 필수 비밀값

| 이름 | 용도 |
|------|------|
| `GUEST_TOKEN_HMAC_SECRET` | guest access token 서명 |
| `APP_FIELD_ENCRYPTION_ENCRYPT_KEY` | 개인정보 필드 암호화 키 |
| `APP_FIELD_ENCRYPTION_HMAC_KEY` | blind index / 검증용 HMAC 키 |
| `SENTRY_DSN` | backend Sentry DSN |

### 외부 연동 사용 시 필수값

| 이름 | 용도 |
|------|------|
| `GOOGLE_OAUTH_CLIENT_ID` | Google 로그인 |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Google 로그인 |
| `KAKAO_API_KEY` | 카카오 알림톡 |
| `KAKAO_SENDER_KEY` | 카카오 알림톡 |
| `SMS_API_KEY` | SMS 발송 |
| `SMS_API_SECRET` | SMS 발송 |
| `SMS_SENDER_NUMBER` | SMS 발신 번호 |

### 운영 튜닝용 선택값

| 이름 | 기본값 |
|------|------|
| `TX_DEFAULT_TIMEOUT` | `10s` |
| `DB_HIKARI_CONNECTION_TIMEOUT_MS` | `2000` |
| `DB_CONNECT_TIMEOUT_MS` | `2000` |
| `DB_SOCKET_TIMEOUT_MS` | `5000` |
| `DB_QUERY_TIMEOUT_MS` | `5000` |
| `DB_LOCK_WAIT_TIMEOUT_SECONDS` | `3` |
| `ACTUATOR_HEALTH_SHOW_DETAILS` | `never` |
| `GOOGLE_OAUTH_TIMEOUT_MILLIS` | `5000` |
| `GOOGLE_OAUTH_CONNECT_TIMEOUT_MILLIS` | `2000` |
| `GOOGLE_OAUTH_ACQUIRE_TIMEOUT_MILLIS` | `1000` |
| `GOOGLE_OAUTH_MAX_CONNECTIONS` | `10` |
| `GOOGLE_OAUTH_KEEP_ALIVE_MILLIS` | `30000` |
| `KAKAO_TIMEOUT_MILLIS` | `5000` |
| `KAKAO_CONNECT_TIMEOUT_MILLIS` | `2000` |
| `KAKAO_ACQUIRE_TIMEOUT_MILLIS` | `1000` |
| `KAKAO_MAX_CONNECTIONS` | `20` |
| `KAKAO_KEEP_ALIVE_MILLIS` | `30000` |
| `SMS_TIMEOUT_MILLIS` | `5000` |
| `SMS_CONNECT_TIMEOUT_MILLIS` | `2000` |
| `SMS_ACQUIRE_TIMEOUT_MILLIS` | `1000` |
| `SMS_MAX_CONNECTIONS` | `20` |
| `SMS_KEEP_ALIVE_MILLIS` | `30000` |

### 메모

- 비밀값은 GitHub Actions가 아니라 ECS task runtime의 Secrets Manager/SSM 주입을 우선한다.
- `app.field-encryption.*`는 Spring relaxed binding 기준으로 `APP_FIELD_ENCRYPTION_ENCRYPT_KEY`, `APP_FIELD_ENCRYPTION_HMAC_KEY` 환경변수로 주입한다.
- local 전용 `ADMIN_API_KEY`, `ADMIN_ENABLE_API_KEY_AUTH=true`는 운영에서 기본값으로 쓰지 않는다.

## 10. ALB Target Group health check 기준

### 현재 기준

- health check path: `/actuator/health`
- health check port: `traffic port (8080)`
- matcher: `200`

### 이유

- 기본 설정상 `management.server.port`는 prod에서 `8081`이다.
- 하지만 초기 ECS/ALB 운영에서는 health check 단순화가 더 중요하므로 `MANAGEMENT_PORT=8080`으로 덮어 actuator를 앱 포트와 합친다.
- 이렇게 하면 ALB target group health check를 별도 관리 포트 없이 바로 사용할 수 있다.

### target group 권장값

- protocol: `HTTP`
- target type: `ip`
- port: `8080`
- healthy threshold: `2`
- unhealthy threshold: `2`
- timeout: `5s`
- interval: `15s` 또는 `30s`

### 재검토 조건

- 나중에 management port를 application port와 다시 분리하고 싶을 때
- ALB 대신 service connect / service mesh / 내부 health check 전략을 별도로 둘 때

## 11. GitHub Actions Secrets / Variables 기준선

현재 `.github/workflows/deploy.yml` 기준으로 필요한 값은 아래와 같다.

### Secrets

| 이름 | 용도 |
|------|------|
| `AWS_ROLE_TO_ASSUME` | GitHub Actions OIDC가 assume할 IAM Role ARN |
| `VITE_SENTRY_DSN` | frontend production build 시 주입하는 Sentry DSN |

### Variables

| 이름 | 용도 |
|------|------|
| `ECR_REPOSITORY` | backend Docker 이미지 push 대상 ECR repository 이름 |
| `ECS_CLUSTER` | backend ECS cluster 이름 |
| `ECS_SERVICE` | backend ECS service 이름 |
| `S3_BUCKET` | frontend 정적 파일을 sync할 S3 bucket 이름 |
| `CLOUDFRONT_DISTRIBUTION_ID` | frontend 배포 후 invalidation 대상 CloudFront distribution ID |

### 메모

- 현재 deploy workflow는 `AWS_ACCOUNT_ID`를 직접 사용하지 않는다.
- DB 비밀번호, Redis 비밀번호, API 키, 암호화 키 같은 운영 비밀값은 GitHub Secrets에 넣지 않는다.
- 앱 런타임 비밀값은 ECS Task Definition + Secrets Manager 또는 SSM Parameter Store에서 주입한다.

## 12. GitHub Actions OIDC IAM Role trust policy 기준선

현재 deploy workflow는 `push -> main`에서만 배포된다.
그래서 trust policy도 우선 `main` 브랜치만 assume 가능하게 좁게 잡는다.

현재 저장소 기준 GitHub repository 식별자는 `ljkhyeong/happyGallery`다.
권장 IAM Role 이름은 `GitHubActionsHappyGalleryDeployRole`로 둔다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:ljkhyeong/happyGallery:ref:refs/heads/main"
        }
      }
    }
  ]
}
```

브랜치 대신 GitHub Environment 기반 승인 게이트를 붙이면 `sub` 조건도 그 구조에 맞게 다시 좁힌다.

## 13. GitHub Actions OIDC IAM Role permission policy 기준선

단일 deploy role을 유지한다면 아래 권한 세트가 현재 workflow와 맞는다.

- ECR login / image push
- ECS service 강제 새 배포
- S3 정적 파일 sync
- CloudFront invalidation

예시 정책:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrAuth",
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EcrPush",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeRepositories",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:ap-northeast-2:<AWS_ACCOUNT_ID>:repository/happygallery-backend"
    },
    {
      "Sid": "EcsDeploy",
      "Effect": "Allow",
      "Action": [
        "ecs:DescribeServices",
        "ecs:UpdateService"
      ],
      "Resource": "arn:aws:ecs:ap-northeast-2:<AWS_ACCOUNT_ID>:service/<ECS_CLUSTER>/<ECS_SERVICE>"
    },
    {
      "Sid": "S3Deploy",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::<S3_BUCKET>"
    },
    {
      "Sid": "S3DeployObjects",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::<S3_BUCKET>/*"
    },
    {
      "Sid": "CloudFrontInvalidate",
      "Effect": "Allow",
      "Action": [
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "arn:aws:cloudfront::<AWS_ACCOUNT_ID>:distribution/<CLOUDFRONT_DISTRIBUTION_ID>"
    }
  ]
}
```

### 메모

- 이 permission policy는 ECR, S3, CloudFront, ECS 리소스 이름이 확정된 뒤 최종 반영한다.
- 나중에 frontend/backend deploy role을 분리하면 이 정책도 역할별로 나누는 편이 더 안전하다.
- ECS 쪽에서 task definition까지 GitHub Actions가 직접 갱신하게 되면 `ecs:RegisterTaskDefinition`, `iam:PassRole` 같은 권한을 추가로 검토한다.

## 14. GitHub에 두지 않는 운영 비밀값

아래 값은 GitHub Secrets 대신 ECS 런타임 쪽에서 관리한다.

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, Redis 인증값
- `GUEST_TOKEN_HMAC_SECRET`
- `app.field-encryption.encrypt-key`
- `app.field-encryption.hmac-key`
- `KAKAO_*`, `SMS_*`, `GOOGLE_OAUTH_*`
- backend `SENTRY_DSN`

## 15. 다음에 이 문서에 이어서 넣을 항목

- Redis 네트워크 및 보안 그룹 기준선
- 배포 전 체크리스트와 rollback 기준

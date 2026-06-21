# AWS 비용 과금 원인 점검 회고

## 날짜

2026-05-03

## 배경

운영 배포를 거의 프리티어 수준으로 생각하고 구성했지만, 2026년 4월 AWS 비용이 약 180달러 수준으로 보였다.
AWS Cost Explorer와 실제 리소스 상태를 확인해 어떤 항목에서 과금이 발생했는지 점검했다.

## 확인한 계정과 기간

- AWS Account: `624558551975`
- 리전: `ap-northeast-2`
- 비용 조회 기간:
  - 2026-04-01 ~ 2026-05-01
  - 2026-05-01 ~ 2026-05-04

## 비용 요약

2026년 4월 실제 비용:

- 사용량: `140.12 USD`
- 세금: `14.02 USD`
- 합계: `154.14 USD`

2026년 5월 1~3일 추정 비용:

- 합계: `10.67 USD`

2026년 5월 전체 예측 비용:

- `188.92 USD`

## 주요 과금 원인

| 서비스 | 2026년 4월 비용 | 확인된 원인 |
| --- | ---: | --- |
| Amazon ElastiCache | `62.17 USD` | `happygallery-prod-redis`가 Valkey Serverless로 계속 실행 |
| EC2 - Other | `36.62 USD` | 대부분 NAT Gateway 시간 과금 |
| Amazon RDS | `16.11 USD` | `happygallery-prod-mysql` `db.t4g.micro`, 20GB gp2 |
| Tax | `14.02 USD` | 세금 |
| Elastic Load Balancing | `11.57 USD` | `happygallery-prod-alb` ALB 실행 시간 과금 |
| Amazon VPC | `7.08 USD` | NAT Gateway 1개 + ALB 2개 public IPv4 주소 과금 |
| Amazon ECS | `6.54 USD` | Fargate task 1개 상시 실행 |

## 세부 확인 내용

### ElastiCache

실제 리소스:

- 이름: `happygallery-prod-redis`
- 타입: ElastiCache Serverless
- 엔진: Valkey 8.1
- 상태: `available`
- endpoint: `happygallery-prod-redis-yrz8rv.serverless.apn2.cache.amazonaws.com`
- 최소 설정:
  - DataStorage minimum: `1 GB`
  - ECPUPerSecond minimum: `1000`

Cost Explorer usage type:

- `APN2-CachedData:Valkey`: `56.68 USD`
- `APN2-ElastiCacheProcessingUnits:Valkey`: `5.45 USD`
- `APN2-NodeUsage:cache.t4g.micro`: `0.04 USD`

결론:

- 비용의 가장 큰 원인은 Redis를 작게 띄운 것이 아니라 Valkey Serverless를 계속 켜둔 것이다.
- 개발/학습용이면 Serverless Redis는 비용 예측이 어렵고, 미사용 기간에는 삭제하거나 더 단순한 대체 구성을 써야 한다.

### NAT Gateway

실제 리소스:

- NAT Gateway: `nat-0fae8f087e1000941`
- 상태: `available`
- VPC: `vpc-09dbb1aed9b448000`
- Public IP: `13.125.140.235`

Cost Explorer usage type:

- `APN2-NatGateway-Hours`: `36.05 USD`, `611 Hrs`
- `APN2-NatGateway-Bytes`: `0.52 USD`, `8.90 GB`

결론:

- NAT Gateway는 트래픽이 거의 없어도 available 상태의 시간 과금이 크다.
- 개인 프로젝트/학습용에서는 가장 먼저 제거하거나 NAT 없이 운영하는 구조를 검토해야 한다.

### RDS

실제 리소스:

- DB: `happygallery-prod-mysql`
- 클래스: `db.t4g.micro`
- 엔진: MySQL
- 상태: `available`
- Multi-AZ: `false`
- 스토리지: `20GB gp2`
- Public access: `false`
- Deletion protection: `true`

Cost Explorer usage type:

- `APN2-InstanceUsage:db.t4g.micro`: `14.06 USD`, `562.42 Hrs`
- `APN2-RDS:GP2-Storage`: `2.05 USD`, `15.62 GB-Month`

결론:

- RDS도 상시 실행하면 작은 인스턴스라도 비용이 계속 붙는다.
- 데이터 보존이 필요하면 stop으로 비용을 줄일 수 있지만, RDS stop은 장기 영구 중지가 아니므로 장기 미사용이면 snapshot 후 삭제를 검토해야 한다.

### ALB

실제 리소스:

- ALB: `happygallery-prod-alb`
- DNS: `happygallery-prod-alb-323895174.ap-northeast-2.elb.amazonaws.com`
- 상태: `active`
- scheme: `internet-facing`
- public subnet 2개에 배치

Cost Explorer usage type:

- `APN2-LoadBalancerUsage`: `11.57 USD`, `514 Hrs`
- `APN2-LCUUsage`: `0.00 USD`

결론:

- 트래픽이 거의 없어도 ALB 실행 시간 비용이 붙는다.
- 개인 프로젝트의 상시 운영 비용을 낮추려면 ALB 기반 구조가 적합한지 다시 판단해야 한다.

### ECS Fargate

실제 리소스:

- Cluster: `happygallery-prod-cluster`
- Service: `happygallery-prod-app-service-dea1frz2`
- Desired count: `1`
- Running count: `1`
- Task definition: `happygallery-prod-app:14`
- Task resource: `512 CPU`, `1024 Memory`, `ARM64`

Cost Explorer usage type:

- `APN2-Fargate-ARM-vCPU-Hours:perCPU`: `5.33 USD`
- `APN2-Fargate-ARM-GB-Hours`: `1.17 USD`

결론:

- Fargate 자체 비용은 전체 비용에서 가장 큰 항목은 아니지만, 상시 실행하면 계속 과금된다.

### Public IPv4

실제 public IPv4 연결:

- `13.125.140.235`: NAT Gateway
- `3.35.59.15`: ALB ENI
- `3.37.122.42`: ALB ENI

Cost Explorer usage type:

- `APN2-PublicIPv4:InUseAddress`: `7.08 USD`, `1416.08 Hrs`

결론:

- public IPv4도 시간당 과금된다.
- ALB와 NAT Gateway를 유지하면 public IPv4 비용도 같이 붙는다.

## 판단

이번 비용은 트래픽 때문이 아니라 상시 운영형 리소스를 켜둔 구조 때문에 발생했다.

특히 아래 리소스는 프리티어 감각으로 접근하면 안 된다.

- ElastiCache Serverless
- NAT Gateway
- ALB
- Public IPv4
- RDS 상시 실행
- ECS Fargate 상시 실행

## 즉시 조치 기준

비용을 멈추기 위해 아래 순서로 조치한다.

1. ECS service desired count를 `0`으로 변경한다.
2. ElastiCache Serverless cache를 삭제한다.
3. ALB를 삭제한다.
4. NAT Gateway를 삭제한다.
5. NAT Gateway에 연결된 Elastic IP를 release한다.
6. RDS는 우선 stop하고, 데이터 보존이 필요 없으면 deletion protection을 끈 뒤 삭제한다.
7. CloudFront는 API origin이 사라졌으므로 disable한다.
8. CloudFront flat-rate pricing plan, WAF, 저장소성 리소스(S3/ECR/CloudWatch Logs)가 남아 있는지 확인한다.
9. 리전 전체와 주요 관리형 서비스를 조회해 추가 과금 후보가 남았는지 확인한다.

## 실행 결과

2026-05-03에 아래 조치를 실행했다.

| 리소스 | 조치 | 최종 확인 상태 |
| --- | --- | --- |
| ECS service `happygallery-prod-app-service-dea1frz2` | desired count `0`으로 변경 | `Desired=0`, `Running=0`, `Pending=0` |
| ElastiCache Serverless `happygallery-prod-redis` | 삭제 | `ServerlessCaches=[]` |
| RDS `happygallery-prod-mysql` | stop 후 deletion protection 해제, final snapshot 없이 삭제, automated backups 삭제 | `DBInstanceNotFound`, automated backups `[]`, manual snapshots `[]` |
| ALB `happygallery-prod-alb` | 삭제 | `describe-load-balancers=[]` |
| NAT Gateway `nat-0fae8f087e1000941` | 삭제 | `deleted` |
| NAT Gateway Elastic IP `eipalloc-0a1acc281e53c9144` | release | `describe-addresses=[]` |
| CloudFront `E49HZ9F34TGGL` | disable | `Status=Deployed`, `Enabled=false` |

### 추가 확인 결과

아래 리소스는 삭제 또는 중지된 상태다.

- EC2 instances: `[]`
- EBS volumes: `[]`
- Elastic IP addresses: `[]`
- VPC endpoints: `[]`
- Route53 hosted zones: `[]`
- Route53 health checks: `[]`
- ElastiCache Serverless: `[]`
- ElastiCache node clusters: `[]`
- ALB: `[]`
- NAT Gateway: `deleted`
- ECS service: `Desired=0`, `Running=0`

아래 리소스는 "실행 중"은 아니지만 저장량 또는 구독 상태 때문에 남은 비용 확인 대상이다.

- S3 bucket `happygallery-frontend-prod-624558551975-ap-northeast-2-an`: 32 objects, `748.5 KiB`
- ECR repository `happygallery-backend`: 14 images, CLI 표시 합계 `2,084,168,598 bytes`
- CloudWatch Logs `/ecs/happygallery-prod-app`: `8,355,588 bytes`, retention 미설정
- Secrets Manager: `[]`
- KMS: `alias/aws/*` AWS 관리형 키만 확인

CloudFront `E49HZ9F34TGGL`은 disabled 상태지만 flat-rate pricing plan 구독에 묶여 있다.
이 상태에서는 AWS가 WebACL 분리와 distribution 삭제를 막는다.

- WebACL: `CreatedByCloudFront-b590ed68`
- WebACL ARN: `arn:aws:wafv2:us-east-1:624558551975:global/webacl/CreatedByCloudFront-b590ed68/b138a3cb-c338-4e19-8520-ff0ff2b95c40`
- WebACL 분리 시도 결과: `Distributions with a pricing plan subscription must have a web ACL resource`
- CloudFront 삭제 시도 결과: `You can't delete this distribution while it's subscribed to a pricing plan`
- Cost Explorer 2026-05-01 ~ 2026-05-04 기준 `CloudFront Flat-Rate Plans`: `0 USD`

AWS 문서 기준으로 pricing plan 구독은 콘솔에서 취소해야 하며, 취소는 현재 billing period 종료 시점에 반영된다.
pricing plan에 가입된 distribution은 구독 취소 후 현재 billing cycle이 끝난 뒤 삭제할 수 있다.
참고: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/flat-rate-pricing-plan.html

### 2026-05-04 추가 과금 후보 점검

Cost Explorer 2026-05-01 ~ 2026-05-04 기준 비용은 대부분 리소스를 끄기 전 약 49시간 사용분이다.

| 비용 항목 | 금액 | 해석 |
| --- | ---: | --- |
| Amazon ElastiCache | `5.41 USD` | 삭제 전 Valkey Serverless 사용분 |
| EC2 - Other | `2.89 USD` | 삭제 전 NAT Gateway 사용분 중심 |
| Amazon RDS | `1.40 USD` | 삭제 전 `db.t4g.micro`와 gp2 storage 사용분 |
| Amazon ECS | `1.11 USD` | desired count 0 전 Fargate 사용분 |
| Elastic Load Balancing | `1.10 USD` | ALB 삭제 전 사용분 |
| Amazon VPC | `0.73 USD` | ALB/NAT public IPv4 삭제 전 사용분 |
| ECR | `0.008 USD` | 이미지 저장량 기반 소액 |
| S3 | `0.00001 USD` | 정적 파일 저장/요청 기반 소액 |
| AWS Secrets Manager | `0.000005 USD` | API request 1건 |
| AWS Cost Explorer | `0.09 USD` | 비용 조회 API 호출 9건 |

2026-05-04에 확인한 추가 리소스 상태:

- 모든 활성 리전 EC2 계열: EC2 instances, EBS volumes, EBS snapshots, Elastic IP, NAT Gateway, VPC endpoint, Transit Gateway, VPN 모두 잔존 없음
- ALB/NLB/Classic ELB: `[]`
- RDS instance/cluster, DocumentDB, Neptune: `[]`
- EFS, FSx: `[]`
- OpenSearch, Redshift, MSK, MemoryDB: `[]`
- Lambda, API Gateway REST/HTTP/WebSocket, EventBridge rules/schedules, AWS Batch: `[]`
- SNS, SQS, DynamoDB, AWS Backup vaults, CloudWatch alarms: `[]`
- Route53 hosted zones/health checks: `[]`
- ACM us-east-1 certificates: `[]`
- Application Auto Scaling / EC2 Auto Scaling Group: `[]`

남은 소액 또는 정리 후보:

- S3 bucket `happygallery-frontend-prod-624558551975-ap-northeast-2-an`
  - 버전 관리: `Enabled`
  - 현재 객체 기준: 32 objects, `748.5 KiB`
  - 버전 포함: 270 versions, 54 delete markers, `6,892,050 bytes`
- ECR repository `happygallery-backend`
  - 14 images, tagged 10, untagged 4, CLI 표시 합계 `2,084,168,598 bytes`
  - lifecycle policy 있음: untagged 1일 후 삭제, tagged 최근 30개 유지
- CloudWatch Logs `/ecs/happygallery-prod-app`
  - `9,065,711 bytes`
  - retention 미설정
- SSM Parameter Store
  - `/happygallery/prod/*` SecureString 4개
  - 모두 `Standard` tier
- ECS cluster/service shell
  - cluster/service 정의는 남아 있음
  - service `Desired=0`, `Running=0`, `Pending=0`
- IAM GitHub Actions OIDC provider
  - `arn:aws:iam::624558551975:oidc-provider/token.actions.githubusercontent.com`
  - IAM 리소스라 직접 과금 후보는 아니지만 배포 재가동 권한으로 남아 있음

2026-05-04에 아래 소액 후보 정리를 실행했다.

| 리소스 | 조치 | 최종 확인 상태 |
| --- | --- | --- |
| CloudWatch Logs `/ecs/happygallery-prod-app` | log group 삭제 대신 retention `14일` 설정 | `StoredBytes=9,065,711`, `Retention=14` |
| S3 bucket `happygallery-frontend-prod-624558551975-ap-northeast-2-an` | versioning `Suspended`, 비최신 object version 238개 삭제 | 최신 version 32개, 비최신 version 0개, 최신 delete marker 54개, version bytes `766,436` |
| ECR repository `happygallery-backend` | 최신 2개 image digest만 남기고 12개 digest 삭제 | image 2개, untagged 0개, CLI 표시 합계 `346,596,261 bytes` |

S3 최신 delete marker 54개는 삭제하지 않았다.
현재 delete marker를 삭제하면 이전 파일 버전이 다시 현재 객체로 보일 수 있기 때문이다.

### 남긴 리소스

아래 리소스는 즉시 큰 시간 과금 원인이 아니거나 데이터/배포 산출물을 보존하기 위해 삭제하지 않았다.

- S3 frontend bucket: versioning suspended, 현재 객체와 최신 delete marker만 유지
- ECR repository/image: 최신 2개 digest만 유지
- CloudWatch Logs log group: retention 14일
- SSM Standard parameters
- ECS cluster/service/task definition
- IAM GitHub Actions OIDC provider
- VPC, subnet, route table, security group
- CloudFront distribution shell과 WebACL: flat-rate pricing plan 구독 때문에 CLI 삭제가 막힘

## 다음에 다시 운영할 때의 기준

- 상시 운영이 필요하지 않으면 AWS 리소스를 기본적으로 꺼둔다.
- 개인 프로젝트 학습/검증용이면 NAT Gateway와 ALB를 먼저 피한다.
- Redis는 꼭 필요한지 다시 판단한다. 필요하면 비용 예측 가능한 node형 또는 앱 설정 단순화를 검토한다.
- RDS는 장기 미사용이면 snapshot 필요 여부를 먼저 정하고 삭제까지 검토한다.
- CloudFront 생성 시 flat-rate pricing plan을 선택하면 WAF가 같이 붙고, distribution disable만으로는 구독이 정리되지 않는다.
- Cost Explorer API도 호출당 비용이 있으므로 반복 조회는 필요한 시점에만 한다.
- S3는 버전 관리가 켜져 있으면 현재 객체보다 과거 버전 저장량이 더 커질 수 있다.
- CloudWatch Logs는 운영 중단 시 retention을 짧게 설정하거나 log group 삭제를 검토한다.
- AWS Budget을 최소 `20 USD`, `50 USD`, `100 USD` 구간으로 설정한다.
- 배포 문서에는 "프리티어 친화 구성"과 "운영형 구성"을 분리해서 적는다.

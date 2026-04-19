# happyGallery

`happyGallery`는 오프라인 공방의 온라인 주문·예약 운영 서비스다.  
상품 주문, 클래스 예약, 8회권, 관리자 운영 기능을 하나의 서비스로 제공한다.

## 서비스 개요

- 온라인 주문과 오프라인 판매가 같은 재고를 공유한다.
- 클래스 예약의 예약금, 환불 마감, 변경 마감 같은 운영 규칙을 시스템으로 관리한다.
- 8회권은 회원 기준으로 관리하고, 만료와 환불 후속 처리까지 자동화한다.
- 관리자 화면에서 주문, 예약, 환불, 문의, Q&A 운영을 한곳에서 처리한다.

### 사용자별 주요 기능

| 사용자 | 주요 기능 |
| --- | --- |
| 비회원 | 휴대폰 인증 기반 주문/예약 생성, 토큰 기반 조회, 회원 가입 후 기존 이력 가져오기 |
| 회원 | 상품 주문, 클래스 예약, 8회권 구매/사용, 장바구니, 알림함, 마이페이지 |
| 관리자 | 상품/슬롯 관리, 주문 승인/거절/배송/픽업, 예약 운영, 대시보드, 환불 재시도, Q&A/문의 답변 |

### 핵심 운영 규칙

- 상품 주문: 결제 즉시 재고 차감, 관리자가 24시간 안에 승인/거절, 미처리 시 자동 환불
- 클래스 예약: 예약금 10%, 전날 00:00 이후 환불 불가, 시작 1시간 전까지만 변경 가능
- 8회권: 회원 전용, 결제일 기준 90일 유효, 환불 시 미래 예약 자동 취소
- 인증 방식: 회원 `HG_SESSION`, 관리자 Bearer 세션, 비회원 `X-Access-Token`

## 운영 주소

- 현재 운영 주소: `https://d36l7yi27358tl.cloudfront.net/`
- 주요 경로: 스토어 `/products`, 예약 생성 `/bookings/new`, 8회권 구매 `/passes/purchase`, 관리자 `/admin`

## 배포 구조

현재 운영 배포는 `CloudFront + S3 + ALB + ECS Fargate` 구조다.

```text
사용자 브라우저
  -> CloudFront
       -> S3 (프론트 정적 파일)
       -> /api/* -> ALB
                     -> ECS Fargate (Spring Boot 앱)
                          -> RDS MySQL
                          -> ElastiCache Redis
```

- 사용자는 CloudFront 주소로 접속한다.
- 프론트 정적 파일은 S3에 배포되고 CloudFront가 캐시와 라우팅을 담당한다.
- API 요청은 CloudFront가 ALB로 전달하고, ALB 뒤에서 ECS Fargate가 백엔드를 실행한다.
- 데이터는 RDS MySQL에 저장한다.
- 회원 세션, 관리자 세션, 요청 제한 카운터는 Redis를 사용한다.

### 배포 파이프라인

`main` 브랜치에 push되면 GitHub Actions가 자동 배포한다.

- 프론트: `npm build` -> `S3 sync` -> `CloudFront invalidation`
- 백엔드: `bootJar` -> Docker build -> `ECR push` -> `ECS update-service --force-new-deployment`

배포 관련 문서:
- [docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md](docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md)
- [docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md](docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md)
- [docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md](docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md)

## 저장소 구조

백엔드는 6개 모듈로 나뉘어 있고, 프론트는 `frontend/`에서 별도로 관리한다.

| 경로 | 역할 |
| --- | --- |
| `bootstrap/` | 앱 시작점, 설정 파일, Flyway migration, 로깅 설정 |
| `adapter-in-web/` | HTTP API, 필터, 요청 처리 |
| `adapter-out-persistence/` | JPA, MyBatis, DB 연동 |
| `adapter-out-external/` | 결제, 알림, OAuth, Redis 세션, 외부 API 연동 |
| `application/` | 유스케이스, 업무 로직, 배치 |
| `domain/` | 핵심 도메인 모델과 규칙 |
| `frontend/` | React 기반 사용자/관리자 화면 |
| `monitoring/` | Prometheus, Grafana, Alertmanager 설정 |

관리자 검색과 대시보드 집계는 MyBatis를 사용하고, 일반 조회/저장은 JPA를 사용한다.

## 기술 스택

- Backend: Spring Boot 4.0.2, Java 21, Gradle
- Frontend: Vite, React 19, TypeScript
- Database: MySQL 8, Flyway
- Session / cache / rate limit: Redis, Spring Session
- Query: JPA, MyBatis
- Infra: AWS CloudFront, S3, ALB, ECS Fargate, RDS, ElastiCache
- Monitoring: Actuator, Prometheus, Grafana, Sentry
- Test: JUnit 5, Testcontainers, Playwright

## 로컬 실행

### 요구사항

- Java 21
- Node.js 20+
- Docker / Docker Compose

### 빠르게 실행하기

1. 인프라 실행

```bash
docker compose up -d mysql redis
```

2. 백엔드 실행

```bash
./gradlew :bootstrap:bootRun
```

3. 프론트 실행

```bash
cd frontend
npm install
npm run dev
```

### 로컬 주소

- 프론트: `http://localhost:3000`
- 백엔드: `http://localhost:8080`
- 헬스 체크: `http://localhost:8080/actuator/health`

### local 프로필 기본 동작

- DB가 비어 있으면 기본 클래스 3종을 자동 생성한다.
- 관리자 계정이 없으면 `admin / admin1234`를 자동 생성한다.
- local/dev 보조 호출은 `X-Admin-Key: dev-admin-key`를 사용할 수 있다.
- 알림 발송은 `!prod`에서 fake sender를 사용한다.

### Docker로 전체 스택 실행

로컬 Docker 구성은 `nginx + app + mysql + redis + monitoring` 조합이다.  
프론트 정적 파일은 `frontend/dist`를 nginx가 서빙하므로 먼저 build가 필요하다.

```bash
cd frontend
npm install
npm run build
cd ..
docker compose up -d --build
```

- `http://localhost`: nginx + 프론트 정적 파일 + `/api` 프록시
- `http://localhost:9090`: Prometheus
- `http://localhost:9093`: Alertmanager
- `http://localhost:3001`: Grafana

### 운영 환경 최초 관리자 계정 생성

- `local`에서는 기본 관리자 계정이 자동 생성되므로 별도 작업이 필요 없다.
- `local`이 아닌 환경에서 관리자 계정이 없으면 `ADMIN_SETUP_TOKEN`을 잠깐 주입하고 `/api/v1/admin/setup`으로 최초 계정을 만든다.
- setup이 끝나면 `ADMIN_SETUP_TOKEN`은 제거한다.

## 자주 쓰는 명령어

### 백엔드

- 전체 빌드: `./gradlew build`
- 전체 테스트: `./gradlew test`
- 정책 테스트: `./gradlew :application:policyTest`
- 통합 테스트: `./gradlew --no-daemon :application:useCaseTest`
- 앱 실행: `./gradlew :bootstrap:bootRun`

### 프론트

- 개발 서버: `cd frontend && npm run dev`
- 프로덕션 빌드: `cd frontend && npm run build`
- E2E 브라우저 설치: `cd frontend && npm run e2e:install`
- E2E 실행: `cd frontend && npm run e2e`

## 테스트 메모

- `@UseCaseIT`는 MySQL/Redis Testcontainers와 고정 `Clock`을 사용한다.
- Playwright 실행 전 백엔드는 `http://localhost:8080`에서 실행 중이어야 한다.
- Playwright 관리자 기본 로그인은 `admin / admin1234`다. 필요하면 환경 변수로 덮어쓴다.

## 설정과 문서

- 로컬 설정 기준: [application-local.yml](bootstrap/src/main/resources/application-local.yml)
- 공통 설정 기준: [application.yml](bootstrap/src/main/resources/application.yml)
- 제품 요구사항: [docs/PRD/0001_기준_스펙/spec.md](docs/PRD/0001_기준_스펙/spec.md)
- API 계약: [docs/PRD/0004_API_계약/spec.md](docs/PRD/0004_API_계약/spec.md)
- 설계 결정: [docs/ADR](docs/ADR/)
- 검토 메모와 배경: [docs/Idea](docs/Idea/)

`docs/Idea`는 배경 메모다. 현재 동작과 운영 기준은 PRD와 ADR을 먼저 본다.

## 브랜치 흐름

- 작업 브랜치: `codex/work-*`
- 통합 브랜치: `codexReview`
- 최종 반영: `main`
- 구현 변경 시 README, HANDOFF, PRD, ADR 중 영향 받는 문서를 함께 갱신한다.

# happyGallery

`happyGallery`는 오프라인 공방의 상품 주문, 클래스 예약, 8회권, 관리자 운영을 다루는 서비스다.
백엔드는 Spring Boot 멀티 모듈 애플리케이션이고, 프론트엔드는 Vite + React SPA다.

## 한눈에 보기

| 사용자 | 주요 기능 |
| --- | --- |
| 비회원 | 휴대폰 인증 기반 주문/예약 생성, 토큰 기반 조회, 회원가입 후 기존 이력 가져오기 |
| 회원 | 상품 주문, 클래스 예약, 8회권 구매/사용, 장바구니, 알림함, 마이페이지 |
| 관리자 | 상품/클래스/슬롯 관리, 주문 승인/거절/배송/픽업, 예약 운영, 환불 재시도, Q&A/문의 답변 |

- 주문/예약/8회권은 `POST /api/v1/payments/prepare` -> `POST /api/v1/payments/confirm` 표준 결제 경로를 사용한다.
- 회원은 `HG_SESSION`, 관리자는 Bearer 세션, 비회원은 `X-Access-Token`을 사용한다.
- 상세 요구사항은 [기준 스펙](docs/PRD/0001_기준_스펙/spec.md), HTTP 계약은 [API 계약](docs/PRD/0004_API_계약/spec.md)을 기준으로 본다.

## 빠른 시작

### 요구사항

- Java 21
- Node.js 20+
- Docker / Docker Compose

### 실행

1. MySQL과 Redis 실행

```bash
docker compose up -d mysql redis
```

2. 백엔드 실행

```bash
./gradlew :bootstrap:bootRun
```

3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

### 로컬 주소

- 프론트엔드: `http://localhost:3000`
- 백엔드: `http://localhost:8080`
- 헬스 체크: `http://localhost:8080/actuator/health`

## 로컬 기본값

- `local` 프로필에서는 DB가 비어 있으면 기본 클래스 3종과 관리자 계정 `admin / admin1234`를 자동 생성한다.
- 로컬과 개발 환경에서는 `X-Admin-Key: dev-admin-key`를 사용할 수 있다.
- `prod`가 아닌 환경에서는 실제 알림/결제 대신 테스트용 발송기와 `FakePaymentProvider`를 사용한다.
- `local`이 아닌 환경에서 최초 관리자 계정이 필요하면 `ADMIN_SETUP_TOKEN`을 주입하고 `/api/v1/admin/setup`을 호출한다.
- 반복 E2E처럼 짧은 시간에 인증/관리 요청이 몰리는 로컬 검증에서는 `RATE_LIMIT_ENABLED=false`를 사용할 수 있다.

전체 로컬 스택이 필요하면 프론트 빌드 후 Docker Compose를 실행한다.

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

## 주요 명령어

### 백엔드

- 전체 빌드: `./gradlew build`
- 전체 테스트: `./gradlew test`
- 정책 테스트: `./gradlew :application:policyTest`
- 통합 테스트: `./gradlew --no-daemon :application:useCaseTest`
- API 계약 문서 테스트: `./gradlew --no-daemon :adapter-in-web:restDocsTest`
- 앱 실행: `./gradlew :bootstrap:bootRun`

### 프론트엔드

- 개발 서버: `cd frontend && npm run dev`
- 프로덕션 빌드: `cd frontend && npm run build`
- E2E 브라우저 설치: `cd frontend && npm run e2e:install`
- E2E smoke: `cd frontend && npm run e2e`
- E2E 도메인별 실행: `cd frontend && npm run e2e:payment`, `npm run e2e:identity`, `npm run e2e:admin`
- E2E 전체 실행: `cd frontend && npm run e2e:full`

## 테스트 기준

- `@UseCaseIT`는 MySQL/Redis Testcontainers와 고정 `Clock`을 사용한다.
- REST Docs 스니펫은 `:adapter-in-web:restDocsTest`가 `adapter-in-web/build/generated-snippets`에 생성한다.
- Playwright 실행 전 백엔드는 `http://localhost:8080`에서 실행 중이어야 한다.
- 기본 E2E는 `@smoke` 대표 경로만 실행한다. 전체 P8 회귀는 `e2e:full` 또는 도메인별 스크립트로 실행한다.

테스트 선택 기준은 [ADR-0027](docs/ADR/0027_테스트_전략과_최소_테스트_세트_기준선/adr.md), E2E 실행 시간 조정 배경은 [Retrospective-0009](docs/Retrospective/0009_프론트_E2E_실행_시간_슬림화/retrospective.md)에 남긴다.

## 저장소 구조

| 경로 | 역할 |
| --- | --- |
| `bootstrap/` | 애플리케이션 시작점, 공통 설정, Flyway, 로깅 |
| `adapter-in-web/` | HTTP API, 필터, 요청/응답 처리 |
| `adapter-out-persistence/` | JPA, MyBatis, 데이터베이스 연동 |
| `adapter-out-external/` | 결제, 알림, OAuth, Redis 세션, 외부 API 연동 |
| `application/` | 유스케이스, 서비스, 배치, 포트 정의 |
| `domain/` | 도메인 모델, 정책, 예외 |
| `frontend/` | React 기반 사용자 화면과 관리자 화면 |
| `monitoring/` | Prometheus, Grafana, Alertmanager 설정 |

- 의존 방향: `bootstrap -> adapter-in-web/out-* -> application -> domain`
- 일반 조회와 저장은 JPA, 관리자 검색과 대시보드 집계는 MyBatis를 사용한다.

## 기술 스택

- 백엔드: Spring Boot 4.0.2, Java 21, Gradle
- 프론트엔드: Vite, React 19, TypeScript
- 데이터베이스: MySQL 8, Flyway
- 세션과 캐시: Redis, Spring Session
- 인프라: AWS CloudFront, S3, ALB, ECS Fargate, RDS, ElastiCache
- 모니터링: Actuator, Prometheus, Grafana, Sentry
- 테스트: JUnit 5, Testcontainers, Spring REST Docs, Playwright

## 운영/배포

- 운영 주소: `https://d36l7yi27358tl.cloudfront.net/`
- 주요 경로: `/products`, `/bookings/new`, `/passes/purchase`, `/my`, `/guest`, `/admin`
- 배포 구조: `CloudFront -> S3`로 프론트 정적 파일을 제공하고, `/api/*`는 `CloudFront -> ALB -> ECS Fargate -> RDS/Redis`로 전달한다.
- 배포 파이프라인: `main` push 시 GitHub Actions가 프론트는 S3/CloudFront로, 백엔드는 ECR/ECS로 배포한다.

운영 배경과 설정 기준:

- [CloudFront + S3 + ALB 배포 구조](docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md)
- [GitHub Actions CI/CD 배포 Fargate](docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md)
- [AWS 배포 설정 베이스라인](docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md)

## 주요 환경 변수

| 이름 | 위치 | 설명 |
| --- | --- | --- |
| `TOSS_SECRET_KEY` | 백엔드 `prod` | Toss Payments secret key |
| `VITE_TOSS_CLIENT_KEY` | 프론트 빌드 | Toss SDK client key |
| `PASS_TOTAL_PRICE` | 백엔드 | 8회권 결제 금액 |
| `RATE_LIMIT_ENABLED` | 백엔드 | 로컬 반복 검증 시 처리율 제한 off 가능 |
| `ADMIN_SETUP_TOKEN` | 백엔드 | 최초 관리자 계정 생성용 일회성 토큰 |

환경별 전체 설정은 [application.yml](bootstrap/src/main/resources/application.yml)과 [application-local.yml](bootstrap/src/main/resources/application-local.yml)을 기준으로 확인한다.

## 문서 진입점

- 요구사항 기준: [docs/PRD/0001_기준_스펙/spec.md](docs/PRD/0001_기준_스펙/spec.md)
- API 계약: [docs/PRD/0004_API_계약/spec.md](docs/PRD/0004_API_계약/spec.md)
- 설계 결정: [docs/ADR](docs/ADR/)
- 배경 메모와 검토 기록: [docs/Idea](docs/Idea/)
- 회고와 트러블슈팅 기록: [docs/Retrospective](docs/Retrospective/)

`docs/Idea`는 배경 메모다. 현재 동작과 운영 기준은 PRD와 ADR을 먼저 본다.

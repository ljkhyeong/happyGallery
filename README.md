# happyGallery

`happyGallery`는 오프라인 공방의 상품 주문, 클래스 예약, 8회권, 관리자 운영을 다루는 서비스다.  
백엔드는 Spring Boot 기반 멀티 모듈 애플리케이션이고, 프론트엔드는 Vite + React SPA로 구성되어 있다.

## 한눈에 보기

- 주문: 온라인과 오프라인이 같은 재고를 공유한다. 결제 시 재고를 차감하고, 관리자가 24시간 안에 승인 또는 거절한다. 미처리 주문은 자동 환불된다.
- 예약: 클래스와 시간 슬롯, 정원을 관리한다. 예약금은 클래스 가격의 10%이며, 전날 00:00 이후에는 환불할 수 없고 시작 1시간 전까지만 변경할 수 있다.
- 8회권: 회원 전용이다. 결제일 기준 90일 유효하며, 환불 시 미래 예약을 자동 취소한다.
- 인증: 회원은 `HG_SESSION`, 관리자는 Bearer 세션, 비회원은 `X-Access-Token`을 사용한다.

## 운영 환경

- 운영 주소: `https://d36l7yi27358tl.cloudfront.net/`
- 주요 경로: `/products`, `/bookings/new`, `/passes/purchase`, `/my`, `/guest`, `/admin`

### 배포 아키텍처

현재 운영 환경은 `CloudFront + S3 + ALB + ECS Fargate + RDS MySQL + Redis` 구조다.

```text
사용자 브라우저
  -> CloudFront
       -> S3 (프론트엔드 정적 파일)
       -> /api/* -> ALB
                     -> ECS Fargate (Spring Boot 앱)
                          -> RDS MySQL
                          -> Redis
```

- CloudFront가 단일 진입점 역할을 한다.
- 프론트엔드 정적 파일은 S3에서 제공한다.
- `/api/*` 요청은 ALB를 거쳐 ECS Fargate의 Spring Boot 애플리케이션으로 전달한다.
- 운영 데이터는 RDS MySQL에 저장하고, 세션과 캐시는 Redis를 사용한다.

### 배포 파이프라인

`main` 브랜치에 push되면 GitHub Actions가 자동으로 배포를 수행한다.

- 프론트엔드: `frontend` 빌드 -> S3 동기화 -> CloudFront 캐시 무효화
- 백엔드: `:bootstrap:bootJar` -> Docker 이미지 빌드 -> ECR 업로드 -> ECS 서비스 업데이트

관련 문서:
- [docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md](docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md)
- [docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md](docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md)
- [docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md](docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md)

## 저장소 구조

이 저장소는 포트/어댑터 구조를 따르는 6개 백엔드 모듈과 별도 프론트엔드 앱으로 구성된다.

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
- 일반 조회와 저장은 JPA를 사용하고, 관리자 검색과 대시보드 집계는 MyBatis를 사용한다.

## 기술 스택

- 백엔드: Spring Boot 4.0.2, Java 21, Gradle
- 프론트엔드: Vite, React 19, TypeScript
- 데이터베이스: MySQL 8, Flyway
- 세션과 캐시: Redis, Spring Session
- 모니터링: Actuator, Prometheus, Grafana, Sentry
- 테스트: JUnit 5, Testcontainers, Playwright

## 로컬 개발

### 요구사항

- Java 21
- Node.js 20+
- Docker / Docker Compose

### 빠른 시작

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

### 로컬 기본 동작

- `local` 프로필에서는 DB가 비어 있으면 기본 클래스 3종을 자동 생성한다.
- 운영 환경에서는 기본 클래스가 자동 생성되지 않으므로, 관리자 화면에서 클래스를 먼저 등록하고 클래스 목록 드롭다운에서 선택해 슬롯을 생성한다.
- `local` 프로필에서는 관리자 계정이 없으면 `admin / admin1234`를 자동 생성한다.
- 로컬과 개발 환경에서는 `X-Admin-Key: dev-admin-key`를 사용할 수 있다.
- `prod`가 아닌 환경에서는 실제 알림 발송 대신 테스트용 발송기를 사용한다.

### Docker로 전체 스택 실행

로컬 Docker 구성은 `nginx + app + mysql + redis + monitoring` 조합이다.  
프론트엔드 정적 파일은 `frontend/dist`를 nginx가 서빙하므로 먼저 빌드가 필요하다.

```bash
cd frontend
npm install
npm run build
cd ..
docker compose up -d --build
```

- `http://localhost`: nginx + 프론트엔드 정적 파일 + `/api` 프록시
- `http://localhost:9090`: Prometheus
- `http://localhost:9093`: Alertmanager
- `http://localhost:3001`: Grafana

### 최초 관리자 계정 생성

- `local` 프로필에서는 기본 관리자 계정이 자동 생성되므로 별도 작업이 필요 없다.
- `local`이 아닌 환경에서 관리자 계정이 없으면 `ADMIN_SETUP_TOKEN`을 주입한 뒤 `/api/v1/admin/setup`으로 최초 계정을 만든다.
- 계정 생성 가능 여부는 `/api/v1/admin/setup/status`에서 확인할 수 있다.
- 최초 관리자 계정 생성이 끝나면 `ADMIN_SETUP_TOKEN`은 제거한다.

## 자주 쓰는 명령어

### 백엔드

- 전체 빌드: `./gradlew build`
- 전체 테스트: `./gradlew test`
- 정책 테스트: `./gradlew :application:policyTest`
- 통합 테스트: `./gradlew --no-daemon :application:useCaseTest`
- 앱 실행: `./gradlew :bootstrap:bootRun`

### 프론트엔드

- 개발 서버: `cd frontend && npm run dev`
- 프로덕션 빌드: `cd frontend && npm run build`
- E2E 브라우저 설치: `cd frontend && npm run e2e:install`
- E2E 실행: `cd frontend && npm run e2e`

## 테스트 메모

- `@UseCaseIT`는 MySQL/Redis Testcontainers와 고정 `Clock`을 사용한다.
- Playwright 실행 전 백엔드는 `http://localhost:8080`에서 실행 중이어야 한다.
- Playwright 관리자 기본 로그인은 `admin / admin1234`다. 필요하면 환경 변수로 덮어쓴다.

## 문서 진입점

- 요구사항 기준 문서: [docs/PRD/0001_기준_스펙/spec.md](docs/PRD/0001_기준_스펙/spec.md)
- API 계약: [docs/PRD/0004_API_계약/spec.md](docs/PRD/0004_API_계약/spec.md)
- 설계 결정: [docs/ADR](docs/ADR/)
- 배경 메모와 검토 기록: [docs/Idea](docs/Idea/)

`docs/Idea`는 배경 메모다. 현재 동작과 운영 기준은 PRD와 ADR을 먼저 본다.

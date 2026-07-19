# happyGallery

`happyGallery`는 오프라인 공방의 상품 주문, 클래스 예약, 8회권, 관리자 운영을 다루는 서비스다.
백엔드는 Spring Boot 멀티 모듈 애플리케이션이고, 프론트엔드는 Vite + React SPA다.

## 한눈에 보기

| 사용자 | 주요 기능 |
| --- | --- |
| 비회원 | 휴대폰 인증 기반 주문/예약 생성, 토큰 기반 조회, 회원가입 후 기존 이력 가져오기 |
| 회원 | SMS 소유 확인 회원가입, 상품 주문, 클래스 예약, 8회권 구매/사용, 장바구니, 알림함, 마이페이지 |
| 관리자 | 상품/클래스/슬롯 관리, 주문 승인/거절/배송/픽업, 예약 운영, 환불 재시도, Q&A/문의 답변 |

- 주문/예약/8회권은 `POST /api/v1/payments/prepare` -> `POST /api/v1/payments/confirm` 표준 결제 경로를 사용한다. confirm은 선점·PG 승인·도메인 저장 트랜잭션을 분리하고 Toss 멱등키와 실패 보상 환불을 사용한다.
- 8회권 사용 예약은 운영자가 일정을 일괄 배정하지 않는다. 회원이 예약 가능 슬롯을 직접 선택해 한 회차씩 예약하고 크레딧 1회를 사용한다.
- 환불은 요청 이력을 먼저 커밋한 뒤 PG를 호출한다. 실행 유실·일시 실패·결과 불명 상태는 최초 멱등키를 유지한 채 매분 복구하며, PG 호출 실행기는 제한 큐와 즉시 거절 정책으로 보호한다.
- 회원은 `HG_SESSION`, 관리자는 Bearer 세션, 비회원은 `X-Access-Token`을 사용한다.
- 브라우저의 비관리자 상태 변경 요청은 `XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더로 CSRF를 방어한다.
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
- `prod`가 아닌 환경에서는 실제 알림·인증 SMS·결제 대신 테스트용 발송기와 `FakePaymentProvider`를 사용한다.
- k3s 운영 배포의 Prometheus 경보는 내부 Alertmanager를 거쳐 저장소 밖 Secret으로 주입한 외부 HTTPS webhook에 전달한다. 노트북 자체 장애 감시는 별도 외부 uptime 서비스가 필요하다.
- `prod`가 아닌 환경의 Google/Naver 로그인은 외부 인증 화면 없이 테스트용 콜백으로 즉시 돌아온다.
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

- 백엔드: Spring Boot 4.0.2, Spring Security, Java 21, Gradle
- 프론트엔드: Vite, React 19, TypeScript
- 데이터베이스: MySQL 8, Flyway
- 세션과 캐시: Redis, Spring Session
- 인프라 목표: 단일 노트북 k3s, Kubernetes Ingress, MySQL 영속 볼륨, cluster 내부 Redis
- 로컬 개발·복구 진단: Docker Compose, Nginx
- 모니터링: Actuator, Prometheus, Grafana, Sentry
- 테스트: JUnit 5, Testcontainers, Spring REST Docs, Playwright

### 프론트엔드 디자인 기준

- 햇빛이 드는 공방을 중심 이미지로 삼고 한지색, 점토색, 잎색을 기본 팔레트로 사용한다.
- 본문은 Pretendard, 전시 제목과 브랜드 표기는 Gowun Batang 계열을 사용한다.
- 공통 색상과 컴포넌트 변수는 `frontend/src/styles/_variables.scss`, 화면 스타일은 `frontend/src/styles/global.scss`에서 관리한다.
- 홈 히어로 이미지는 `frontend/src/assets/studio-hero.jpg`를 사용하며, 모바일 구도와 `prefers-reduced-motion`을 함께 지원한다.

## 운영/배포

AWS 운영 배포는 폐기했다. 목표 운영 환경은 소유한 단일 노트북의 단일 노드 k3s다.

```text
브라우저 -> DNS/공유기/방화벽 -> k3s Ingress(TLS)
                                  -> /api/* -> Spring Boot -> cluster 내부 MySQL/Redis
                                  -> 그 외   -> React SPA
```

- 프론트엔드와 API는 같은 origin으로 제공하고 외부에는 ingress의 HTTP/HTTPS 포트만 연다.
- 애플리케이션, MySQL, Redis와 관리·모니터링 포트는 외부에 직접 공개하지 않는다.
- Docker Compose는 로컬 개발, 통합 검증과 복구 진단용이다. 현재 `local` 프로필과 개발 기본값을 사용하므로 운영 배포 기준이 아니다.
- [`deploy/k3s`](deploy/k3s/README.md)에 namespace, ingress/TLS, MySQL PVC, 비공개 Actuator/Prometheus, secret 주입, 불변 이미지 import, rollout·rollback, 암호화 백업·복원 절차를 둔다.
- 현재 공개 운영 주소와 자동 배포 workflow는 없다. 실제 노트북에서 DNS·공유기·방화벽·TLS·복원 훈련과 핵심 사용자 흐름을 검증하기 전에는 운영 중으로 간주하지 않는다.

운영 목표와 불변 조건은 [ADR-0037 자가 호스팅 배포 토폴로지 기준](docs/ADR/0037_자가_호스팅_배포_토폴로지_기준/adr.md)을 따른다. 이전 AWS 구조와 배포 설정은 [Idea-0028](docs/Idea/0028_CloudFront_S3_ALB_배포_구조/idea.md), [Idea-0029](docs/Idea/0029_GitHub_Actions_CI_CD_배포_Fargate/idea.md), [Idea-0039](docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md)에 역사 기록으로 남긴다.

## 주요 환경 변수

| 이름 | 위치 | 설명 |
| --- | --- | --- |
| `TOSS_SECRET_KEY` | 백엔드 `prod` | Toss Payments secret key |
| `VITE_TOSS_CLIENT_KEY` | 프론트 빌드 | Toss SDK client key |
| `PAYMENT_EXECUTOR_POOL_SIZE` | 백엔드 | PG 호출 실행 스레드 수, 기본 `4` |
| `PAYMENT_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | PG 호출 대기열 크기, 기본 `20` |
| `NOTIFICATION_EXECUTOR_POOL_SIZE` | 백엔드 | Alimtalk·SMS timeout 보호 실행 스레드 수, 기본 `6` |
| `NOTIFICATION_EXECUTOR_QUEUE_CAPACITY` | 백엔드 | Alimtalk·SMS timeout 보호 대기열 크기, 기본 `20` |
| `NOTIFICATION_TIMEOUT_MILLIS` | 백엔드 | 알림 외부 호출 전체 TimeLimiter, 기본 `5000` |
| `ALIMTALK_TIMEOUT_MILLIS` / `SMS_TIMEOUT_MILLIS` | 백엔드 `prod` | NHN 응답 대기 상한, 기본 `2000` (연결 풀 `500` + 연결 `1000`보다 바깥 TimeLimiter가 크게 유지돼야 함) |
| `PASS_TOTAL_PRICE` | 백엔드 | 8회권 결제 금액 |
| `GOOGLE_OAUTH_CLIENT_ID` | 백엔드 `prod` | Google 로그인 client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | 백엔드 `prod` | Google 로그인 client secret |
| `GOOGLE_OAUTH_REDIRECT_URI` | 백엔드 `prod` | Google에 등록한 exact backend callback URI (`https://<host>/api/v1/auth/social/callback/google`) |
| `NAVER_OAUTH_CLIENT_ID` | 백엔드 `prod` | Naver 로그인 client ID |
| `NAVER_OAUTH_CLIENT_SECRET` | 백엔드 `prod` | Naver 로그인 client secret |
| `NAVER_OAUTH_REDIRECT_URI` | 백엔드 `prod` | Naver에 등록한 exact backend callback URI (`https://<host>/api/v1/auth/social/callback/naver`) |
| `ALIMTALK_APP_KEY` | 백엔드 `prod` | NHN Cloud Alimtalk 서비스 app key |
| `ALIMTALK_SECRET_KEY` | 백엔드 `prod` | NHN Cloud Alimtalk `X-Secret-Key` 값 |
| `ALIMTALK_SENDER_KEY` | 백엔드 `prod` | NHN Cloud에 등록한 카카오 발신 프로필 키 |
| `SMS_API_KEY` | 백엔드 `prod` | NHN Cloud 일반·인증 SMS app key |
| `SMS_API_SECRET` | 백엔드 `prod` | NHN Cloud SMS API secret |
| `SMS_SENDER_NUMBER` | 백엔드 `prod` | 사전 등록한 SMS 발신 번호 |
| `RATE_LIMIT_ENABLED` | 백엔드 | 로컬 반복 검증 시 처리율 제한 off 가능 |
| `RATE_LIMIT_KEY_PREFIX` | 백엔드 | 환경별 Redis 처리율 제한 키 prefix |
| `REDIS_CONNECT_TIMEOUT` | 백엔드 | Redis 연결 대기 상한, 기본 `1s` |
| `REDIS_COMMAND_TIMEOUT` | 백엔드 | Redis 명령 대기 상한, 기본 `1s` |
| `FORWARD_HEADERS_STRATEGY` | 백엔드 `prod` | 통제된 ingress 구성 후 `native`로 설정 |
| `RATE_LIMIT_TRUST_FORWARDED` | 백엔드 | 통제된 ingress 구성 후에만 `true`로 설정 |
| `ENCRYPT_KEY` | 백엔드 `prod` | 개인정보 AES-256 키, 64자리 hex |
| `HMAC_KEY` | 백엔드 `prod` | 블라인드 인덱스 HMAC 키, 64자리 hex |
| `GUEST_TOKEN_HMAC_SECRET` | 백엔드 `prod` | 비회원 접근 토큰 서명 키 |
| `ADMIN_SETUP_TOKEN` | 백엔드 | 최초 관리자 계정 생성용 일회성 토큰 |

환경별 전체 설정은 [application.yml](bootstrap/src/main/resources/application.yml)과 [application-local.yml](bootstrap/src/main/resources/application-local.yml)을 기준으로 확인한다.

Naver 로그인 운영 등록 조건:

- Naver Developers 애플리케이션에 서비스 origin과 정확한 백엔드 콜백 URI `${서비스 origin}/api/v1/auth/social/callback/naver`를 등록한다.
- 회원 프로필의 이메일과 이름 제공 항목을 사용하도록 설정한다. 둘 중 하나가 제공되지 않으면 서비스 회원을 식별할 수 없어 로그인을 거절한다.
- 로그인 버튼은 [Naver 로그인 버튼 사용 가이드](https://developers.naver.com/docs/login/bi/bi.md)의 공식 심벌과 지정 색상을 사용한다.

## 문서 진입점

- 요구사항 기준: [docs/PRD/0001_기준_스펙/spec.md](docs/PRD/0001_기준_스펙/spec.md)
- API 계약: [docs/PRD/0004_API_계약/spec.md](docs/PRD/0004_API_계약/spec.md)
- 설계 결정: [docs/ADR](docs/ADR/)
- 배경 메모와 검토 기록: [docs/Idea](docs/Idea/)
- 회고와 트러블슈팅 기록: [docs/Retrospective](docs/Retrospective/)

`docs/Idea`는 배경 메모다. 현재 동작과 운영 기준은 PRD와 ADR을 먼저 본다.

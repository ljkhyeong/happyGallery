# happyGallery

오프라인 공방의 **온라인 쇼핑몰 + 체험 예약 시스템**.
상품 주문, 클래스 예약, 8회권 패스, 관리자 운영을 하나의 플랫폼에서 처리한다.

- **백엔드**: Spring Boot 4.0.2 / Java 21 / MySQL 8
- **프론트엔드**: Vite / React 19 / TypeScript / Bootstrap
- **구조**: Gradle 6-module (`bootstrap` · `adapter-in-web` · `adapter-out-persistence` · `adapter-out-external` · `application` · `domain`) + `frontend/` 워크스페이스

---

## 🧭 구조와 주요 라이브러리

### 백엔드

| 구분 | 구성 | 용도 |
|------|------|------|
| 백엔드 구조 | `bootstrap` / `adapter-in-web` / `adapter-out-persistence` / `adapter-out-external` / `application` / `domain` | 헥사고날 풀-스플릿 6-module 구조. 의존 방향은 `bootstrap → adapter-in-web/out-* → application → domain`. ArchUnit `LayerDependencyArchTest`로 경계를 강제한다. |
| 관측성 구조 | `monitoring/` + `docker-compose.yml` | Prometheus, Grafana, alert rule, 대시보드 설정을 로컬에서 바로 띄울 수 있게 묶어 둔다. |
| Resilience4j | `resilience4j-circuitbreaker`, `resilience4j-timelimiter` | PG 환불 호출에 circuit breaker와 timeout을 적용해 장애 전파를 줄인다. |
| Redis + Spring Session | `spring-boot-starter-data-redis`, `spring-session-data-redis` | 회원 세션(`HG_SESSION`), 관리자 Bearer 세션, 요청 제한 카운터를 Redis에 저장한다. |
| JPA + MyBatis | `spring-boot-starter-data-jpa`, `mybatis`, `mybatis-spring` | 일반 영속성은 JPA로 처리하고, 관리자 주문/예약 검색과 관리자 대시보드 집계 쿼리는 MyBatis mapper로 분리해 최적화한다. |
| HTTP 캐시 | `ShallowEtagHeaderFilter` | 공개 상품/클래스/공지 GET 응답에 `ETag`를 붙이고 `If-None-Match` 기반 `304 Not Modified`를 지원한다. |
| Flyway | `spring-boot-starter-flyway`, `flyway-mysql` | MySQL 스키마 변경을 버전으로 관리한다. |
| Spring Actuator | `spring-boot-starter-actuator` | 헬스 체크와 메트릭 엔드포인트를 제공한다. |
| Prometheus | `micrometer-registry-prometheus` | Actuator 메트릭과 `happygallery.funnel.*` 커스텀 메트릭을 Prometheus 형식으로 노출한다. |
| Grafana | Grafana provisioning + dashboard JSON | 시스템 지표, executor thread pool, 전환 퍼널 지표를 대시보드로 본다. |
| Sentry | `sentry-spring-boot-4-starter` | 서버 500 예외를 `requestId`와 함께 수집한다. |
| Testcontainers | `spring-boot-testcontainers`, `testcontainers`, `testcontainers-mysql` | `@UseCaseIT`에서 MySQL, Redis 같은 실제 의존성을 가깝게 재현한다. |

### 프론트

| 구분 | 구성 | 용도 |
|------|------|------|
| 프론트 구조 | `frontend/` (Vite + React 19 + TypeScript) | 스토어, 마이페이지, 관리자 화면을 담당한다. |
| TanStack Query | `@tanstack/react-query` | 상품, 예약, 주문, 관리자 데이터를 조회하고 캐시한다. |
| React Router | `react-router-dom` | 스토어, guest 조회, `/my`, 관리자 라우트를 관리한다. |
| Bootstrap | `bootstrap`, `react-bootstrap` | 기본 레이아웃과 공통 UI 컴포넌트를 맞춘다. |
| Sentry | `@sentry/react` | 프론트 5xx 에러와 브라우저 예외를 수집한다. |
| Playwright | `@playwright/test` | guest, member, admin 핵심 브라우저 시나리오를 자동화한다. |

### 🔄 마이그레이션 현황

- 아키텍처: 헥사고날 풀-스플릿을 마무리해 `app/infra`를 6-module(`bootstrap`, `adapter-in-web`, `adapter-out-persistence`, `adapter-out-external`, `application`, `domain`)로 나눴다. 모듈 경계는 ArchUnit `LayerDependencyArchTest`로 강제한다.
- 회원 세션: 직접 만든 세션 저장 로직을 걷어내고 Spring Session + Redis로 옮겼다. `HG_SESSION` 쿠키 이름은 그대로 유지한다.
- 회원 세션 테이블: 세션을 Redis로 옮긴 뒤 DB `user_sessions` 테이블도 제거했다.
- 관리자 세션과 요청 제한: 관리자 Bearer 세션과 rate limit 카운터를 각 서버 메모리 대신 Redis에 저장한다. 여러 인스턴스가 떠 있어도 같은 제한값을 공유한다.
- 유스케이스 진입점: 회원 인증과 guest claim부터 시작해 예약 변경/취소, 8회권 만료 배치, 픽업 만료 배치도 유스케이스 인터페이스를 통해 호출하도록 정리했다.
- 인터페이스 분리: 상품, 알림, 결제, 예약, 주문 영역에서 조회/저장/외부 연동 인터페이스를 나눴다. JPA와 외부 연동 구현은 별도 구현 클래스로 둔다.
- 관리자 검색/집계: 관리자 주문/예약 검색과 매출/환불/가동률 대시보드 조회는 `adapter-out-persistence`의 MyBatis adapter + mapper로 처리한다.
- 8회권: guest 소유 8회권을 없애고 회원 전용 구매로 단일화했다. `pass_purchases.guest_id`도 제거했다.
- guest 토큰: URL query와 평문 저장을 걷어내고 `X-Access-Token` 헤더 + SHA-256 해시 저장으로 바꿨다.
- 로그: `prod`는 JSON 구조화 로그를 쓰고, `local`/`test`는 읽기 쉬운 텍스트 로그를 유지한다. 전화번호, Bearer 토큰, 세션 토큰, access token은 로그 출력 전에 마스킹한다.
- 관측성: requestId 로그만 보던 상태에서 Actuator, Prometheus, Grafana, Sentry를 붙였다. 지금은 서버 상태와 client funnel 지표를 함께 본다.
- 테스트: `@UseCaseIT`는 MySQL/Redis Testcontainers와 고정 `Clock`을 사용한다. 시간 경계와 Redis 의존 흐름을 운영과 가깝게 검증한다.
- 배치: Spring Batch로는 아직 옮기지 않았다. 현재는 커스텀 배치를 유지한다.

* 자세한 배경은 관련 ADR을 우선 보고, 아직 채택하지 않은 대안이나 보류 메모는 `docs/Idea`를 본다.

---

## 📚 문서 안내

### 🗂 현재 운영 문서

- `README.md`: 저장소 개요, 실행 방법, 문서 진입점을 모아 둔 시작 문서다.
- `docs/Idea/0039_AWS_배포_설정_베이스라인/idea.md`: AWS 운영 배포에 필요한 ECR, OIDC, GitHub Actions 설정 기준선을 한곳에 모아 둔 문서다.
- `simple-idea.md`: 작은 개선 아이디어를 `As-Is | To-Be` 표로 누적한다.
- `PRD`: 제품 요구사항과 운영 정책의 기준 문서다.
- `ADR`: 데이터 모델, 인증, 결제, 관측성, 아키텍처 같은 핵심 설계 결정을 남긴다.
- `Idea`: 아직 확정하지 않은 아이디어와 검토 메모를 둔다.
- `POC`: 실험 결과와 적용 판단 근거를 남긴다.
- `Retrospective`: 구현 후 회고와 교훈을 정리한다.
- `1Pager`: 이해관계자 공유용 요약 문서 카테고리다.
- `AGENTS.md`, `CLAUDE.md`: 에이전트 작업 규칙과 로컬 운영 메모다.

### 📐 PRD

| 문서 | 경로 | 설명 |
|------|------|------|
| [PRD-0001 ~ PRD-0004](docs/PRD/) | `docs/PRD/` | 제품 요구사항, 범위, API 계약 기준 문서 모음 |

### 🧱 ADR

| 문서 | 경로 | 설명 |
|------|------|------|
| [ADR-0001 ~ ADR-0030](docs/ADR/) | `docs/ADR/` | 데이터 모델, 인증, 운영, 테스트, 아키텍처 결정 기록 |

### 💡 Idea

| 문서 | 경로 | 설명 |
|------|------|------|
| [IDEA-0001 ~ IDEA-0039](docs/Idea/) | `docs/Idea/` | 검토 메모, 후속 아이디어, 운영 가이드 문서 모음 |

### 🧪 POC

| 문서 | 경로 | 설명 |
|------|------|------|
| [POC-0001](docs/POC/) | `docs/POC/` | 실험 결과와 적용 판단 기록 |

### 🔁 Retrospective

| 문서 | 경로 | 설명 |
|------|------|------|
| [RETRO-0001 ~ RETRO-0008](docs/Retrospective/) | `docs/Retrospective/` | 구현 후 교훈, 운영 회고, 패턴 정리 문서 모음 |

### 📄 1Pager

| 문서 | 경로 | 설명 |
|------|------|------|
| [1PAGER-README](docs/1Pager/) | `docs/1Pager/` | 한 장 요약 문서 카테고리 안내 |

문서 운영 규칙:
- 세션성 인수인계 메모와 활성 실행 계획은 README 문서 목록에서 관리하지 않는다.
- 완료된 임시 실행 계획은 `docs/1Pager`에 남기지 않는다.
- 장기 보관 가치가 있는 내용만 `docs/Idea`, `docs/1Pager`, `docs/PRD`, `docs/POC`, `docs/Retrospective`, `docs/ADR`에 남긴다.

## ✅ 현재 제공 기능

- 공개 사용자 흐름
  - 상점형 홈 / 스토어 네비게이션
  - 상품 목록/상세
  - 회원가입 / 로그인 / Google 소셜 로그인
  - 제출 직전에 인증하는 예약 생성 흐름
  - 회원 전용 8회권 구매 (`/passes/purchase`, 비로그인 시 로그인 리다이렉트)
  - 상품 상세에서 회원 주문 생성
  - 비회원 주문 보조 경로 (`/orders/new`, 상품/수량 미리 채우기 지원, 주소를 직접 입력해 들어온 경우는 명시적 계속 후 진행)
  - 비회원 조회 허브 (`/guest`)
  - 비회원 예약 조회/변경/취소 (`/guest/bookings`, 조회용 보조 경로)
  - 비회원 주문 조회 (`/guest/orders`, 조회용 보조 경로)
  - 회원 마이페이지 (`내 주문`, `내 예약`, `내 8회권`, 비회원 이력 가져오기)
  - 회원 주문/예약/8회권 전체 목록 (`/my/orders`, `/my/bookings`, `/my/passes`, 검색/상태 필터/quick tab/정렬 포함)
  - 회원 예약 상세/변경/취소 (`/my/bookings/:id`)
  - 회원 장바구니 (`/cart`)에서 상품 수량 조정 후 바로 주문
  - 회원 상단 알림함에서 최근 알림 확인, 읽지 않음 개수 조회, 개별/전체 읽음 처리
  - Google 로그인/회원가입 후 `/auth/callback/google`에서 회원 세션 연결
  - 상품 상세 Product Q&A 조회/회원 작성/비밀글 비밀번호 확인
  - 회원 1:1 문의 작성/목록 조회 (`/my/inquiries`)
  - 회원 마이페이지에서 같은 전화번호의 비회원 이력 가져오기 (휴대폰 재인증 후 claim)
  - 비회원 성공 화면에서 회원가입/로그인 후 `/my` claim으로 바로 이어지는 CTA
  - 로그인/회원가입 페이지에서 `redirect`, `claim`, `name`, `phone` 문맥 유지
- 관리자 흐름
  - 최초 관리자 계정 셋업 (`/api/v1/admin/setup`, `/api/v1/admin/setup/status`)
  - 상품 등록/조회
  - 슬롯 생성/비활성화
  - 예약 목록 조회/검색/노쇼 처리
  - 주문 커서 목록 조회/검색/승인/거절/제작 재개/제작 완료/지연/배송/픽업 관리
  - 주문 결정 이력 조회
  - 매출 요약/환불 현황/주문 상태/상위 상품/일별 매출/예약 가동률 관리자 대시보드 조회
  - 8회권 만료/환불
  - 환불 실패 조회/재시도
  - 상품 Q&A 답변 관리
  - 1:1 문의 답변 관리

프론트 주요 경로:

- `/products`
- `/products/:id`
- `/notices/:id`
- `/login`
- `/signup`
- `/auth/callback/google`
- `/my`
- `/cart`
- `/my/orders`
- `/my/orders/:id`
- `/my/bookings`
- `/my/bookings/:id`
- `/my/passes`
- `/my/inquiries`
- `/my/inquiries/new`
- `/bookings/new`
- `/guest`
- `/guest/bookings`
- `/passes/purchase`
- `/orders/new`
- `/guest/orders`
- `/admin`

## 🏗 저장소 구조

- `bootstrap/`
  - `@SpringBootApplication` 엔트리, `application*.yml`, `db/migration` Flyway 스크립트, `logback-spring.xml`, `bootstrap.config.*`, `bootstrap.logging.*` (마스킹 layout)
- `adapter-in-web/`
  - 컨트롤러, 필터(`RequestIdFilter`/`RateLimitFilter`/`CustomerAuthFilter`/`AdminAuthFilter`), `@CustomerUserId`/`@AdminUserId` resolver, 웹 전용 properties
- `adapter-out-persistence/`
  - JPA Repository, MyBatis mapper/adapter, persistence 전용 config
- `adapter-out-external/`
  - 결제(PG + CircuitBreaker), 알림(Kakao/SMS), Google OAuth, 외부 HTTP pool, 관리자 Redis 세션 저장소
- `application/`
  - 유스케이스 입력(`port.in`)/출력(`port.out`) 인터페이스, application service, batch, application 공용 properties, `java-test-fixtures` 기반 `support/**` 공용 테스트 인프라
- `domain/`
  - 엔티티, 상태 전이, 정책 enum, 도메인 예외 등 핵심 비즈니스 규칙
- `monitoring/`
  - Prometheus scrape/alert rule, Grafana datasource/provisioning, 대시보드 JSON
- `frontend/`
  - Vite + React + TypeScript 프론트엔드

의존 방향은 `bootstrap → adapter-in-web / adapter-out-* → application → domain` 한 방향이며, ArchUnit `application/src/test/java/com/personal/happygallery/policy/LayerDependencyArchTest.java` 가 회귀를 막는다.

## 🚀 로컬 실행

### 1. 요구사항

- Java 21
- Node.js 20+
- Docker / Docker Compose

### 2. 주요 환경 변수 / 설정

- DB: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD`, `DB_HIKARI_CONNECTION_TIMEOUT_MS`, `DB_CONNECT_TIMEOUT_MS`, `DB_SOCKET_TIMEOUT_MS`, `DB_QUERY_TIMEOUT_MS`, `DB_LOCK_WAIT_TIMEOUT_SECONDS`
- 관리자 local/dev: `ADMIN_API_KEY`, `ADMIN_ENABLE_API_KEY_AUTH`, `ADMIN_SETUP_TOKEN`
- 결제: `PAYMENT_TIMEOUT_MILLIS`, `PAYMENT_CB_*`
- 트랜잭션: `TX_DEFAULT_TIMEOUT`
- 요청 제한 / Actuator: `RATE_LIMIT_TRUST_FORWARDED`, `ACTUATOR_HEALTH_SHOW_DETAILS`
- Google OAuth: `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GOOGLE_OAUTH_TIMEOUT_MILLIS`, `GOOGLE_OAUTH_CONNECT_TIMEOUT_MILLIS`, `GOOGLE_OAUTH_ACQUIRE_TIMEOUT_MILLIS`, `GOOGLE_OAUTH_MAX_CONNECTIONS`, `GOOGLE_OAUTH_KEEP_ALIVE_MILLIS`
- 필드 암호화: 비local 프로필은 `app.field-encryption.encrypt-key`, `app.field-encryption.hmac-key`를 64자리 hex 값으로 반드시 주입
- 알림: `KAKAO_API_KEY`, `KAKAO_SENDER_KEY`, `KAKAO_TIMEOUT_MILLIS`, `KAKAO_CONNECT_TIMEOUT_MILLIS`, `KAKAO_ACQUIRE_TIMEOUT_MILLIS`, `KAKAO_MAX_CONNECTIONS`, `KAKAO_KEEP_ALIVE_MILLIS`, `SMS_API_KEY`, `SMS_API_SECRET`, `SMS_SENDER_NUMBER`, `SMS_TIMEOUT_MILLIS`, `SMS_CONNECT_TIMEOUT_MILLIS`, `SMS_ACQUIRE_TIMEOUT_MILLIS`, `SMS_MAX_CONNECTIONS`, `SMS_KEEP_ALIVE_MILLIS`
- Grafana: `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`
- Playwright: `PLAYWRIGHT_ADMIN_USERNAME`, `PLAYWRIGHT_ADMIN_PASSWORD`, `PLAYWRIGHT_BACKEND_URL`

기본 프로필은 `local`이다.
- `local` 프로필은 샘플 필드 암호화 키를 기본 제공하지만, `dev`/`prod`는 직접 설정해야 한다.
- `admin_user`가 비어 있을 때만 `ADMIN_SETUP_TOKEN`을 잠깐 주입해 `/api/v1/admin/setup`으로 최초 관리자 계정을 만들 수 있다. 완료 후에는 토큰을 제거한다.
- timeout 기준선 기본값은 `frontend 35s > nginx read 30s > transaction 10s > DB query 5s > lock wait 3s > DB/Hikari acquire 2s` 순서를 따른다. 상세 원칙은 `ADR-0030`을 따른다.
- ingress keep-alive 기준선은 `client -> nginx keepalive_timeout 15s`, `nginx -> app` upstream keep-alive 활성화로 시작한다. caller가 먼저 연결을 버리고 callee가 나중에 닫도록 유지해 stale connection 재사용 가능성을 줄인다. 상세 원칙은 `ADR-0030`을 따른다.
- `prod` 프로필의 외부 알림/Google OAuth `RestClient`는 downstream별 Apache HttpClient 5 커넥션 풀을 사용한다. 기본값은 `acquire 1s`, `connect 2s`, `read 5s`, `keep-alive 30s`이며, 알림 풀은 최대 20개, Google OAuth 풀은 최대 10개다.

### 3. 백엔드 실행 방식

MySQL, Redis만 Docker로 띄우고 앱은 로컬에서 실행:

```bash
docker compose up -d mysql redis
docker compose stop app
./gradlew :bootstrap:bootRun
```

- 이미 `docker compose up -d --build`로 앱 컨테이너가 떠 있다면, 로컬 `bootRun` 전에 `docker compose stop app`으로 8080 충돌을 먼저 해소한다.
- 회원 세션, 관리자 세션, 요청 제한 카운터가 모두 Redis를 쓰므로 로컬 `bootRun`에도 Redis(`localhost:6379`)가 필요하다.
- `local` 프로필로 `bootRun`하면 `classes` 테이블이 비어 있을 때 기본 클래스 3종(향수/우드/니트)을 자동으로 넣는다.
- 알림 발송은 `!prod`에서 fake sender를, `prod`에서 카카오 알림톡/NHN SMS sender를 사용한다.

MySQL + 앱 컨테이너를 함께 실행:

```bash
docker compose up -d --build
```

- `nginx`: `http://localhost` (frontend `dist` 정적 파일 + `/api` 리버스 프록시)
- `prometheus`: `http://localhost:9090`
- `grafana`: `http://localhost:3001` (`GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`; 사용자명 기본값은 `admin`)

백엔드 헬스 체크:

```bash
curl http://localhost:8080/actuator/health
```

### 4. 프론트 실행

```bash
cd frontend
npm install
npm run dev
```

- 프론트 개발 서버: `http://localhost:3000`
- `/api` 요청은 Vite proxy로 `http://localhost:8080`에 연결된다.

## 🧪 빌드와 검증

### 백엔드

- 전체 빌드: `./gradlew build`
- 전체 테스트: `./gradlew test`
- 정책 테스트: `./gradlew :application:policyTest`
- 유스케이스 통합 테스트: `./gradlew --no-daemon :application:useCaseTest`
- 단일 테스트 예시:
  - `./gradlew --no-daemon :application:test --tests com.personal.happygallery.application.order.OrderApprovalUseCaseIT`
- `@UseCaseIT`는 MySQL/Redis Testcontainers와 함께 고정 `Clock`(Asia/Seoul)을 써서 시간 경계 테스트가 벽시계에 흔들리지 않도록 한다.

### 프론트

- 프로덕션 빌드: `cd frontend && npm run build`
- E2E 브라우저 테스트 설치: `cd frontend && npm run e2e:install`
- E2E 브라우저 테스트 실행: `cd frontend && npm run e2e`

E2E 참고:
- Playwright는 `frontend/playwright.config.ts` 기준으로 동작한다.
- Vite dev server는 Playwright가 직접 띄우거나 기존 `localhost:3000`을 재사용한다.
- 백엔드는 별도로 `http://localhost:8080`에서 실행 중이어야 한다.
- 테스트 파일은 사용자 여정 기준으로 나뉘어 있다.
- 현재 파일은 `admin-product-order.smoke.spec.ts`, `guest-booking-pass.smoke.spec.ts`, `member-self-service.smoke.spec.ts`, `guest-claim-onboarding.smoke.spec.ts`다.
- 시나리오 번호(`P8-1`~`P8-9`)는 유지하므로 `--grep "P8-8"` 같은 실행 방식도 그대로 쓸 수 있다.
- 관리자 보조 API 호출은 `POST /api/v1/admin/auth/login`으로 얻은 Bearer 토큰을 사용한다.
- 로컬 `bootRun`은 `classes` 테이블이 비어 있으면 기본 클래스를 자동 생성하므로 clean DB에서도 예약/8회권 시나리오를 바로 돌릴 수 있다.
- 시나리오 5(`환불 실패 -> 재시도`)는 local 전용 실패 주입 API(`/api/v1/admin/dev/payment/refunds/fail-next`)로 자동화했다. 요청 바디에 `orderId`를 넣으면 특정 주문만 대상으로 잡을 수 있다.
- Playwright 관리자 로그인 기본값은 `admin` / `admin1234`이며, 필요하면 `PLAYWRIGHT_ADMIN_USERNAME`, `PLAYWRIGHT_ADMIN_PASSWORD`로 덮어쓴다.
- 백엔드 기준 URL을 바꾸려면 `PLAYWRIGHT_BACKEND_URL`을 사용한다.
- 현재 브라우저 테스트 범위는 admin/order 1·4·5, guest booking/pass 2·3, member self-service 6·7, guest claim/onboarding 8·9다. `P8-7`은 회원 예약 상세/변경/취소, `P8-8`은 guest claim, `P8-9`는 회원가입 후 claim 모달 자동 진입까지 포함한다.
- `/bookings/new`는 첫 화면에서 인증하지 않고 제출 직전에 인증 선택 단계를 연다.
- `/passes/purchase`는 비로그인 사용자를 로그인으로 보내고, 로그인한 회원만 구매를 진행한다.
- 상품 상세의 `비회원 주문하기`는 `/orders/new?productId=&qty=`로 이동해 선택한 상품과 수량을 미리 채운다.
- 비회원 진입 허브는 `/guest`다. 기본 비회원 조회 경로는 `/guest/orders`, `/guest/bookings`이며 생성 후 확인용 보조 경로로 유지한다.

## API/운영 메모

- 표준 API 경로는 `/api/v1/**`다.
- 레거시 무버전 경로도 일부 남아 있지만, 문서와 테스트는 `/api/v1/**`를 기준으로 본다.
- 관리자 화면은 사용자명/비밀번호로 로그인한 뒤 Bearer 토큰으로 동작한다. local/dev 보조 호출은 `X-Admin-Key` 폴백을 쓸 수 있다.
- 주문 승인/거절/제작 재개/제작 완료 이력의 adminId는 Bearer 세션에서 가져온다. API Key 폴백 경로는 adminId가 null일 수 있다.
- 배송 준비/출발/완료 전이와 주문 결정 이력 조회도 `/api/v1/admin/orders/**` 아래에서 같은 Bearer 세션 기준으로 동작한다.
- 회원 UI는 `/my`, `/my/orders`, `/my/bookings`, `/my/passes`를 쓴다. 백엔드는 `/api/v1/me/**`로 동작한다.
- `/api/v1/me/guest-claims/{preview,verify,claim}` 로 같은 전화번호의 guest 주문/예약을 회원 계정으로 가져올 수 있다.
- guest 주문/예약 성공 화면의 회원가입/로그인 CTA는 `/my?claim=1` 로 이어지고, claim 모달을 자동으로 연다.
- 로그인/회원가입 페이지는 `redirect`, `claim`, `name`, `phone` query를 유지해 이전 문맥을 잃지 않는다.
- 비회원 조회는 여전히 토큰 기반(`bookingId + token`, `orderId + token`)이다. 프론트 기본 경로는 `/guest/orders`, `/guest/bookings`다.
- 상품 상세에서 비회원 주문으로 넘어갈 때는 `/orders/new`가 `productId`, `qty` query를 받아 초기 주문 항목을 채운다. query 없이 직접 열면 계속 버튼을 한 번 더 눌러야 수동 다중 상품 주문을 진행할 수 있다.
- 현재 운영 권장안은 `/guest` 허브, 기본 비회원 조회 경로, `/orders/new` 직접 진입 확인 단계를 유지하는 것이다. 회원 경로를 안정화한 뒤 비회원 보조 경로를 줄일지 다시 결정한다.
- `/api/v1/monitoring/client-events` 는 guest/member 전환 이벤트를 받아 `[client-monitoring]` 로그를 남긴다. 지금은 `/guest` 유입, `/orders/new` 직접 진입 뒤 계속, 회원 전환 CTA, `/my` claim 모달 오픈, claim 완료를 추적한다.

## 브랜치 흐름

- 작업 브랜치에서 변경 수행
- 먼저 `codexReview`로 반영해 통합 확인
- 이후 `codexReview -> main` PR 생성 및 머지
- 구현 변경 시 관련 문서도 함께 갱신

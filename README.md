# happyGallery

오프라인 공방의 **온라인 쇼핑몰 + 체험 예약 시스템**.
상품 주문, 클래스 예약, 8회권 패스, 관리자 운영을 하나의 플랫폼에서 처리한다.

- **백엔드**: Spring Boot 4.0.2 / Java 21 / MySQL 8
- **프론트엔드**: Vite / React 19 / TypeScript / Bootstrap
- **구조**: Gradle 멀티 모듈(`app` · `domain` · `infra` · `common`) + `frontend/` 워크스페이스

---

## 🧭 구조와 주요 라이브러리

### 백엔드

| 구분 | 구성 | 용도 |
|------|------|------|
| 백엔드 구조 | `app` / `domain` / `infra` / `common` | 진입점, 도메인 규칙, 외부 연동, 공통 유틸을 분리한 멀티 모듈 구조 위에서 `port/in`, `port/out`, adapter를 도입하는 점진적 헥사고날 전환 진행 중 |
| 관측성 구조 | `monitoring/` + `docker-compose.yml` | Prometheus scrape, Grafana provisioning, alert rule, 대시보드 JSON을 로컬 운영 스택으로 묶음 |
| Resilience4j | `resilience4j-circuitbreaker`, `resilience4j-timelimiter` | PG 환불 외부 호출에 CircuitBreaker + TimeLimiter를 적용해 장애 전파를 줄임 |
| Redis + Spring Session | `spring-boot-starter-data-redis`, `spring-session-data-redis` | `HG_SESSION`, 관리자 Bearer 세션, Redis 기반 rate limit 저장소를 함께 운영 |
| Flyway | `spring-boot-starter-flyway`, `flyway-mysql` | MySQL 스키마 변경을 버전 관리하고 환경 간 DB 상태를 일관되게 맞춤 |
| Spring Actuator | `spring-boot-starter-actuator` | `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus` 운영 엔드포인트 제공 |
| Prometheus | `micrometer-registry-prometheus` | Actuator 메트릭과 `happygallery.funnel.*` 커스텀 메트릭을 scrape 가능한 포맷으로 노출 |
| Grafana | Grafana provisioning + dashboard JSON | 시스템 메트릭, Tomcat/커스텀 executor thread pool, 제품 전환 퍼널 지표를 대시보드로 시각화 |
| Sentry | `sentry-spring-boot-4-starter` | 서버 500 예외를 requestId 태그와 함께 캡처 |
| Testcontainers | `spring-boot-testcontainers`, `testcontainers`, `testcontainers-mysql` | `@UseCaseIT`에서 MySQL/Redis 등 실제에 가까운 통합 환경을 테스트로 재현 |

### 프론트

| 구분 | 구성 | 용도 |
|------|------|------|
| 프론트 구조 | `frontend/` (Vite + React 19 + TypeScript) | 스토어/마이페이지/관리자 UI와 브라우저 흐름 구현 |
| TanStack Query | `@tanstack/react-query` | 상품/예약/주문/관리자 데이터를 조회·캐시하고 mutation 후 invalidate를 처리 |
| React Router | `react-router-dom` | 스토어, guest 조회, `/my`, 관리자 라우트를 구성하고 브라우저 내비게이션을 관리 |
| Bootstrap | `bootstrap`, `react-bootstrap` | 스토어/관리자 화면의 기본 UI 레이아웃과 컴포넌트 스타일링 |
| Sentry | `@sentry/react` | 프론트 API 5xx 에러와 브라우저 예외를 캡처 |
| Playwright | `@playwright/test` | guest/member/admin 주요 브라우저 smoke 시나리오를 자동화 |

### 🔄 마이그레이션 현황

- 백엔드 아키텍처: 기존 `app` / `domain` / `infra` / `common` 멀티 모듈 구조는 유지하면서, `app` 내부에 `port/in`, `port/out`과 adapter 경계를 점진적으로 도입하는 헥사고날 전환을 진행 중이다.
- 인증/세션: 회원 인증은 직접 세션 저장 구현에서 Spring Session + Redis 기반 `HG_SESSION`으로 전환했고, `CustomerAuthFilter`는 Spring Session filter 이후 사용자 ID를 읽는 구조로 정리했다.
- 운영 세션/레이트리밋: 관리자 Bearer 세션 저장소와 `RateLimitFilter`도 인메모리/프로세스 로컬 상태에서 Redis 기반 저장소로 옮겨 다중 인스턴스 환경에 맞췄다.
- 유스케이스 경계: `customer auth + guest claim` persistence pilot 이후 booking cancel/reschedule, pass purchase/expiry batch, pickup expire batch 등에서 controller/batch 진입점이 `UseCase` 포트를 통해 들어오도록 확장했다.
- 포트/어댑터 확산: product, notification, payment, booking, order 도메인에 reader/store/external port를 도입하고, JPA/외부 연동 구현은 adapter로 분리하는 방향으로 수렴 중이다.
- guest access token: query param/평문 저장 방식에서 `X-Access-Token` 헤더 + SHA-256 해시 저장 방식으로 전환해 로그/Referer 노출과 원문 저장 위험을 줄였다.
- 로깅 포맷: 운영(`prod`) 로그는 `LogstashEncoder` 기반 JSON 구조화 로그로 전환했고, 비운영(`!prod`)은 로컬 가독성을 위해 텍스트 로그를 유지한다.
- 관측성: requestId 중심 로그만 두던 상태에서 Actuator + Prometheus + Grafana + Sentry + client funnel metric까지 붙여 운영 가시성을 단계적으로 보강했다.
- 테스트/실행 환경: 순수 로컬 의존에 가까웠던 통합 검증을 Testcontainers(MySQL/Redis) 기반 `@UseCaseIT`로 정리해 실제 운영 환경과 더 가까운 테스트 경로를 확보했다.
- 배치: Spring Batch 전면 마이그레이션은 아직 하지 않았고, 현재는 커스텀 배치를 유지하면서 운영 배치/수동 트리거/배치 결과 구조를 보강하는 방향을 택했다.

* 마이그레이션 배경 등 상세 내용은 관련 ADR을 우선 확인하고, 아직 채택 전 대안 비교나 보류 항목은 `docs/Idea`를 확인.

---

## 📚 문서 안내

### 🗂 현재 운영 문서

- `README.md`: 저장소 개요, 실행 방법, 문서 진입점과 현재 사용하는 주요 라이브러리를 정리한다.
- `simple-idea.md`: 작은 개선/정리 아이디어를 `As-Is` / `To-Be` 두 열 표로 한 문장씩 누적한다.
- `PRD`: 제품 요구사항과 운영 정책의 기준 문서다. 기능 계약이나 정책 변경 시 먼저 맞춘다.
- `ADR`: 데이터 모델, 상태 전이, 인증, 결제, 관측성, 헥사고날 전환 같은 핵심 설계 결정을 남긴다.
- `Idea`: 정식 요구사항으로 확정하지 않은 아이디어, 향후 검토 메모, PRD 밖에서 관리하는 엔지니어링 가이드를 보관한다.
- `POC`: 실제 실험 결과와 적용 판단 근거를 남긴다.
- `Retrospective`: 지나온 변경 흐름을 되짚어 얻은 교훈과 회고를 남긴다.
- `1Pager`: 이해관계자 공유용 요약 문서 카테고리다.
- `AGENTS.md`, `CLAUDE.md`: 에이전트별 작업 규칙과 로컬 운영 메모다.

### 📐 PRD

| 문서 | 경로 | 설명 |
|------|------|------|
| [Core MVP Specification](docs/PRD/0001_spec/spec.md) | `docs/PRD/0001_spec/` | 전체 시스템의 기능 요구사항 정의서 |
| [Member Store Transition](docs/PRD/0002_member_store_transition/spec.md) | `docs/PRD/0002_member_store_transition/` | 회원 인증·스토어 전환 차기 요구사항 |
| [Out Of Scope](docs/PRD/0003_out_of_scope/scope.md) | `docs/PRD/0003_out_of_scope/` | 초기 버전에서 명시적으로 제외하는 범위 |
| [API Contract Baseline](docs/PRD/0004_api_contract/spec.md) | `docs/PRD/0004_api_contract/` | 요청/응답 예시, 에러 포맷, v1 API 계약 기준 문서 |

### 🧱 ADR

| 문서 | 경로 | 설명 |
|------|------|------|
| `ADR-0001` ~ `ADR-0026` | `docs/ADR/` | 데이터 모델, 상태 전이, 결제, 인증, 운영, 테스트 기준, 헥사고날 전환 등 기술 결정 |

### 💡 Idea

| 문서 | 경로 | 설명 |
|------|------|------|
| [JSON + Generated Column](docs/Idea/0001_json-generated-column-consideration/idea.md) | `docs/Idea/0001_json-generated-column-consideration/` | 가변 속성 저장 패턴 검토 |
| [Bulkhead (Resilience4j)](docs/Idea/0002_bulkhead-resilience4j-consideration/idea.md) | `docs/Idea/0002_bulkhead-resilience4j-consideration/` | 외부 호출 격리 전략 검토 |
| [테스트 전략 및 assertion 작성 규칙](docs/Idea/0003_test-strategy-and-assertion-guidelines/idea.md) | `docs/Idea/0003_test-strategy-and-assertion-guidelines/` | PRD에서 분리한 테스트 철학, 최소 세트, `SoftAssertions.assertSoftly` 규칙 |
| [관리자 인증 세션 확장 검토](docs/Idea/0004_admin-auth-session-scaling/idea.md) | `docs/Idea/0004_admin-auth-session-scaling/` | 인메모리 관리자 세션의 수평 확장 시 대안 비교 메모 |
| [Guest Token Signed Expiry 전환](docs/Idea/0005_guest-token-signed-expiry/idea.md) | `docs/Idea/0005_guest-token-signed-expiry/` | guest access token의 만료·서명 방식 후속 개선 메모 |
| [local/dev 지원 기능 경계](docs/Idea/0009_local-dev-support-boundary/idea.md) | `docs/Idea/0009_local-dev-support-boundary/` | local 전용 seed/dev hook/지원 API의 경계와 운영 규율을 정리한 메모 |
| [ConfigurationProperties 기반 설정 바인딩 정리](docs/Idea/0010_configuration-properties-binding-guideline/idea.md) | `docs/Idea/0010_configuration-properties-binding-guideline/` | 이미 적용된 설정 바인딩 패턴과 이후 확장 기준 메모 |
| [OAuth 로그인 도입 검토](docs/Idea/0011_oauth-login-adoption-consideration/idea.md) | `docs/Idea/0011_oauth-login-adoption-consideration/` | 기존 이메일 회원, guest claim, 전화번호 인증 흐름과의 연결 정책 검토 메모 |
| [폼 접근성 향상 가이드](docs/Idea/0012_form-accessibility-guideline/idea.md) | `docs/Idea/0012_form-accessibility-guideline/` | `controlId` 기반 라벨-입력 연결과 이후 폼 접근성 유지 기준 메모 |
| [회원 세션의 Spring Session 전환](docs/Idea/0013_member-session-spring-session-consideration/idea.md) | `docs/Idea/0013_member-session-spring-session-consideration/` | `HG_SESSION` 계약을 유지한 채 Spring Session + Redis로 전환한 배경과 적용 메모 |
| [테스트 컨텍스트 공유와 프로파일 분리](docs/Idea/0014_test-context-sharing-and-profile-separation/idea.md) | `docs/Idea/0014_test-context-sharing-and-profile-separation/` | Spring Boot 4.0 테스트 컨텍스트 비용과 `test` 프로파일 정렬 기준 메모 |
| [Redis 도입 — 다중 인스턴스 대응](docs/Idea/0015_redis-introduction-for-multi-instance/idea.md) | `docs/Idea/0015_redis-introduction-for-multi-instance/` | 회원 세션, 관리자 세션, rate limit의 Redis 전환 배경과 적용 메모 |
| [금액 타입 도입 검토](docs/Idea/0016_money-type-adoption-consideration/idea.md) | `docs/Idea/0016_money-type-adoption-consideration/` | 현재 `long` 기반 금액 표현을 유지할지 별도 Money 타입을 둘지 검토한 메모 |
| [Spring Batch 마이그레이션 검토](docs/Idea/0017_spring-batch-migration-consideration/idea.md) | `docs/Idea/0017_spring-batch-migration-consideration/` | 현재 커스텀 배치를 유지할 이유와 Spring Batch 재검토 조건을 정리한 메모 |
| [8회권 구매 회원 전용 전환](docs/Idea/0018_pass-member-only-purchase/idea.md) | `docs/Idea/0018_pass-member-only-purchase/` | 비회원 8회권 구매 경로를 제거하고 회원 전용으로 단순화할지 검토한 메모 |
| [기능 롤아웃 및 canonical route 체크리스트](docs/Idea/0020_feature-rollout-and-canonical-route-checklist/idea.md) | `docs/Idea/0020_feature-rollout-and-canonical-route-checklist/` | 공개 경로, alias 종료 시점, 문서/E2E/운영 지표를 함께 정리하는 기준 메모 |
| [Controller 경계와 Query Facade 정리 가이드](docs/Idea/0021_controller-boundary-and-query-facade-guideline/idea.md) | `docs/Idea/0021_controller-boundary-and-query-facade-guideline/` | controller 책임을 HTTP 매핑 중심으로 제한하고 읽기 조합을 facade로 내리는 기준 메모 |

### 🧪 POC

| 문서 | 경로 | 설명 |
|------|------|------|
| [PaymentProvider CircuitBreaker 적용 POC](docs/POC/0001_payment-provider-circuit-breaker-rollout/poc.md) | `docs/POC/0001_payment-provider-circuit-breaker-rollout/` | 결제 환불 경계의 CircuitBreaker/TimeLimiter 적용 실험과 결과 |

### 🔁 Retrospective

| 문서 | 경로 | 설명 |
|------|------|------|
| [기반 선행 구축 회고](docs/Retrospective/0001_bootstrap-foundation-retrospective/retrospective.md) | `docs/Retrospective/0001_bootstrap-foundation-retrospective/` | 프로필/문서/마이그레이션/CI/테스트 기반을 먼저 깔았을 때의 효과 회고 |
| [vertical slice delivery 방식 회고](docs/Retrospective/0002_vertical-slice-delivery-pattern/retrospective.md) | `docs/Retrospective/0002_vertical-slice-delivery-pattern/` | 기능을 도메인별 slice로 완성한 방식의 효과와 한계 회고 |
| [multi-step 흐름 E2E와 canonical route 안정화 회고](docs/Retrospective/0003_multistep-flow-e2e-and-route-stabilization/retrospective.md) | `docs/Retrospective/0003_multistep-flow-e2e-and-route-stabilization/` | guest/member 전환 흐름에서 브라우저 smoke와 경로 기준이 준 효과 회고 |
| [기능 성장 후 관측성 보강 회고](docs/Retrospective/0004_observability-retrofit-after-feature-growth/retrospective.md) | `docs/Retrospective/0004_observability-retrofit-after-feature-growth/` | requestId에서 메트릭/대시보드/Sentry까지 확장한 운영 보강 회고 |
| [guest/member 흐름 수렴 회고](docs/Retrospective/0005_guest-member-flow-convergence/retrospective.md) | `docs/Retrospective/0005_guest-member-flow-convergence/` | guest 중심 공개 흐름 위에 member/claim 흐름이 쌓이면서 생긴 구조 변화 회고 |
| [query facade와 운영 경계 정리 회고](docs/Retrospective/0006_query-facade-and-ops-boundary-alignment/retrospective.md) | `docs/Retrospective/0006_query-facade-and-ops-boundary-alignment/` | controller/query 조합 책임과 운영 경계가 뒤늦게 정리된 패턴 회고 |
| [문서 동기화와 canonical route 운영 규율 회고](docs/Retrospective/0007_document-sync-and-canonical-route-discipline/retrospective.md) | `docs/Retrospective/0007_document-sync-and-canonical-route-discipline/` | 문서와 공개 경로 기준을 늦게 정리할 때의 비용 회고 |
| [local/dev 지원 기능 경계 회고](docs/Retrospective/0008_local-dev-support-boundary/retrospective.md) | `docs/Retrospective/0008_local-dev-support-boundary/` | local 전용 seed/dev hook/지원 API가 늘어나며 생긴 경계 문제 회고 |

### 📄 1Pager

| 문서 | 경로 | 설명 |
|------|------|------|
| [1Pager Guide](docs/1Pager/README.md) | `docs/1Pager/` | 이해관계자 공유용 한 장 요약 문서 카테고리 안내 |

문서 운영 규칙:
- 세션성 인수인계 메모와 활성 실행 계획은 README 문서 목록에서 관리하지 않는다.
- 완료된 임시 실행 계획은 `docs/1Pager`에 남기지 않는다.
- 장기 보관 가치가 있는 내용만 `docs/Idea`, `docs/1Pager`, `docs/PRD`, `docs/POC`, `docs/Retrospective`, `docs/ADR`에 남긴다.

## ✅ 현재 제공 기능

- 공개 사용자 흐름
  - 상점형 홈 / 스토어 네비게이션
  - 상품 목록/상세
  - 회원가입 / 로그인
  - 제출 직전 인증 게이트 기반 예약 생성
  - 제출 직전 인증 게이트 기반 8회권 구매
  - 상품 상세에서 회원 주문 생성
  - legacy 비회원 주문 fallback (`/orders/new`, 상품/수량 prefill 지원, direct entry는 수동 fallback gate 후 진행)
  - 비회원 조회 허브 (`/guest`)
  - 비회원 예약 조회/변경/취소 (`/guest/bookings`, 조회용 보조 경로)
  - 비회원 주문 조회 (`/guest/orders`, 조회용 보조 경로)
  - 회원 마이페이지 (`내 주문`, `내 예약`, `내 8회권`, guest claim)
  - 회원 주문/예약/8회권 전체 목록 (`/my/orders`, `/my/bookings`, `/my/passes`, 검색/상태 필터/quick tab/정렬 포함)
  - 회원 예약 상세/변경/취소 (`/my/bookings/:id`)
  - 상품 상세 Product Q&A 조회/회원 작성/비밀글 비밀번호 확인
  - 회원 1:1 문의 작성/목록 조회 (`/my/inquiries`)
  - 회원 마이페이지에서 비회원 이력 가져오기 (휴대폰 재인증 후 claim)
  - 비회원 성공 화면에서 회원가입/로그인 후 `/my` claim으로 바로 이어지는 CTA
  - 로그인/회원가입 페이지에서 `redirect`·`claim`·회원가입 prefill(`name`/`phone`) 컨텍스트 유지
- 관리자 흐름
  - 상품 등록/조회
  - 슬롯 생성/비활성화
  - 예약 목록 조회/노쇼 처리
  - 주문 승인/거절/제작 재개/제작 완료/지연/배송/픽업 관리
  - 주문 결정 이력 조회
  - 8회권 만료/환불
  - 환불 실패 조회/재시도
  - 상품 Q&A 답변 관리
  - 1:1 문의 답변 관리

프론트 주요 경로:

- `/products`
- `/products/:id`
- `/login`
- `/signup`
- `/my`
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

- `app/`
  - Spring Boot 진입점, 컨트롤러, 유스케이스 orchestration, `port/in`·`port/out`, 배치, 통합 테스트
- `domain/`
  - 엔티티, 상태 전이, 정책 등 핵심 비즈니스 규칙
- `infra/`
  - JPA 리포지토리와 persistence/external adapter, 결제/알림/세션/모니터링 등 외부 연동 구현
- `common/`
  - 공통 예외, 시간 유틸, 공용 타입
- `monitoring/`
  - Prometheus scrape/alert rule, Grafana datasource/provisioning, 대시보드 JSON
- `frontend/`
  - Vite + React + TypeScript 프론트엔드

## 🚀 로컬 실행

### 1. 요구사항

- Java 21
- Node.js 20+
- Docker / Docker Compose

### 2. 주요 환경 변수

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ADMIN_API_KEY`
- `ADMIN_ENABLE_API_KEY_AUTH`
- `PAYMENT_TIMEOUT_MILLIS`, `PAYMENT_CB_*`
- `RATE_LIMIT_TRUST_FORWARDED`
- `ACTUATOR_HEALTH_SHOW_DETAILS`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `PLAYWRIGHT_ADMIN_USERNAME`
- `PLAYWRIGHT_ADMIN_PASSWORD`
- `PLAYWRIGHT_BACKEND_URL`

기본 프로필은 `local`이다.

### 3. 백엔드 실행 방식

MySQL만 Docker로 띄우고 앱은 로컬에서 실행:

```bash
docker compose up -d mysql
docker compose stop app
./gradlew :app:bootRun
```

- 이미 `docker compose up -d --build`로 앱 컨테이너가 떠 있다면, 로컬 `bootRun` 전에 `docker compose stop app`으로 8080 충돌을 먼저 해소한다.
- `local` 프로필로 `bootRun`하면 `classes` 테이블이 비어 있을 때 기본 클래스 3종(향수/우드/니트)을 자동 seed한다.

MySQL + 앱 컨테이너를 함께 실행:

```bash
docker compose up -d --build
```

- `prometheus`: `http://localhost:9090`
- `grafana`: `http://localhost:3001` (`admin` / `admin`)

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
- 정책 테스트: `./gradlew :app:policyTest`
- 유스케이스 통합 테스트: `./gradlew --no-daemon :app:useCaseTest`
- 단일 테스트 예시:
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderApprovalUseCaseIT`

### 프론트

- 프로덕션 빌드: `cd frontend && npm run build`
- E2E smoke 브라우저 설치: `cd frontend && npm run e2e:install`
- E2E smoke 실행: `cd frontend && npm run e2e`

E2E 참고:
- Playwright는 `frontend/playwright.config.ts` 기준으로 동작한다.
- Vite dev server는 Playwright가 직접 띄우거나 기존 `localhost:3000`을 재사용한다.
- 백엔드는 별도로 `http://localhost:8080`에서 실행 중이어야 한다.
- smoke spec은 사용자 여정 기준으로 분리되어 있다.
- 현재 파일 구성은 `admin-product-order.smoke.spec.ts`, `guest-booking-pass.smoke.spec.ts`, `member-self-service.smoke.spec.ts`, `guest-claim-onboarding.smoke.spec.ts`다.
- 시나리오 번호(`P8-1`~`P8-9`)는 유지하므로 기존 `--grep "P8-8"` 같은 실행 방식은 그대로 사용할 수 있다.
- 관리자 보조 API 호출은 `POST /api/v1/admin/auth/login`으로 얻은 Bearer 토큰을 사용한다.
- 로컬 `bootRun`은 `classes` 테이블이 비어 있으면 기본 클래스를 자동 생성하므로 clean DB에서도 예약/8회권 시나리오를 바로 돌릴 수 있다.
- 시나리오 5(`환불 실패 -> 재시도`)는 local 전용 dev hook(`/api/v1/admin/dev/payment/refunds/fail-next`)으로 자동화되어 있고, 필요하면 요청 바디에 `orderId`를 넣어 특정 주문으로 범위를 좁힐 수 있다.
- Playwright 관리자 로그인 기본값은 `admin` / `admin1234`이며, 필요하면 `PLAYWRIGHT_ADMIN_USERNAME`, `PLAYWRIGHT_ADMIN_PASSWORD`로 덮어쓴다.
- 백엔드 기준 URL을 바꾸려면 `PLAYWRIGHT_BACKEND_URL`을 사용한다.
- 현재 smoke 범위는 admin/order 1·4·5, guest booking/pass 2·3, member self-service 6·7, guest claim/onboarding 8·9이며, `P8-7`은 회원 예약 상세/변경/취소, `P8-8`은 guest claim, `P8-9`는 guest 성공 화면에서 회원가입 후 claim 모달 자동 진입까지 포함한다.
- `/bookings/new`, `/passes/purchase`는 첫 화면에서 인증하지 않고 제출 직전에 auth gate를 연다.
- 상품 상세의 `비회원 주문하기`는 `/orders/new?productId=&qty=`로 이동해 선택한 상품과 수량을 prefill 한다.
- 비회원 진입 허브는 `/guest`이고, canonical guest 조회 경로는 `/guest/orders`, `/guest/bookings`이며 생성 후 확인용 보조 경로로 유지한다.

## API/운영 메모

- 표준 API 경로는 `/api/v1/**`다.
- 레거시 무버전 경로도 일부 유지하지만, 신규 문서와 테스트는 `/api/v1/**`를 기준으로 한다.
- 관리자 화면은 사용자명/비밀번호 로그인 후 Bearer 토큰으로 동작하고, local/dev API 보조 호출은 `X-Admin-Key` 폴백을 사용할 수 있다.
- 주문 승인/거절/제작 재개/제작 완료 이력의 admin 식별자는 Bearer 세션에서 추출한다. API Key 폴백 경로는 adminId가 null일 수 있다.
- 배송 준비/출발/완료 전이와 주문 결정 이력 조회도 `/api/v1/admin/orders/**` 아래에서 같은 Bearer 세션 기준으로 동작한다.
- 회원 UI는 `/my`, `/my/orders`, `/my/orders/:id`, `/my/bookings`, `/my/bookings/:id`, `/my/passes`를 사용하고, 목록 페이지는 검색/상태 필터/quick tab/정렬을 제공한다. 백엔드는 `/api/v1/me/**`로 동작한다.
- `/api/v1/me/guest-claims/{preview,verify,claim}` 로 같은 번호의 guest 주문/예약/8회권을 회원 계정으로 이전할 수 있다.
- guest 주문/예약/8회권 성공 화면의 회원가입/로그인 CTA는 `/my?claim=1` 로 이어져 claim 모달을 자동으로 열 수 있다.
- 로그인/회원가입 페이지는 `redirect`, `claim`, `name`, `phone` query를 유지해 guest 성공 화면이나 member gate에서 넘어온 문맥을 잃지 않는다.
- 비회원 조회는 계속 토큰 기반(`bookingId + token`, `orderId + token`)을 사용하되, 프론트 canonical route는 `/guest/orders`, `/guest/bookings`다.
- 상품 상세에서 guest 주문으로 넘길 때는 `/orders/new`가 `productId`, `qty` query를 받아 초기 주문 항목을 채운다. query 없이 직접 연 `/orders/new`는 명시적 계속 버튼 뒤에만 수동 다중 상품 주문을 허용한다.
- 운영 권장안은 `/guest` 허브와 canonical guest 조회 경로, `/orders/new` direct gate를 당분간 유지하고, member route 안정화 후 2~4주 동안 사용량과 문의를 본 뒤 direct guest fallback 축소 여부를 결정하는 것이다.
- `/api/v1/monitoring/client-events` 는 guest/member 전환 이벤트를 fire-and-forget으로 받아 `[client-monitoring]` 로그를 남긴다. 현재는 `/guest` 허브 유입, `/orders/new` direct continue, guest 성공/조회 화면의 회원 전환 CTA, `/my` claim 모달 오픈, claim 완료를 추적한다.

## 브랜치 흐름

- 작업 브랜치에서 변경 수행
- 먼저 `codexReview`로 반영해 통합 확인
- 이후 `codexReview -> main` PR 생성 및 머지
- 구현 변경 시 관련 문서도 함께 갱신

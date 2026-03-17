# happyGallery

오프라인 공방의 **온라인 쇼핑몰 + 체험 예약 시스템**.
상품 주문, 클래스 예약, 8회권 패스, 관리자 운영을 하나의 플랫폼에서 처리한다.

- **백엔드**: Spring Boot 4.0.2 / Java 21 / MySQL 8
- **프론트엔드**: Vite / React 19 / TypeScript / Bootstrap
- **구조**: Gradle 멀티 모듈(`app` · `domain` · `infra` · `common`) + `frontend/` 워크스페이스

---

## 구조와 주요 라이브러리

| 구분 | 구성 | 용도 |
|------|------|------|
| 백엔드 구조 | `app` / `domain` / `infra` / `common` | 진입점, 도메인 규칙, 외부 연동, 공통 유틸을 분리한 멀티 모듈 구조 |
| 프론트 구조 | `frontend/` (Vite + React 19 + TypeScript) | 스토어/마이페이지/관리자 UI와 브라우저 흐름 구현 |
| Resilience4j | `resilience4j-circuitbreaker`, `resilience4j-timelimiter` | PG 환불 외부 호출에 CircuitBreaker + TimeLimiter를 적용해 장애 전파를 줄임 |
| Spring Actuator | `spring-boot-starter-actuator` | `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus` 운영 엔드포인트 제공 |
| Prometheus | `micrometer-registry-prometheus` | Actuator 메트릭과 `happygallery.funnel.*` 커스텀 메트릭을 scrape 가능한 포맷으로 노출 |
| Grafana | Prometheus 대시보드 기준 도구 | 운영 메트릭과 전환 퍼널 지표를 시각화하는 대시보드 대상으로 사용하며, 현재 연동 트랙을 진행 중 |
| TanStack Query | `@tanstack/react-query` | 상품/예약/주문/관리자 데이터를 조회·캐시하고 mutation 후 invalidate를 처리 |
| Bootstrap | `bootstrap` | 스토어/관리자 화면의 기본 UI 레이아웃과 컴포넌트 스타일링 |

---

## 문서 목록

### 루트 운영 문서

| 문서 | 경로 | 설명 |
|------|------|------|
| `README.md` | `/README.md` | 저장소 개요와 문서/실행 진입점 |
| `HANDOFF.md` | `/HANDOFF.md` | 현재 구현 상태, 최근 변경, 다음 작업 |
| `plan.md` | `/plan.md` | 현재 활성 실행 계획과 백로그 |
| `AGENTS.md` | `/AGENTS.md` | 저장소 작업 규칙 |
| `CLAUDE.md` | `/CLAUDE.md` | 별도 에이전트 운영 메모 |

### PRD

| 문서 | 경로 | 설명 |
|------|------|------|
| [Core MVP Specification](docs/PRD/0001_spec/spec.md) | `docs/PRD/0001_spec/` | 전체 시스템의 기능 요구사항 정의서 |
| [Member Store Transition](docs/PRD/0002_member_store_transition/spec.md) | `docs/PRD/0002_member_store_transition/` | 회원 인증·스토어 전환 차기 요구사항 |
| [Out Of Scope](docs/PRD/0003_out_of_scope/scope.md) | `docs/PRD/0003_out_of_scope/` | 초기 버전에서 명시적으로 제외하는 범위 |

### ADR

| 문서 | 경로 | 설명 |
|------|------|------|
| `ADR-0001` ~ `ADR-0021` | `docs/ADR/` | 데이터 모델, 상태 전이, 결제, 인증, 운영, 헥사고날 전환 등 기술 결정 |

### Idea

| 문서 | 경로 | 설명 |
|------|------|------|
| [JSON + Generated Column](docs/Idea/0001_json-generated-column-consideration/idea.md) | `docs/Idea/0001_json-generated-column-consideration/` | 가변 속성 저장 패턴 검토 |
| [Bulkhead (Resilience4j)](docs/Idea/0002_bulkhead-resilience4j-consideration/idea.md) | `docs/Idea/0002_bulkhead-resilience4j-consideration/` | 외부 호출 격리 전략 검토 |

### POC

| 문서 | 경로 | 설명 |
|------|------|------|
| [PaymentProvider CircuitBreaker 적용 POC](docs/POC/0001_payment-provider-circuit-breaker-rollout/poc.md) | `docs/POC/0001_payment-provider-circuit-breaker-rollout/` | 결제 환불 경계의 CircuitBreaker/TimeLimiter 적용 실험과 결과 |

### 1Pager

| 문서 | 경로 | 설명 |
|------|------|------|
| [1Pager Guide](docs/1Pager/README.md) | `docs/1Pager/` | 이해관계자 공유용 한 장 요약 문서 카테고리 안내 |

문서 운영 규칙:
- 현재 활성 실행 계획은 루트 `plan.md`만 사용한다.
- 완료된 임시 실행 계획은 `docs/1Pager`에 남기지 않는다.
- 장기 보관 가치가 있는 내용만 `docs/Idea`, `docs/1Pager`, `docs/PRD`, `docs/POC`, `docs/ADR`에 남긴다.

## 현재 제공 기능

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
- `/bookings/new`
- `/guest`
- `/guest/bookings`
- `/passes/purchase`
- `/orders/new`
- `/guest/orders`
- `/admin`

## 저장소 구조

- `app/`
  - Spring Boot 진입점, 컨트롤러, 애플리케이션 서비스, 배치, 통합 테스트
- `domain/`
  - 엔티티, 상태 전이, 정책 등 핵심 비즈니스 규칙
- `infra/`
  - JPA 리포지토리, 결제/알림 등 외부 연동 구현
- `common/`
  - 공통 예외, 시간 유틸, 공용 타입
- `frontend/`
  - Vite + React + TypeScript 프론트엔드

## 로컬 실행

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

## 빌드와 검증

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
- 관리자 보조 API 호출은 `POST /api/v1/admin/auth/login`으로 얻은 Bearer 토큰을 사용한다.
- 로컬 `bootRun`은 `classes` 테이블이 비어 있으면 기본 클래스를 자동 생성하므로 clean DB에서도 예약/8회권 시나리오를 바로 돌릴 수 있다.
- 시나리오 5(`환불 실패 -> 재시도`)는 local 전용 dev hook(`/api/v1/admin/dev/payment/refunds/fail-next`)으로 자동화되어 있고, 필요하면 요청 바디에 `orderId`를 넣어 특정 주문으로 범위를 좁힐 수 있다.
- Playwright 관리자 로그인 기본값은 `admin` / `admin1234`이며, 필요하면 `PLAYWRIGHT_ADMIN_USERNAME`, `PLAYWRIGHT_ADMIN_PASSWORD`로 덮어쓴다.
- 백엔드 기준 URL을 바꾸려면 `PLAYWRIGHT_BACKEND_URL`을 사용한다.
- 현재 smoke 범위는 guest/admin 1~5와 member storefront/claim/onboarding 6~9이며, `P8-7`은 회원 예약 상세/변경/취소, `P8-8`은 guest claim, `P8-9`는 guest 성공 화면에서 회원가입 후 claim 모달 자동 진입까지 포함한다.
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

## 문서 우선순위

1. `HANDOFF.md`
2. `plan.md`
3. `docs/PRD/0001_spec/spec.md`
4. 관련 `docs/ADR/*`
5. 필요 시 `docs/Idea`, `docs/1Pager`, `docs/POC`

## 브랜치 흐름

- 작업 브랜치에서 변경 수행
- 먼저 `codexReview`로 반영해 통합 확인
- 이후 `codexReview -> main` PR 생성 및 머지
- 구현 변경 시 관련 문서도 함께 갱신

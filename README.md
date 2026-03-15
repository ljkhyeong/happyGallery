# happyGallery

happyGallery는 오프라인 공방의 온라인 쇼핑몰 + 체험 예약 시스템이다.

- 백엔드: Spring Boot 4.0.2, Java 21, MySQL 8
- 프론트: Vite, React 19, TypeScript, Bootstrap
- 저장소 형태: Gradle 멀티 모듈 백엔드 + 별도 `frontend/` 워크스페이스

## 빠른 길잡이

- 현재 세션 인수인계: `HANDOFF.md`
- 기준 스펙: `docs/PRD/0001_spec/spec.md`
- 전체 계획/백로그: `docs/1Pager/0000_project_plan/plan.md`
- 프론트 계획: `docs/1Pager/0003_frontend_plan/plan.md`
- 설계 결정: `docs/ADR/`

## 현재 제공 기능

- 공개 사용자 흐름
  - 상품 목록/상세
  - 회원가입 / 로그인
  - 제출 직전 인증 게이트 기반 예약 생성
  - 제출 직전 인증 게이트 기반 8회권 구매
  - 상품 상세에서 회원 주문 생성
  - 비회원 예약 조회/변경/취소
  - 비회원 주문 조회
  - 회원 마이페이지 (`내 주문`, `내 예약`, `내 8회권`)
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
- `/my/orders/:id`
- `/bookings/new`
- `/bookings/manage`
- `/passes/purchase`
- `/orders/new`
- `/orders/detail`
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
- 현재 smoke 범위는 guest/admin 1~5와 member storefront 6~7이다.
- `/bookings/new`, `/passes/purchase`는 첫 화면에서 인증하지 않고 제출 직전에 auth gate를 연다.
- `/orders/detail`, `/bookings/manage`는 아직 legacy guest 조회 경로다.

## API/운영 메모

- 표준 API 경로는 `/api/v1/**`다.
- 레거시 무버전 경로도 일부 유지하지만, 신규 문서와 테스트는 `/api/v1/**`를 기준으로 한다.
- 관리자 화면은 사용자명/비밀번호 로그인 후 Bearer 토큰으로 동작하고, local/dev API 보조 호출은 `X-Admin-Key` 폴백을 사용할 수 있다.
- 주문 승인/거절/제작 재개/제작 완료 이력의 admin 식별자는 Bearer 세션에서 추출한다. API Key 폴백 경로는 adminId가 null일 수 있다.
- 배송 준비/출발/완료 전이와 주문 결정 이력 조회도 `/api/v1/admin/orders/**` 아래에서 같은 Bearer 세션 기준으로 동작한다.
- 회원 UI는 `/my`와 `/my/orders/:id`를 사용하고, 백엔드는 `/api/v1/me/**`로 동작한다.
- 비회원 조회는 계속 토큰 기반(`bookingId + token`, `orderId + token`) legacy 경로를 유지한다.

## 문서 우선순위

1. `HANDOFF.md`
2. `docs/PRD/0001_spec/spec.md`
3. 관련 `docs/ADR/*`
4. 도메인별 `docs/1Pager/*`

## 브랜치 흐름

- 작업 브랜치에서 변경 수행
- 먼저 `codexReview`로 반영해 통합 확인
- 이후 `codexReview -> main` PR 생성 및 머지
- 구현 변경 시 관련 문서도 함께 갱신

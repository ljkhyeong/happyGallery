# HANDOFF.md
> 다음 세션을 위한 인수인계 문서.
> 작성 시점: 2026-03-15 (프론트 F0–F9 완료, P8·P9·P10 완료, CR-P1~P7 완료, U1 완료, U2/U3/U4 완료, U5 완료, U6 3차 완료, 회원 온보딩 polish 완료)

---

## 프로젝트 요약

**happyGallery** — 오프라인 공방의 온라인 쇼핑몰 + 체험 예약 시스템 (Spring Boot 4.0.2 / Java 21 / MySQL 8)

- 빠른 진입 문서: `README.md`
- 핵심 스펙: `docs/PRD/0001_spec/spec.md`
- 전체 구현 계획: `docs/1Pager/0000_project_plan/plan.md`
- 의사결정 기록: `docs/ADR/`
- 리팩토링 계획: `docs/1Pager/0002_refactoring_plan/plan.md`
- 프론트 계획: `docs/1Pager/0003_frontend_plan/plan.md`
- 후속 폴리시 계획: `docs/1Pager/0004_polish_plan/plan.md`
- 코드리뷰 후속 계획: `docs/1Pager/0006_code_review_followups/plan.md`
- 회원 스토어 전환 계획: `docs/1Pager/0007_member_store_transition/plan.md`
- 관측성 스택 고도화 계획: `docs/1Pager/0009_observability_stack_upgrade/plan.md`
- 헥사고날 전환 계획: `docs/1Pager/0010_hexagonal_architecture_transition/plan.md`
- 회원 스토어 차기 PRD 초안: `docs/PRD/0002_member_store_transition/spec.md`
- 기준 확인 순서: `HANDOFF.md -> docs/PRD/0001_spec/spec.md -> docs/ADR/*`

---

## 현재 브랜치 / 워크트리 상태

- 권장 작업 브랜치: `codex/work-20260315-015031`
- 최근 작업:
  - 헥사고날 전환 pilot 1차 진행 — customer auth / guest claim / admin session / booking / order / payment / notification / product 경계에 `port/in`, `port/out`, `*PortAdapter`를 도입하고, 기존 애플리케이션 서비스가 `infra` 구현 대신 애플리케이션 포트를 의존하도록 정리
  - 운영 모니터링 1차 구현 완료 — 프론트 주요 guest/member 전환 지점에서 `/api/v1/monitoring/client-events` 로 fire-and-forget 이벤트를 보내고, 서버는 requestId가 붙은 `[client-monitoring]` 로그로 `/guest` 허브 유입, `/orders/new` direct continue, guest→member CTA, claim 완료를 추적하도록 정리
  - `/orders/new` direct fallback gate 추가 — 상품 상세에서 prefill로 내려온 경우는 바로 진행하고, query 없이 직접 진입한 `/orders/new`는 “보조 경로” 안내 후 명시적으로 계속해야 수동 비회원 다중 상품 주문을 진행하도록 정리
  - guest lookup 허브 추가 완료 — `/guest` 진입 페이지를 추가하고, 상단 utility bar와 홈은 direct guest lookup 대신 `/guest` 허브를 우선 노출하도록 정리
  - guest route 운영 카피 정리 완료 — `/guest/orders`, `/guest/bookings`, 홈 lookup 패널, guest 성공 CTA에서 guest 경로를 “조회용 보조 경로”로 명확히 표시하고, 로그인/회원가입 후 `/my` claim으로 이어지는 전환 문구를 강화
  - 로그인 직후 회원 상태 전역 동기화 완료 — `CustomerAuthProvider`를 도입해 로그인/회원가입 후 상단 네비가 새로고침 없이 즉시 `로그아웃` 상태로 반영되도록 정리하고, `P8-9`에 즉시 상태 전환 검증 추가
  - `/my` 목록 고도화 완료 — `/my/orders`, `/my/bookings`, `/my/passes`에 quick status tab, 정렬, 요약 chip을 추가해 회원 이력 탐색 흐름을 한 단계 정리
  - 회원 온보딩 polish 완료 — 로그인/회원가입 페이지를 storefront/member 문맥에 맞는 2열 레이아웃으로 정리하고, `redirect`·`claim`·회원가입 prefill(`name`/`phone`) 컨텍스트를 로그인/회원가입 전환 링크에도 유지, `/my?claim=1` 진입 뒤 모달을 닫아도 후속 claim 안내 카드가 남도록 보강
  - `/my` 목록 필터 확장 완료 — `/my/orders`, `/my/bookings`, `/my/passes`에 상태 필터와 검색을 추가하고, 회원 8회권 구매 완료 CTA도 `/my/passes`로 맞춤
  - member/guest success flow 고도화 완료 — guest 주문/예약/8회권 성공 화면에서 `회원가입/로그인 -> /my claim` 경로를 직접 안내하고, 회원가입 페이지는 휴대폰/이름 prefill + claim 안내로 진입, `/my?claim=1`은 claim 모달을 자동으로 열도록 정리
  - `/my` 세부 라우트 확장 완료 — `/my/orders`, `/my/bookings`, `/my/passes` 목록 페이지 추가, 대시보드는 최근 5건 + `전체 보기` 구조로 정리, 회원 상세 페이지의 back link도 각 목록 기준으로 맞춤
  - guest/member lookup UX polish 완료 — 상단 utility bar와 홈 lookup CTA에서 `회원 내 정보`/`비회원 조회` 구분을 명확히 하고, `/guest/orders`·`/guest/bookings`를 안내형 카드 + 액션 버튼 구조로 정리, guest 주문/예약 완료 카드에도 `/guest/**` 조회와 회원 claim 진입 버튼을 함께 배치
  - member self-service polish 완료 — `/my` 로그인 게이트를 회원/비회원 진입이 분명한 대시보드형 카드로 정리하고, 로그인 후에는 주문·예약·8회권 요약 통계/다음 예약/guest claim 진입을 한 화면에 배치, `/my/orders/:id`·`/my/bookings/:id` 상세 헤더와 CTA도 회원용 카피 기준으로 정리
  - legacy guest route 정리 완료 — `/orders/detail`, `/bookings/manage` redirect alias 제거, guest 조회 경로를 `/guest/orders`, `/guest/bookings`로 단일화하고 관련 README/PRD/1Pager/P8 문서를 canonical route 기준으로 갱신
  - P8-3 smoke 안정화 완료 — Playwright가 고정 시각 슬롯과 충돌하지 않도록 E2E helper에서 기존 admin 슬롯 시작 시각을 피하는 유니크 슬롯 윈도우를 선택하도록 보강, `P8-5`의 모호한 `주문하기` selector도 같이 정리해서 full smoke 1~8 재통과
  - U6 guest claim browser automation 완료 — Playwright `P8-8` 추가, guest 주문·8회권·예약 생성 후 같은 번호의 회원이 `/my`에서 휴대폰 재인증과 선택 claim을 수행하는 시나리오 자동화, helper에 locked-phone verification과 custom signup phone 지원 추가
  - U3 guest 주문 fallback 보강 — 상품 상세의 `비회원 주문하기`가 `/orders/new?productId=&qty=`로 이동하며 선택한 상품/수량을 prefill 하고, legacy guest 주문 페이지에서 그대로 수정/추가 주문할 수 있게 정리, `P8-4`도 이 경로 기준으로 갱신
  - U5 회원 셀프서비스 완료 — `/my/bookings/:id` 회원 예약 상세 페이지 추가, 회원 예약 변경/취소 UI 연결, `/guest/orders`·`/guest/bookings` canonical guest 경로 정리, `GET /api/v1/me/bookings/{id}` 상세 조회의 slot/class eager fetch 보강
  - U5 guest claim 2차 완료 — `/api/v1/me/guest-claims/{preview,verify,claim}` 추가, 회원 휴대폰 재인증 후 같은 번호의 guest 주문/예약/8회권을 선택 이전하는 마이페이지 모달 반영, 예약 연결 8회권 자동 claim, 회원 전화번호의 하이픈/숫자-only 포맷 차이 흡수
  - U3 storefront / 상품 상세 1차 완료 — 홈과 네비게이션을 상점형 IA로 재구성, 상품 상세를 구매 중심 레이아웃으로 정리, `/orders/new`를 legacy guest fallback 경로로 명시
  - U6 rollout / E2E 3차 완료 — Playwright smoke 1~9 통과, guest/member/claim 혼합 시나리오와 guest 성공 화면 -> 회원가입 -> claim 모달 자동 진입까지 정리, 관리자 로그인 토큰 캐시와 고객 세션 쿠키 bootstrap helper 반영, README/P8/U6/PRD/HANDOFF 문서 동기화
  - U5 회원 셀프서비스 1차 — `/my`, `/my/orders/:id`, 회원 주문/예약/8회권 목록 조회, 회원 주문 상세, 회원 예약/8회권 생성과 `/api/v1/me/**` 기반 흐름 확장
  - U4 제출 직전 인증 게이트 + 주문 진입 전환 — `/products/:id` 회원 주문, `/bookings/new`·`/passes/purchase`의 member/guest auth gate 분기, legacy guest 조회 경로와 공존
  - U1 고객 인증 기반 — `User`/`UserSession` 엔티티, `CustomerAuthService`(BCrypt + DB 세션), `CustomerAuthFilter`(HttpOnly 쿠키 `HG_SESSION`), `/api/v1/auth/{signup,login,logout}` + `GET /api/v1/me`, 프론트 `LoginPage`/`SignupPage`/Layout 로그인 상태 표시, V14 migration, rate limit(customer-login 10/min, customer-signup 5/min)
  - CR-P7 배송 흐름 및 운영 이력 확장 — 배송 전이 API 3종 (prepare-shipping, mark-shipped, mark-delivered), 주문 이력 조회 API, expectedShipDate write guard, 프론트 배송 액션/이력 패널, spec/plan/HANDOFF 문서 동기화
  - CR-P6 계약/감사/무결성 후속 — fulfillment.status FE 정합, admin principal 세션 전환, fulfillments.order_id unique, convertToPickup stale 데이터 제거, resume-production HTTP test, 프론트 중복 정리, README/PRD/ADR/E2E 문서 정합화
  - 문서 정합화 — spec.md, ADR-0013, ADR-0014 상태명·Fulfillment 구조 반영
  - CR-P5 장기 리팩토링 — DELAY_REQUESTED 재개 흐름, Fulfillment 단일성, *_REFUNDED 상태명 변경, Fulfillment.status 제거, PG 환불 패턴 통합, local hook 범위 제한
  - P10 관측성/운영 준비 — Actuator 노출 정책, 에러 응답 requestId 포함, 배치 MDC requestId 주입
  - P9 프로덕션 인증 계층 — BCrypt 기반 관리자 로그인, UUID 세션 토큰, X-Admin-Key dev fallback 토글
  - P8 E2E 시나리오 검증 — local 환불 실패 hook 추가, 기본 관리자 계정 정합화, 실제 로컬 smoke 1~9 pass
- 프론트 생성물(`node_modules`, `dist`, `*.tsbuildinfo`)은 `frontend/.gitignore` 기준으로 추적 제외
- 최근 검증:
  - `cd frontend && npm run e2e -- --grep "P8-9"` 통과
  - `cd frontend && npm run e2e` 통과 (smoke 1~9)
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.customer.CustomerGuestClaimUseCaseIT` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.customer.CustomerAuthUseCaseIT` 통과
  - `cd frontend && npm run build` 통과
  - `cd frontend && npm run e2e -- --grep "P8-8"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(2|3|4|8|9)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(6|7)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(2|4)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-7"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-(4|7|8)"` 통과
  - `cd frontend && npm run e2e -- --grep "P8-6"` 통과 (U3 핵심 회원 주문 경로)
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderProductionUseCaseIT` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.booking.LocalBookingClassSeedServiceTest` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.infra.payment.FakePaymentProviderTest --tests com.personal.happygallery.app.web.admin.LocalRefundFailureControllerTest --tests com.personal.happygallery.app.web.admin.AdminLoginUseCaseIT` 통과
  - `curl -s http://127.0.0.1:8080/actuator/health` 응답 확인

---

## 프론트 진행 상황

프론트 플랜: `docs/1Pager/0003_frontend_plan/plan.md`

### 전 단위 완료 (B1–B4, F0–F9)

| 단위 | 내용 |
|------|------|
| **B1** | 프론트 선행 API 갭 분석 문서화 |
| **B2** | 공개 상품/클래스/슬롯 조회 API 추가 |
| **B3** | 8회권 구매 계약 보완 |
| **B4** | 사용자 주문 API 계약 추가 |
| **F0** | 프론트 워크스페이스 스캐폴딩 |
| **F1** | 공통 API 클라이언트와 에러 처리 계층 |
| **F2** | 앱 셸/테마/공통 UI 안정화 + Pretendard 폰트 + 홈페이지 카드 |
| **F3** | 관리자 상품/슬롯 화면 + 401/400 에러 구분 + 클래스 드롭다운 |
| **F4** | 예약 조회/변경/취소 화면 |
| **F5** | 공개 상품 카탈로그 화면 |
| **F6** | 예약 생성 화면 |
| **F7** | 8회권 구매 화면 |
| **F8** | 관리자 운영 확장 화면 + 401 처리 보강 |
| **F9** | 사용자 주문 화면 + 총액 미리보기 |

### 현재 프론트 진입 경로

- `/` — 서비스 홈 (진입 카드)
- `/products` — 상품 목록
- `/products/:id` — 상품 상세 + 회원 주문 진입
- `/login` — 고객 로그인
- `/signup` — 고객 회원가입
- `/my` — 회원 마이페이지 (`내 주문`, `내 예약`, `내 8회권`, `비회원 이력 가져오기`)
- `/my/orders` — 회원 주문 전체 목록
- `/my/orders/:id` — 회원 주문 상세
- `/my/bookings` — 회원 예약 전체 목록
- `/my/bookings/:id` — 회원 예약 상세 + 변경/취소
- `/my/passes` — 회원 8회권 전체 목록
- `/bookings/new` — 예약 생성 (member/guest 제출 직전 auth gate)
- `/passes/purchase` — 8회권 구매 (member/guest 제출 직전 auth gate)
- `/guest` — 비회원 조회 안내 허브
- `/guest/orders` — 비회원 주문 조회
- `/guest/bookings` — 비회원 예약 조회/변경/취소
- `/orders/new` — legacy guest 주문 생성 fallback (`productId`, `qty` prefill 지원, direct entry는 수동 fallback gate 후 진행)
- `/admin` — 관리자 (사용자명/비밀번호 로그인, Bearer 토큰 인증)

---

## 다음 우선순위 (polish plan)

폴리시 플랜: `docs/1Pager/0004_polish_plan/plan.md`

| 우선순위 | 단위 | 상태 | 내용 |
|------|------|------|------|
| 즉시 | **P1** | 완료 | 스펙 문서 동기화 |
| 즉시 | **P2** | 완료 | 홈페이지 구현 |
| 즉시 | **P3** | 완료 | 폼 검증 및 에러 UX 강화 |
| 다음 | **P4** | 완료 | 반응형 UI 및 접근성 점검 |
| 다음 | **P5** | 완료 | 관리자 슬롯 조회 API 및 화면 보강 |
| 다음 | **P6** | 완료 | 관리자 예약 조회/노쇼 처리 화면 |
| 다음 | **P7** | 완료 | 관리자 주문 목록 조회 화면 |
| 배포 전 | **P8** | 완료 | Playwright smoke 1~9 pass, local refund failure hook과 회원 storefront/guest claim/success-onboarding 시나리오까지 검증 완료 |
| 배포 전 | **P9** | 완료 | 프로덕션 인증 계층 (BCrypt 로그인, UUID 세션 토큰, API Key dev fallback) |
| 운영 | **P10** | 완료 | 관측성 및 운영 준비 (Actuator 정책, 에러 requestId, 배치 MDC) |

### 코드리뷰 후속 진행 상황

| 단위 | 상태 | 내용 |
|------|------|------|
| **CR-P1** | 완료 | 인증/조회 상태 정합성 (admin 캐시 제거, 조회 실패 초기화, 슬롯 선택 해제) |
| **CR-P2** | 완료 | 운영 인증/테스트 경계 (Playwright Bearer 전환, 로그인 rate limit, admin id 컨텍스트 주입) |
| **CR-P3** | 완료 | 운영 메모리/인프라 리스크 (세션 eviction, bucket eviction, XFF 신뢰 정책) |
| **CR-P4** | 완료 | UX 후속 (예약 기본 날짜 Asia/Seoul, 주문 상품 로딩 실패 에러 UI) |
| **CR-P5** | 완료 | 장기 리팩토링 및 기능 공백 |
| **CR-P6** | 완료 | 계약/감사/무결성 후속 (fulfillment.status 계약 정합, admin principal 세션 기반 전환, fulfillment unique migration, stale ship metadata 정리, resume-production HTTP test, 프론트 정리) |
| **CR-P7** | 완료 | 배송 흐름 및 운영 이력 확장 (배송 전이 API 3종, 이력 조회 API, expectedShipDate write guard, 프론트 배송 액션/이력 패널) |

### 다음 추천 작업

1. 관측성 스택 고도화 — `docs/1Pager/0009_observability_stack_upgrade/plan.md` 기준으로 Prometheus/Grafana/Sentry와 product funnel metric을 정식 운영 경로로 승격
2. 운영 리뷰 — member route 안정화 후 2~4주 동안 `[client-monitoring]` 로그 또는 후속 metric의 `/guest` 허브 유입, `/orders/new` direct continue, guest → member CTA, claim 완료, 문의 유형을 보고 direct guest fallback 축소 여부를 결정
3. `/my` 운영 피드백 반영 — 현재 quick tab/정렬 구성이 충분한지 보고 상태 탭 세분화나 기본 정렬 정책을 조정할지 판단

---

## 리팩토링 진행 상황

리팩토링 플랜: `docs/1Pager/0002_refactoring_plan/plan.md`

### 완료 (R1–R10)

| 단위 | 내용 |
|------|------|
| **R1** | Order 도메인 상태 전이 캡슐화 강화 |
| **R2** | API 예외 매핑 일관성 정리 |
| **R3** | Booking 유스케이스 공통 절차 추출 |
| **R4** | Notification fallback 전략 객체화 |
| **R5** | Batch 서비스 공통 처리 템플릿화 |
| **R6** | Admin Controller DTO 변환 책임 정리 |
| **R7** | Pass 도메인 계산/검증 메서드 명확화 |
| **R8** | Product/Inventory 경계 정리 |
| **R9** | 시간 경계 계산 호출부 정리 |
| **R10** | 테스트 픽스처/중복 유틸 정리 |

---

## 알아야 할 것들

### Spring Boot 4.0 특이사항
- `@UseCaseIT`는 현재 `@AutoConfigureMockMvc(addFilters = false)` 기반으로 유지 중
- `@SpringBootTest` 컨텍스트에서 `ObjectMapper` autowire 불가 → JSON 문자열 직접 구성
- Codex 샌드박스에서는 Gradle JVM 명령이 `FileLockContentionHandler` 소켓 생성 제한에 걸릴 수 있어, 테스트와 `:app:bootRun`은 처음부터 권한 상승 실행으로 처리하는 편이 안정적
- 동일하게 `gh pr *`, 원격 `git fetch/push/pull`, Docker 컨테이너 제어, Playwright 브라우저 설치/실행, 워크스페이스 밖 경로 쓰기처럼 반복적으로 막혔던 작업도 샌드박스 재시도 없이 처음부터 권한 상승 실행으로 처리한다.

### 프론트 공통 패턴
- 고객 인증: `useCustomerAuth()` 훅에서 `/api/v1/me`로 세션 확인 (HttpOnly 쿠키 자동 포함), `login`/`signup`/`logout` 제공
- 회원가입 전화번호는 프론트에서 숫자만 정규화해 전송하고, guest claim은 회원 전화번호의 하이픈/숫자-only 포맷 차이를 모두 허용한다.
- Layout에서 `useCustomerAuth()`로 로그인 상태에 따라 "로그인"/"사용자명+로그아웃" 표시
- `/products/:id`, `/bookings/new`, `/passes/purchase` 는 회원이면 세션 기준으로 바로 제출하고, 비회원이면 제출 직전에 auth gate를 연다.
- 상품 상세의 `비회원 주문하기`는 `/orders/new?productId=&qty=` 로 이동해 선택 상품과 수량을 legacy guest fallback에 미리 담아둔다. query 없이 직접 진입한 `/orders/new`는 수동 fallback gate를 먼저 거친다.
- `/my` 에서 `비회원 이력 가져오기` 모달을 열면, `phoneVerified=false` 회원은 같은 번호로 재인증 후 preview를 보고 주문/예약/8회권을 선택 claim 할 수 있다.
- guest 주문/예약/8회권 성공 화면의 회원가입/로그인 CTA는 `redirect=/my?claim=1` 로 이어지고, `/my`는 이 쿼리로 claim 모달을 자동으로 연다.
- 로그인/회원가입 페이지는 `redirect`와 `claim` 문맥을 유지하고, 회원가입은 guest 성공 화면에서 넘어온 `name`/`phone` prefill도 이어받는다.
- `/my?claim=1` 로 진입한 뒤 자동 오픈된 claim 모달을 닫아도, 대시보드의 claim 카드에서 후속 안내와 재진입 버튼을 계속 노출한다.
- `/my/bookings/:id` 는 회원 예약 상세/변경/취소 화면이며, 비회원 조회는 `/guest/bookings` 로 분리한다.
- `/my/orders`, `/my/bookings`, `/my/passes` 는 검색, 상태 필터, quick tab, 정렬을 제공한다.
- `/guest` 를 비회원 조회 entry route로 노출하고, `/guest/orders`, `/guest/bookings` 는 canonical guest 조회 경로이자 생성 후 확인용 보조 경로로 유지한다.
- 현재 운영 권장안은 `/guest` 허브와 `/guest/orders`, `/guest/bookings`, `/orders/new` direct gate를 그대로 유지한 채 member route 안정화 이후 2~4주 동안 사용량과 문의 유형을 관찰한 뒤 direct guest fallback 축소 여부를 결정하는 것이다.
- `/api/v1/monitoring/client-events` 는 guest/member 주요 전환 이벤트를 requestId가 포함된 `[client-monitoring]` 로그로 남긴다. 현재 수집 범위는 `/guest` 허브 진입, `/orders/new` direct continue, guest 성공/조회 화면의 회원 전환 CTA, `/my` claim 모달 오픈, guest claim 완료다.
- 관리자 API 401 처리: `onAuthError` 콜백을 AdminPage에서 모든 하위 컴포넌트에 전달
- 관리자 인증: `useAdminKey()` 훅에서 사용자명/비밀번호 로그인 → UUID 세션 토큰을 `sessionStorage` (`hg_admin_token`)에 저장, 이후 `Authorization: Bearer {token}` 헤더 사용
- Playwright smoke는 관리자 Bearer 토큰과 고객 `HG_SESSION` 쿠키를 backend API로 bootstrap해 로그인 rate limit과 UI 초기화 타이밍 영향을 줄였다.
- `P8-8`은 guest 주문·8회권·예약을 만든 뒤, 같은 번호의 회원이 `/my` 모달에서 재인증 후 claim 하는 흐름을 검증한다.
- `P8-9`는 guest 주문 성공 화면에서 회원가입으로 넘어가 휴대폰/이름 prefill과 `/my` claim 모달 자동 오픈을 검증한다.
- 기본 관리자 계정: `admin` / `admin1234` (Flyway V11로 정합화)
- 개발/테스트에서는 `X-Admin-Key` 폴백 가능 (`enable-api-key-auth=true`)
- 현재 세션 저장소는 인메모리 단일 인스턴스 기준이다. 운영에서 인스턴스가 늘어나면 JWT 기반 인증 전환을 우선 검토한다.
- API 에러 응답은 필요 시 `requestId`를 포함하고, 배치 로그도 `batch-*` requestId를 같이 남긴다.
- 슬롯 생성: 공개 `/classes` API로 클래스 드롭다운 제공 (API 없을 시 ID 직접 입력 폴백)
- 주문 총액: `OrderItemsForm`에서 상품 가격 × 수량으로 실시간 합계 표시
- `BookingFormStep`의 결제 방식 라디오는 명시적 `id`를 써서 라벨 접근성을 보장

### 로컬 실행 메모
- `local` 프로필 `:app:bootRun`은 `classes` 테이블이 비어 있으면 향수/우드/니트 기본 클래스 3종을 seed한다.
- clean DB 기준으로도 P8 guest/member smoke 1~9를 바로 실행할 수 있다.
- `DELETE /api/v1/admin/dev/payment/refunds/fail-next`로 훅을 비우고, `POST /api/v1/admin/dev/payment/refunds/fail-next`로 다음 환불 1회 실패를 arm할 수 있다.
  요청 바디에 `orderId`를 넣으면 특정 주문으로 범위를 좁힐 수 있다.

### 테스트 실행

```bash
./gradlew test
./gradlew :app:test --tests "*.SomeIT"
./gradlew :app:policyTest
./gradlew --no-daemon :app:useCaseTest
./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.admin.AdminSlotUseCaseIT
cd frontend && npm run build
cd frontend && npm run e2e:install
cd frontend && npm run e2e
```

### 미해결 과제
- 로컬 `bootRun` 전 `happygallery-app` 컨테이너가 떠 있으면 8080 충돌 발생
- PG 환불 패턴 중복 → 실 PG 연동 시 RefundExecutor로 통합 예정
- ~~공개 주문 상세 `fulfillment.status` 계약 drift 정리 필요~~ (CR-P6에서 FE/BE 정합)
- ~~`X-Admin-Id` 헤더 의존 제거 전까지 운영 이력의 admin 식별자가 null/위조 가능~~ (CR-P6에서 Bearer 세션 attribute 기반으로 전환)
- ~~`fulfillments.order_id` unique 부재로 단일 fulfillment 가정이 DB에서 아직 보장되지 않음~~ (CR-P6에서 V13 migration으로 unique 제약 추가)
- ~~배송 상태 enum(`SHIPPING_PREPARING`, `SHIPPED`, `DELIVERED`)은 있으나 운영 API/화면 흐름과 `expectedShipDate` write guard는 미완성~~ (CR-P7에서 배송 전이 API 3종, 이력 조회, write guard 구현)
- ~~`DELAY_REQUESTED` → 재개 경로 없음~~ (CR-P5에서 `resumeProduction` 추가)
- ~~Fulfillment.status와 Order.status 이중 관리~~ (CR-P5에서 Fulfillment.status 제거, Order.status가 단일 소스)

---

## ADR 목록

| 번호 | 주제 |
|------|------|
| ADR-0001 | 핵심 스키마 |
| ADR-0002 | 상태 전이 가드 |
| ADR-0003 | 슬롯 동시성 전략 |
| ADR-0004 | 슬롯 관리 구현 |
| ADR-0005 | 게스트 예약 구현 |
| ADR-0006 | 예약 변경 결정 |
| ADR-0007 | 예약 취소 결정 |
| ADR-0008 | 결제 인터페이스 추상화 |
| ADR-0009 | 예약금 결제 정책 |
| ADR-0010 | 8회권 구매/만료 결정 |
| ADR-0011 | 8회권 사용/소모/환불 결정 |
| ADR-0012 | 상품/재고 구현 결정 (§8.1) |
| ADR-0013 | 주문 승인 모델 결정 (§8.2) |
| ADR-0014 | 예약 제작 주문 구현 결정 (§8.3) |
| ADR-0015 | 운영 로그 구조화 및 비즈니스 예외 스택 최적화 |
| ADR-0016 | API URI 버저닝 전략 도입 |
| ADR-0017 | 필터 기반 API 처리율 제한 도입 |
| ADR-0018 | 환불 이력 저장 트랜잭션 분리(REQUIRES_NEW) |
| ADR-0019 | 비밀번호 해시 정책 (Salt + Key Stretching) |
| ADR-0020 | 결제 환불 외부 호출 보호를 위한 CircuitBreaker 도입 |
| ADR-0021 | 기존 app/domain/infra 구조 위에서 점진적 헥사고날 전환 채택 |

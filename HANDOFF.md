# HANDOFF.md
> 다음 세션을 위한 인수인계 문서.
> 작성 시점: 2026-03-14 (프론트 F0–F9 완료, P8·P9·P10 완료)

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
- 기준 확인 순서: `HANDOFF.md -> docs/PRD/0001_spec/spec.md -> docs/ADR/*`

---

## 현재 브랜치 / 워크트리 상태

- 권장 작업 브랜치: `codex/work-20260314`
- 최근 작업:
  - CR-P5 장기 리팩토링 — DELAY_REQUESTED 재개 흐름, Fulfillment 단일성, *_REFUNDED 상태명 변경, Fulfillment.status 제거, PG 환불 패턴 통합, local hook 범위 제한
  - CR-P1/P2 코드리뷰 후속 — admin 캐시 정합성, Playwright Bearer 전환, 로그인 rate limit, admin id 컨텍스트 주입
  - P10 관측성/운영 준비 — Actuator 노출 정책, 에러 응답 requestId 포함, 배치 MDC requestId 주입
- 프론트 생성물(`node_modules`, `dist`, `*.tsbuildinfo`)은 `frontend/.gitignore` 기준으로 추적 제외
- 최근 검증:
  - `cd frontend && npm run build` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.booking.LocalBookingClassSeedServiceTest` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.infra.payment.FakePaymentProviderTest --tests com.personal.happygallery.app.web.admin.LocalRefundFailureControllerTest --tests com.personal.happygallery.app.web.admin.AdminLoginUseCaseIT` 통과
  - `cd frontend && npx playwright test --list` 통과
  - `docker compose stop app` 후 로컬 `:app:bootRun` 기동 확인
  - `cd frontend && npm run e2e` 실행: 5 passed

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
- `/products/:id` — 상품 상세
- `/bookings/new` — 예약 생성
- `/bookings/manage` — 예약 조회/변경/취소
- `/passes/purchase` — 8회권 구매
- `/orders/new` — 주문 생성
- `/orders/detail` — 주문 조회
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
| 배포 전 | **P8** | 완료 | Playwright smoke 1~5 pass, local refund failure hook으로 재시도 시나리오까지 검증 완료 |
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

### 다음 추천 작업

1. ~~문서 정합화 — spec.md, ADR-0013, ADR-0014에 변경된 상태명과 Fulfillment 구조 반영~~ (완료)

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
- 관리자 API 401 처리: `onAuthError` 콜백을 AdminPage에서 모든 하위 컴포넌트에 전달
- 관리자 인증: `useAdminKey()` 훅에서 사용자명/비밀번호 로그인 → UUID 세션 토큰을 `sessionStorage` (`hg_admin_token`)에 저장, 이후 `Authorization: Bearer {token}` 헤더 사용
- 기본 관리자 계정: `admin` / `admin1234` (Flyway V11로 정합화)
- 개발/테스트에서는 `X-Admin-Key` 폴백 가능 (`enable-api-key-auth=true`)
- 현재 세션 저장소는 인메모리 단일 인스턴스 기준이다. 운영에서 인스턴스가 늘어나면 JWT 기반 인증 전환을 우선 검토한다.
- API 에러 응답은 필요 시 `requestId`를 포함하고, 배치 로그도 `batch-*` requestId를 같이 남긴다.
- 슬롯 생성: 공개 `/classes` API로 클래스 드롭다운 제공 (API 없을 시 ID 직접 입력 폴백)
- 주문 총액: `OrderItemsForm`에서 상품 가격 × 수량으로 실시간 합계 표시
- `BookingFormStep`의 결제 방식 라디오는 명시적 `id`를 써서 라벨 접근성을 보장

### 로컬 실행 메모
- `local` 프로필 `:app:bootRun`은 `classes` 테이블이 비어 있으면 향수/우드/니트 기본 클래스 3종을 seed한다.
- clean DB 기준으로도 P8 예약/8회권 smoke를 바로 실행할 수 있다.
- `DELETE /api/v1/admin/dev/payment/refunds/fail-next`로 훅을 비우고, `POST /api/v1/admin/dev/payment/refunds/fail-next`로 다음 환불 1회 실패를 arm할 수 있다.

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

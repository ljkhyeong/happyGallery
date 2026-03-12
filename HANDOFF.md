# HANDOFF.md
> 다음 세션을 위한 인수인계 문서.
> 작성 시점: 2026-03-12 (프론트 F0–F9 전 단위 완료, polish plan 진입)

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
- 기준 확인 순서: `HANDOFF.md -> docs/PRD/0001_spec/spec.md -> docs/ADR/*`

---

## 현재 브랜치 / 워크트리 상태

- 작업 브랜치: `jk-refactor2`
- 최근 작업 (2026-03-12):
  - F2 앱 셸/테마 안정화 — Pretendard 폰트 로딩, 네비 active 판정 수정, 관리자 링크 분리, 홈페이지 카드 UI
  - P2 홈페이지 구현 — 히어로 섹션 + 6개 서비스 진입 카드
  - F3 관리자 상품/슬롯 검증 — 401 자동 로그아웃+토스트, 슬롯 생성 클래스 드롭다운
  - P5 관리자 슬롯 조회 보강 — `/admin/slots?classId=` 추가, 클래스 선택 기반 슬롯 목록/예약 현황 UI
  - F8 관리자 운영 검증 — 주문/패스/환불 컴포넌트에 401 처리 추가
  - P3 폼/에러 UX 보강 진행 — 에러 코드 사용자 메시지 매핑, 전화번호/이름/금액 검증, 주문 총액 표시
  - 문서 동기화 — api-gap-analysis.md, frontend plan, HANDOFF.md 갱신
- 프론트 생성물(`node_modules`, `dist`, `*.tsbuildinfo`)은 `frontend/.gitignore` 기준으로 추적 제외
- 최근 검증:
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.admin.AdminSlotUseCaseIT` 통과
  - `cd frontend && npm run build` 통과
  - 전체 백엔드 테스트는 미실행 (필요 시 `./gradlew test` 추가 확인)

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
- `/admin` — 관리자 (X-Admin-Key 인증)

---

## 다음 우선순위 (polish plan)

폴리시 플랜: `docs/1Pager/0004_polish_plan/plan.md`

| 우선순위 | 단위 | 상태 | 내용 |
|------|------|------|------|
| 즉시 | **P1** | 완료 | 스펙 문서 동기화 |
| 즉시 | **P2** | 완료 | 홈페이지 구현 |
| 즉시 | **P3** | 진행 중 | 폼 검증 및 에러 UX 강화 |
| 다음 | **P4** | 미착수 | 반응형 UI 및 접근성 점검 |
| 다음 | **P5** | 완료 | 관리자 슬롯 조회 API 및 화면 보강 |
| 다음 | **P6** | 미착수 | 관리자 예약 조회/노쇼 처리 화면 |
| 다음 | **P7** | 미착수 | 관리자 주문 목록 조회 화면 |
| 배포 전 | **P8** | 미착수 | E2E 시나리오 검증 |
| 배포 전 | **P9** | 미착수 | 프로덕션 인증 계층 |
| 운영 | **P10** | 미착수 | 관측성 및 운영 준비 |

### 다음 추천 작업

1. `P3` — 폼 검증과 에러 UX 마무리 (남은 화면 전파)
2. `P6` → `P7` — 관리자 운영 화면 보강 (예약 조회, 주문 목록)
3. `P4` — 반응형/접근성

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

### 프론트 공통 패턴
- 관리자 API 401 처리: `onAuthError` 콜백을 AdminPage에서 모든 하위 컴포넌트에 전달
- 관리자 키: `sessionStorage` (`hg_admin_key`), `useAdminKey()` 훅으로 관리
- 슬롯 생성: 공개 `/classes` API로 클래스 드롭다운 제공 (API 없을 시 ID 직접 입력 폴백)
- 주문 총액: `OrderItemsForm`에서 상품 가격 × 수량으로 실시간 합계 표시

### 테스트 실행

```bash
./gradlew test
./gradlew :app:test --tests "*.SomeIT"
./gradlew :app:policyTest
./gradlew --no-daemon :app:useCaseTest
./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.admin.AdminSlotUseCaseIT
cd frontend && npm run build
```

### 미해결 과제
- PG 환불 패턴 중복 → 실 PG 연동 시 RefundExecutor로 통합 예정
- `DELAY_REQUESTED` → 재개 경로 없음 (ADR-0014)
- Fulfillment.status와 Order.status 이중 관리 → 불일치 위험 (ADR-0014)

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

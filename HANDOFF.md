# HANDOFF.md
> 다음 세션을 위한 인수인계 문서.
> 작성 시점: 2026-03-08 (리팩토링 R1–R10 완료, frontend plan F5 추가)

---

## 프로젝트 요약

**happyGallery** — 오프라인 공방의 온라인 쇼핑몰 + 체험 예약 시스템 (Spring Boot 4.0.2 / Java 21 / MySQL 8)

- 핵심 스펙: `docs/PRD/0001_spec/spec.md`
- 전체 구현 계획: `PLAN.md`
- 의사결정 기록: `docs/ADR/`
- 리팩토링 계획: `docs/1Pager/0002_refactoring_plan/plan.md`
- 프론트 착수 계획: `docs/1Pager/0003_frontend_plan/plan.md`
- 기준 확인 순서: `HANDOFF.md -> docs/PRD/0001_spec/spec.md -> docs/ADR/*`

---

## 현재 브랜치 / 워크트리 상태

- 작업 브랜치: `ljkhyeong/frontend-setup`
- 최근 작업:
  - `codexReview`의 R10 테스트 픽스처 정리 반영
  - B2 공개 상품/클래스/슬롯 조회 API 추가
  - F4 예약 조회/변경/취소 화면 추가
  - F5 공개 상품 카탈로그 화면 추가
- 프론트 생성물(`node_modules`, `dist`, `*.tsbuildinfo`)은 `frontend/.gitignore` 기준으로 추적 제외
- 최근 검증:
  - `./gradlew :app:policyTest` 통과
  - `./gradlew --no-daemon :app:useCaseTest`는 프론트 브랜치 기준 실패 6건 확인 후 별도 워크스페이스에서 수정 진행
  - `cd frontend && npm run build` 통과

---

## 프론트 진행 상황

프론트 플랜: `docs/1Pager/0003_frontend_plan/plan.md`

### 완료

| 단위 | 내용 | 주요 변경 파일 |
|------|------|----------------|
| **B1** | 프론트 선행 API 갭 분석 문서화 | `docs/1Pager/0003_frontend_plan/api-gap-analysis.md` |
| **B2** | 공개 상품/클래스/슬롯 조회 API 추가 | `ProductController`, `ClassController`, `SlotController`, `SlotRepository`, `docs/PRD/0001_spec/spec.md` |
| **F0** | 프론트 워크스페이스 스캐폴딩 | `frontend/package.json`, `frontend/vite.config.ts`, `frontend/src/**/*` |
| **F1** | 공통 API 클라이언트와 에러 처리 계층 | `frontend/src/shared/api/**/*`, `frontend/src/shared/types/**/*`, `frontend/src/shared/lib/**/*` |
| **F4** | 예약 조회/변경/취소 화면 | `frontend/src/features/booking-manage/**/*`, `frontend/src/pages/BookingManagePage.tsx`, `frontend/src/app/App.tsx` |
| **F5** | 공개 상품 카탈로그 화면 | `frontend/src/features/product/**/*`, `frontend/src/pages/ProductListPage.tsx`, `frontend/src/pages/ProductDetailPage.tsx`, `frontend/src/shared/ui/Layout.tsx` |

### 진행 중

| 단위 | 내용 | 주요 변경 파일 |
|------|------|----------------|
| **F2** | 앱 셸/테마 기초 | `frontend/src/shared/ui/**/*`, `frontend/src/styles/**/*`, `frontend/src/pages/NotFoundPage.tsx` |
| **F3** | 관리자 상품/슬롯 화면 MVP | `frontend/src/features/admin-product/**/*`, `frontend/src/features/admin-slot/**/*`, `frontend/src/pages/admin/AdminPage.tsx` |

### 다음 우선순위

- `F2`: 로딩/에러/empty 상태와 공통 셸 마무리
- `F3`: 관리자 상품/슬롯 화면 검증 후 세부 UX 보완
- `B3`: 8회권 구매 계약 보완
- `F6`: 예약 생성 화면 착수

---

## 리팩토링 진행 상황

리팩토링 플랜: `docs/1Pager/0002_refactoring_plan/plan.md`

### 완료 (R1–R10)

| 단위 | 내용 | 주요 변경 파일 |
|------|------|----------------|
| **R1** | Order 도메인 상태 전이 캡슐화 강화 | `OrderStatus.java`, `Order.java` |
| **R2** | API 예외 매핑 일관성 정리 | `ErrorCode.java`, `GlobalExceptionHandler.java` |
| **R3** | Booking 유스케이스 공통 절차 추출 | `BookingSupport.java` |
| **R4** | Notification fallback 전략 객체화 | `NotificationService.java`, `FakeKakaoSender`, `FakeSmsSender` |
| **R5** | Batch 서비스 공통 처리 템플릿화 | `BatchExecutor.java` |
| **R6** | Admin Controller DTO 변환 책임 정리 | `OrderProductionResponse`, `PickupResponse`, `AdminOrderController` |
| **R7** | Pass 도메인 계산/검증 메서드 명확화 | `PassPurchase.java`, `GuestBookingService` |
| **R8** | Product/Inventory 경계 정리 | `InventoryService.java`, `ProductAdminService`, `Inventory.deduct()` |
| **R9** | 시간 경계 계산 호출부 정리 | `TimeBoundary.java`, booking/pass 호출부 |
| **R10** | 테스트 픽스처/중복 유틸 정리 | `BookingTestHelper`, `OrderTestHelper`, `NotificationLogTestHelper`, `TestFixtures`, booking/order/pass use-case 테스트 |

---

## 구조 변경 요약

```
신규 파일:
  app/booking/BookingSupport.java
  app/batch/BatchExecutor.java
  app/src/test/.../support/OrderTestHelper.java
  app/src/test/.../support/NotificationLogTestHelper.java
  frontend/src/features/booking-manage/*
  frontend/src/features/product/*

삭제 파일:
  domain/product/InventoryPolicy.java

변경된 주요 패턴:
  NotificationService    ← List<NotificationSender> @Order 순회
  PassPurchase.java      ← requireUsable/hasRemainingCredits/calculateRefundAmount 추가
  TimeBoundary.java      ← LocalDateTime 오버로드 추가
  BookingRepository      ← 예약 조회 fetch join 보강
  ProductController      ← 공개 상품 목록 API 추가
  ClassController        ← 공개 클래스 목록 API 추가
  SlotController         ← 공개 예약 가능 슬롯 조회 API 추가
```

---

## 알아야 할 것들

### 리팩토링 원칙

- 기능 변경 없이 구조만 정리하는 방향 유지
- R10에서 예약 조회 테스트 중 드러난 LAZY 초기화 예외는 `BookingRepository.findDetailByIdAndAccessToken()` fetch join 추가로 보정
- 각 단위 완료 시 관련 Gradle 검증을 우선 수행

### Spring Boot 4.0 특이사항

- `@AutoConfigureMockMvc` 제거됨 → `MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build()` 패턴
- `@SpringBootTest` 컨텍스트에서 `ObjectMapper` autowire 불가 → JSON 문자열 직접 구성

### 테스트 실행

```bash
./gradlew test
./gradlew :app:test --tests "*.SomeIT"
./gradlew :app:policyTest
./gradlew --no-daemon :app:useCaseTest
cd frontend && npm run build
```

### 미해결 과제

- `BatchScheduler` cron — 시스템 TZ 기준. 운영 서버 `Asia/Seoul` 설정 여부 확인 필요
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

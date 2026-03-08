# HANDOFF.md
> 다음 Claude 세션을 위한 인수인계 문서.
> 작성 시점: 2026-03-08 (frontend plan F1 공통 API 계층 추가)

---

## 프로젝트 요약

**happyGallery** — 오프라인 공방의 온라인 쇼핑몰 + 체험 예약 시스템 (Spring Boot 4.0.2 / Java 21 / MySQL 8)

- 핵심 스펙: `docs/PRD/0001_spec/spec.md`
- 전체 구현 계획: `PLAN.md`
- 의사결정 기록: `docs/ADR/`
- **리팩토링 계획**: `docs/1Pager/0002_refactoring_plan/plan.md`
- **프론트 착수 계획**: `docs/1Pager/0003_frontend_plan/plan.md`
- 도메인별 작업 절차와 세부 테스트 분기는 각 `docs/1Pager/*/plan.md`와 저장소 규칙을 우선 참고하고, 여러 영역에 걸친 변경은 `HANDOFF.md -> docs/PRD/0001_spec/spec.md -> docs/ADR/*` 순으로 기준을 확인

---

## 현재 브랜치 / 워크트리 상태

- 작업 브랜치: `ljkhyeong/frontend-setup`
- 최근 작업: `frontend/src/shared/**/*` 공통 API/타입/포맷터 추가, `App.tsx`에 `QueryClientProvider` 연결
- 프론트 생성물(`node_modules`, `dist`, `*.tsbuildinfo`)은 `frontend/.gitignore` 기준으로 추적 제외
- 검증: `cd frontend && npm run build`

---

## 프론트 진행 상황

프론트 플랜: `docs/1Pager/0003_frontend_plan/plan.md`

### 완료

| 단위 | 내용 | 주요 변경 파일 |
|------|------|----------------|
| **B1** | 프론트 선행 API 갭 분석 문서화 | `docs/1Pager/0003_frontend_plan/api-gap-analysis.md` — 공개/관리자 API 현황, GAP-1~5, 프론트 선행 관계 정리 |
| **F0** | 프론트 워크스페이스 스캐폴딩 | `frontend/package.json`, `frontend/vite.config.ts`, `frontend/src/**/*` — Vite + React + TypeScript, 라우팅, `@` alias, `/api -> :8080` proxy |
| **F1** | 공통 API 클라이언트와 에러 처리 계층 | `frontend/src/shared/api/**/*`, `frontend/src/shared/types/**/*`, `frontend/src/shared/lib/**/*` — QueryClient, fetch 래퍼, `ApiError`, DTO 타입, 날짜/통화 포맷터 추가 |

### 다음 우선순위

- `F2`: 앱 셸, 테마, 공통 UI 기반
- `B2`: 공개 상품/클래스/슬롯 조회 API 추가
- 화면 단위에서 공통 API 계층을 실제로 사용하는 첫 기능(F3 또는 F4) 착수

---

## 리팩토링 진행 상황

리팩토링 플랜: `docs/1Pager/0002_refactoring_plan/plan.md`

### 완료 (R1–R9)

| 단위 | 내용 | 주요 변경 파일 |
|------|------|----------------|
| **R1** | Order 도메인 상태 전이 캡슐화 강화 | `OrderStatus.java` — 가드 메서드 4개 추가 (`requireInProduction`, `requireProductionCompletable`, `requireFulfillmentPending`, `requirePickupReady`). `Order.java` — 인라인 if 체크 5곳 → 가드 메서드 호출로 통일 |
| **R2** | API 예외 매핑 일관성 정리 | `ErrorCode.java` — `PHONE_VERIFICATION_FAILED` 정렬 이동, `INTERNAL_ERROR(500)` 추가. `GlobalExceptionHandler.java` — 500 catch-all 핸들러 추가, 인프라 예외 로깅 추가 |
| **R3** | Booking 유스케이스 공통 절차 추출 | `BookingSupport.java` (신규) — `findByToken()`, `recordHistory()`, `notifyBookingGuest()`. Cancel/Reschedule/Booking/Query 4개 서비스에서 `bookingHistoryRepository`+`notificationService` 의존 제거 |
| **R4** | Notification fallback 전략 객체화 | `NotificationService.java` — `FALLBACK_ORDER` 하드코딩 + `Map` 제거 → `List<NotificationSender>`를 `@Order` 순 순회. `FakeKakaoSender`/`FakeSmsSender`에 `@Order(1)`/`@Order(2)` |
| **R5** | Batch 서비스 공통 처리 템플릿화 | `BatchExecutor.java` (신규) — `execute(candidates, idExtractor, processor, label)`. 배치 3종 + 알림 배치 1종의 for-try-catch-집계 루프 제거 |
| **R6** | Admin Controller DTO 변환 책임 정리 | `OrderProductionResponse`/`PickupResponse`에 `from()` 팩토리 추가. `AdminOrderController` 5개 메서드 → 팩토리 1줄 호출로 단순화 |
| **R7** | Pass 도메인 계산/검증 메서드 명확화 | `PassPurchase.java` — `requireUsable(now)`, `hasRemainingCredits()`, `calculateRefundAmount()`, `useCredit()` 내부 가드. `GuestBookingService` 만료/잔여 인라인 체크 제거 |
| **R8** | Product/Inventory 경계 정리 | `InventoryPolicy.java` 삭제 → `Inventory.deduct()` 인라인. `InventoryService.create()` 추가. `ProductAdminService` → `InventoryService` 위임으로 쓰기 경로 통일 |
| **R9** | 시간 경계 계산 호출부 정리 | `TimeBoundary.java` — `LocalDateTime` 오버로드 3개 추가. 호출부(`BookingCancelService`, `BookingRescheduleService`, `PassPurchaseService`)에서 타입 변환 코드 제거 |

### 미완료: R10 — 테스트 픽스처/중복 유틸 정리

**범위:**
- `app/src/test/java/com/personal/happygallery/app/order/*`
- `app/src/test/java/com/personal/happygallery/app/booking/*`
- `app/src/test/java/com/personal/happygallery/app/pass/*`
- `app/src/test/java/com/personal/happygallery/support/*`

**작업 목표:**
- 테스트 데이터 생성/공통 assertion 중복을 지원 클래스로 정리
- 시나리오 가독성을 유지하면서 보일러플레이트 축소

**완료 조건:**
- 테스트 중복 코드 감소, 읽기 난도 낮아짐
- 테스트 실행 시간/신뢰도 회귀 없음

**최소 검증 명령:**
```bash
./gradlew --no-daemon :app:useCaseTest
```

**지시 템플릿:**
``` 
리팩토링 plan의 R10만 진행해줘. 기능 변경 없이 리팩토링만 하고, 결과는 변경 파일/핵심 의사결정/실행 테스트 형식으로 보고해줘.
```

**주의사항:**
- R7에서 `InventoryPolicyTest` 내용이 `Inventory.deduct()` 테스트로 변경됨 — R10에서 파일명/패키지 조정 가능
- R3에서 `BookingSupport`가 package-private으로 생성됨 — 테스트에서 직접 접근 불필요 (서비스 통합 테스트로 검증)
- 충돌 위험: `R7 -> R10` 순차 권장 (완료됨)

---

## 구조 변경 요약 (기존 HANDOFF 대비)

```
신규 파일:
  app/booking/BookingSupport.java         ← R3: 패키지 내부 헬퍼
  app/batch/BatchExecutor.java            ← R5: 배치 공통 실행기

삭제 파일:
  domain/product/InventoryPolicy.java     ← R8: Inventory.deduct()에 인라인

변경된 주요 패턴:
  OrderStatus.java       ← 가드 메서드 8개 (기존 3 + 신규 4 = requireInProduction/ProductionCompletable/FulfillmentPending/PickupReady)
  NotificationService    ← List<NotificationSender> @Order 순회 (Map + FALLBACK_ORDER 제거)
  PassPurchase.java      ← requireUsable(now), hasRemainingCredits(), calculateRefundAmount() 추가
  TimeBoundary.java      ← LocalDateTime 오버로드 3개 추가
  InventoryService.java  ← create() 추가 (쓰기 단일 진입점)
  ErrorCode.java         ← INTERNAL_ERROR(500) 추가
  GlobalExceptionHandler ← 500 catch-all + 인프라 예외 로깅
```

---

## 알아야 할 것들

### 리팩토링 원칙
- 기능 변경 없이 구조만 정리 — HTTP 계약/상태 결과 변화 없음
- 각 단위 완료 시 `./gradlew test` 전체 통과 확인됨
- R10은 미완료이며, 필요 시 `./gradlew --no-daemon :app:useCaseTest` 기준으로 이어서 진행

### Spring Boot 4.0 특이사항
- `@AutoConfigureMockMvc` 제거됨 → `MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build()` 패턴
- `@SpringBootTest` 컨텍스트에서 `ObjectMapper` autowire 불가 → JSON 문자열 직접 구성

### 테스트 실행
```bash
./gradlew test                           # 전체
./gradlew :app:test --tests "*.SomeIT"   # 단일 클래스
./gradlew --no-daemon :app:useCaseTest   # R10 검증용
```

### 미해결 과제 (이전 세션에서 이어짐)
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

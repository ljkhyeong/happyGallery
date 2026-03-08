# HANDOFF.md
> 다음 Claude 세션을 위한 인수인계 문서.
> 작성 시점: 2026-03-08 (리팩토링 R1–R10 완료, R2 재검토 보강 반영)

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

## 미커밋 상태

- `codexReview`에는 PR #24(`테스트 픽스처 정리와 예약 조회 보강`)가 머지되어 R10 테스트 리팩터링과 예약 조회 fetch join 보강이 반영됨
- 현재 워크스페이스에는 R2 재검토 보강만 미커밋 상태
  - `ErrorCode.CONFLICT(409)` 추가
  - `GlobalExceptionHandler` 예약/비예약 충돌 응답 분리
  - `GlobalExceptionHandlerTest` 추가
  - `spec.md`, `HANDOFF.md` 갱신
- 검증 상태
  - `./gradlew --no-daemon :app:useCaseTest` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderApprovalUseCaseIT --tests com.personal.happygallery.app.order.PickupExpireBatchUseCaseIT` 통과
  - `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.web.admin.AdminSlotUseCaseIT --tests com.personal.happygallery.app.web.GlobalExceptionHandlerTest` 통과

---

## 리팩토링 진행 상황

리팩토링 플랜: `docs/1Pager/0002_refactoring_plan/plan.md`

### 완료 (R1–R10)

| 단위 | 내용 | 주요 변경 파일 |
|------|------|----------------|
| **R1** | Order 도메인 상태 전이 캡슐화 강화 | `OrderStatus.java` — 가드 메서드 4개 추가 (`requireInProduction`, `requireProductionCompletable`, `requireFulfillmentPending`, `requirePickupReady`). `Order.java` — 인라인 if 체크 5곳 → 가드 메서드 호출로 통일 |
| **R2** | API 예외 매핑 일관성 정리 | `ErrorCode.java` — `PHONE_VERIFICATION_FAILED` 정렬 이동, `INTERNAL_ERROR(500)` 및 일반 `CONFLICT(409)` 추가. `GlobalExceptionHandler.java` — 500 catch-all 핸들러 추가, 인프라 예외 로깅 추가, 예약/비예약 충돌 응답 분리 |
| **R3** | Booking 유스케이스 공통 절차 추출 | `BookingSupport.java` (신규) — `findByToken()`, `recordHistory()`, `notifyBookingGuest()`. Cancel/Reschedule/Booking/Query 4개 서비스에서 `bookingHistoryRepository`+`notificationService` 의존 제거 |
| **R4** | Notification fallback 전략 객체화 | `NotificationService.java` — `FALLBACK_ORDER` 하드코딩 + `Map` 제거 → `List<NotificationSender>`를 `@Order` 순 순회. `FakeKakaoSender`/`FakeSmsSender`에 `@Order(1)`/`@Order(2)` |
| **R5** | Batch 서비스 공통 처리 템플릿화 | `BatchExecutor.java` (신규) — `execute(candidates, idExtractor, processor, label)`. 배치 3종 + 알림 배치 1종의 for-try-catch-집계 루프 제거 |
| **R6** | Admin Controller DTO 변환 책임 정리 | `OrderProductionResponse`/`PickupResponse`에 `from()` 팩토리 추가. `AdminOrderController` 5개 메서드 → 팩토리 1줄 호출로 단순화 |
| **R7** | Pass 도메인 계산/검증 메서드 명확화 | `PassPurchase.java` — `requireUsable(now)`, `hasRemainingCredits()`, `calculateRefundAmount()`, `useCredit()` 내부 가드. `GuestBookingService` 만료/잔여 인라인 체크 제거 |
| **R8** | Product/Inventory 경계 정리 | `InventoryPolicy.java` 삭제 → `Inventory.deduct()` 인라인. `InventoryService.create()` 추가. `ProductAdminService` → `InventoryService` 위임으로 쓰기 경로 통일 |
| **R9** | 시간 경계 계산 호출부 정리 | `TimeBoundary.java` — `LocalDateTime` 오버로드 3개 추가. 호출부(`BookingCancelService`, `BookingRescheduleService`, `PassPurchaseService`)에서 타입 변환 코드 제거 |
| **R10** | 테스트 픽스처/중복 유틸 정리 | `BookingTestHelper`에 생성 결과 record + 검증 포함 예약 생성 API 추가. `OrderTestHelper`/`NotificationLogTestHelper` 신규 추가. `TestFixtures`에 `booking()`/`passPurchase()` 추가. booking/order/pass use-case 테스트의 반복 fixture와 비동기 로그 대기 중복 정리 |

---

## 구조 변경 요약 (기존 HANDOFF 대비)

```
신규 파일:
  app/booking/BookingSupport.java         ← R3: 패키지 내부 헬퍼
  app/batch/BatchExecutor.java            ← R5: 배치 공통 실행기
  app/src/test/.../support/OrderTestHelper.java   ← R10: 주문 테스트 fixture 지원
  app/src/test/.../support/NotificationLogTestHelper.java   ← R10: 알림 로그 polling 지원

삭제 파일:
  domain/product/InventoryPolicy.java     ← R8: Inventory.deduct()에 인라인

변경된 주요 패턴:
  OrderStatus.java       ← 가드 메서드 8개 (기존 3 + 신규 4 = requireInProduction/ProductionCompletable/FulfillmentPending/PickupReady)
  NotificationService    ← List<NotificationSender> @Order 순회 (Map + FALLBACK_ORDER 제거)
  PassPurchase.java      ← requireUsable(now), hasRemainingCredits(), calculateRefundAmount() 추가
  TimeBoundary.java      ← LocalDateTime 오버로드 3개 추가
  InventoryService.java  ← create() 추가 (쓰기 단일 진입점)
  ErrorCode.java         ← INTERNAL_ERROR(500), CONFLICT(409) 추가
  GlobalExceptionHandler ← 500 catch-all + 인프라 예외 로깅 + 예약/비예약 충돌 매핑 분리
  BookingTestHelper      ← CreatedBooking record + verified booking/pass booking 생성 메서드
  TestFixtures.java      ← booking()/passPurchase() fixture 추가
  BookingRepository      ← findDetailByIdAndAccessToken() fetch join 추가로 예약 조회 LAZY 예외 방지
```

---

## 알아야 할 것들

### 리팩토링 원칙
- 기능 변경 없이 구조만 정리 — HTTP 계약/상태 결과 변화 없음
- R10 중 예약 조회 테스트에서 드러난 LAZY 초기화 예외는 `BookingRepository.findDetailByIdAndAccessToken()` fetch join 추가로 보정함
- 각 단위 완료 시 관련 Gradle 검증 통과 확인
- R10 정리분은 PR #24로 `codexReview`에 반영됨. 현재 로컬 미커밋은 R2 재검토 보강만 남아 있음

### Spring Boot 4.0 특이사항
- `@AutoConfigureMockMvc` 제거됨 → `MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build()` 패턴
- `@SpringBootTest` 컨텍스트에서 `ObjectMapper` autowire 불가 → JSON 문자열 직접 구성

### 테스트 실행
```bash
./gradlew test                           # 전체
./gradlew :app:test --tests "*.SomeIT"   # 단일 클래스
./gradlew --no-daemon :app:useCaseTest   # R10 검증 완료
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

# Refactoring Plan (Unit-driven)

## 1) 목적
- 기능 변경 없이 구조를 정리해 유지보수성과 테스트 신뢰도를 높인다.
- 에이전트가 `단위 1개`씩 맡아 병렬 또는 순차로 안전하게 진행할 수 있게 분해한다.

## 2) 기준 문서
- 스펙: `docs/PRD/0001_spec/spec.md`
- 설계 결정: `docs/ADR/*`
- 현재 상태: `HANDOFF.md`

## 3) 운영 규칙
- 한 번의 지시는 `단위 1개`만 선택한다.
- 단위별 PR 생성, 머지 기준은 `코드 + 테스트 통과 + 문서 반영`이다.
- 도메인 규칙은 `domain`, 유스케이스 orchestration은 `app`, 외부 연동은 `infra`에만 둔다.
- Testcontainers 대상 검증은 `--no-daemon`으로 실행한다.

## 4) 지시 템플릿
`리팩토링 plan의 [단위ID]만 진행해줘. 기능 변경 없이 리팩토링만 하고, 결과는 변경 파일/핵심 의사결정/실행 테스트 형식으로 보고해줘.`

예시:
- `리팩토링 plan의 R2만 진행해줘. 예외 매핑 일관성만 집중해서.`
- `리팩토링 plan의 R7만 진행해줘. 배치 중복 로직 제거 중심으로.`

## 5) 단위 우선순위
- P0: R1, R2, R3, R4
- P1: R5, R6, R7, R8
- P2: R9, R10

---

## R1. Order 도메인 상태 전이 캡슐화 강화
### 범위
- `domain/src/main/java/com/personal/happygallery/domain/order/Order.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/OrderStatus.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/Fulfillment.java`
- `app/src/main/java/com/personal/happygallery/app/order/OrderApprovalService.java`

### 작업 목표
- 상태 전이 검증 로직을 도메인 메서드로 모아 서비스의 분기/중복 조건을 축소한다.
- 승인/거절/자동환불/픽업완료 전이 조건을 한 위치에서 읽히게 만든다.

### 완료 조건
- 서비스 계층의 상태 체크 중복이 줄고, 도메인 메서드 호출 중심으로 정리되었다.
- 기존 동작(HTTP 계약/상태 결과) 변화가 없다.

### 최소 검증 명령
- `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderApprovalUseCaseIT --tests com.personal.happygallery.app.order.PickupExpireBatchUseCaseIT`

---

## R2. API 예외 매핑 일관성 정리
### 범위
- `app/src/main/java/com/personal/happygallery/app/web/GlobalExceptionHandler.java`
- `common/src/main/java/com/personal/happygallery/common/error/ErrorCode.java`
- `common/src/main/java/com/personal/happygallery/common/error/*.java`

### 작업 목표
- 에러 코드/HTTP 상태/메시지 매핑 기준을 통일한다.
- 중복 예외 처리 분기를 축소하고 누락된 도메인 예외를 명시적으로 연결한다.

### 완료 조건
- 예외별 응답 포맷과 status 매핑이 한 규칙으로 설명 가능하다.
- 관리자/게스트 API 모두 기존 계약과 호환된다.

### 최소 검증 명령
- `./gradlew :app:test --tests com.personal.happygallery.app.web.admin.AdminSlotUseCaseIT`

---

## R3. Booking 유스케이스 공통 절차 추출
### 범위
- `app/src/main/java/com/personal/happygallery/app/booking/GuestBookingService.java`
- `app/src/main/java/com/personal/happygallery/app/booking/BookingRescheduleService.java`
- `app/src/main/java/com/personal/happygallery/app/booking/BookingCancelService.java`
- `app/src/main/java/com/personal/happygallery/app/booking/BookingQueryService.java`

### 작업 목표
- 게스트 조회/토큰 검증/이력 기록/알림 호출의 반복 패턴을 내부 헬퍼 또는 전용 컴포넌트로 추출한다.
- 서비스 간 책임 경계를 명확히 해서 변경 파급 범위를 줄인다.

### 완료 조건
- 3개 이상 서비스에 중복된 절차 코드가 제거되었다.
- 동작 시나리오(생성/변경/취소)의 결과가 동일하다.

### 최소 검증 명령
- `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.booking.BookingCancelUseCaseIT --tests com.personal.happygallery.app.booking.ConcurrentBookingUseCaseIT`

---

## R4. Notification fallback 전략 객체화
### 범위
- `app/src/main/java/com/personal/happygallery/app/notification/NotificationService.java`
- `infra/src/main/java/com/personal/happygallery/infra/notification/NotificationSender.java`
- `infra/src/main/java/com/personal/happygallery/infra/notification/FakeKakaoSender.java`
- `infra/src/main/java/com/personal/happygallery/infra/notification/FakeSmsSender.java`

### 작업 목표
- 채널 선택/실패 fallback/로그 기록 흐름을 전략화해서 테스트 가능성을 높인다.
- 채널 추가 시 기존 서비스 수정량을 최소화한다.

### 완료 조건
- `NotificationService`의 채널 분기 복잡도가 감소했다.
- 카카오 실패 시 SMS fallback 동작이 기존과 동일하다.

### 최소 검증 명령
- `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.pass.PassExpiryNotificationUseCaseIT --tests com.personal.happygallery.app.booking.BookingCancelUseCaseIT`

---

## R5. Batch 서비스 공통 처리 템플릿화
### 범위
- `app/src/main/java/com/personal/happygallery/app/order/OrderAutoRefundBatchService.java`
- `app/src/main/java/com/personal/happygallery/app/order/PickupExpireBatchService.java`
- `app/src/main/java/com/personal/happygallery/app/pass/PassExpiryBatchService.java`
- `app/src/main/java/com/personal/happygallery/app/batch/BatchResult.java`

### 작업 목표
- 배치의 `조회 -> 건별 처리 -> 집계 -> 결과 반환` 패턴을 공통 템플릿으로 정리한다.
- 건별 실패 격리/집계 규칙을 서비스마다 동일하게 맞춘다.

### 완료 조건
- 배치 3종에서 공통 구조가 드러나고 중복 코드가 감소했다.
- 실패 건 처리 정책(계속 진행)이 보장된다.

### 최소 검증 명령
- `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.PickupExpireBatchUseCaseIT --tests com.personal.happygallery.app.order.OrderApprovalUseCaseIT --tests com.personal.happygallery.app.pass.PassPurchaseUseCaseIT`

---

## R6. Admin Controller DTO 변환 책임 정리
### 범위
- `app/src/main/java/com/personal/happygallery/app/web/admin/AdminOrderController.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/AdminPassController.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/AdminRefundController.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/dto/*.java`

### 작업 목표
- 컨트롤러 내부의 응답 조립 로직을 DTO 팩토리/매퍼로 이동해 controller를 얇게 만든다.
- admin 응답 모델의 필드 규칙(nullable/필수)을 코드에서 명확히 보이게 한다.

### 완료 조건
- 컨트롤러 메서드가 요청 검증 + 서비스 호출 + 반환 중심으로 단순화되었다.
- API 응답 필드/이름/의미는 기존과 동일하다.

### 최소 검증 명령
- `./gradlew :app:test --tests com.personal.happygallery.app.web.admin.AdminSlotUseCaseIT --tests com.personal.happygallery.app.order.OrderProductionUseCaseIT`

---

## R7. Pass 도메인 계산/검증 메서드 명확화
### 범위
- `domain/src/main/java/com/personal/happygallery/domain/pass/PassPurchase.java`
- `domain/src/main/java/com/personal/happygallery/domain/pass/PassLedger.java`
- `app/src/main/java/com/personal/happygallery/app/pass/PassPurchaseService.java`
- `app/src/main/java/com/personal/happygallery/app/pass/PassRefundService.java`

### 작업 목표
- 크레딧 사용/환불/만료 관련 계산과 guard 로직을 도메인 중심으로 재배치한다.
- 서비스의 숫자 계산/상태 검증 중복을 줄인다.

### 완료 조건
- 패스 관련 수치 계산 규칙이 엔티티/값 객체 중심으로 읽힌다.
- 잔여 크레딧/원가 계산 결과가 기존과 동일하다.

### 최소 검증 명령
- `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.pass.PassCreditUsageUseCaseIT --tests com.personal.happygallery.app.pass.PassPurchaseUseCaseIT`

---

## R8. Product/Inventory 경계 정리
### 범위
- `app/src/main/java/com/personal/happygallery/app/product/InventoryService.java`
- `app/src/main/java/com/personal/happygallery/app/product/ProductAdminService.java`
- `domain/src/main/java/com/personal/happygallery/domain/product/Inventory.java`
- `infra/src/main/java/com/personal/happygallery/infra/product/InventoryRepository.java`

### 작업 목표
- 재고 차감/복원 정책 책임을 명확히 하여 서비스와 도메인 경계 혼선을 줄인다.
- 락/동시성 처리 경로를 단일 패턴으로 통일한다.

### 완료 조건
- 재고 관련 정책이 한눈에 추적 가능해졌다.
- 동시 주문 시나리오의 결과가 기존과 동일하다.

### 최소 검증 명령
- `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.ConcurrentOrderUseCaseIT`

---

## R9. 시간 경계 계산 호출부 정리
### 범위
- `common/src/main/java/com/personal/happygallery/common/time/TimeBoundary.java`
- `app/src/main/java/com/personal/happygallery/app/booking/BookingCancelService.java`
- `app/src/main/java/com/personal/happygallery/app/booking/BookingRescheduleService.java`
- `app/src/main/java/com/personal/happygallery/app/pass/PassExpiryBatchService.java`

### 작업 목표
- 시간 경계 계산 호출 방식을 통일해 조건문 분산을 줄인다.
- 타임존/경계시간 의도를 메서드 이름으로 드러나게 정리한다.

### 완료 조건
- 날짜 경계 관련 로직이 호출부에서 단순해졌다.
- D-1, 1시간 경계, 만료 7일 전 계산 결과가 동일하다.

### 최소 검증 명령
- `./gradlew :app:policyTest`
- `./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.pass.PassExpiryNotificationUseCaseIT`

---

## R10. 테스트 픽스처/중복 유틸 정리
### 범위
- `app/src/test/java/com/personal/happygallery/app/order/*`
- `app/src/test/java/com/personal/happygallery/app/booking/*`
- `app/src/test/java/com/personal/happygallery/app/pass/*`
- `app/src/test/java/com/personal/happygallery/support/*`

### 작업 목표
- 테스트 데이터 생성/공통 assertion 중복을 지원 클래스로 정리한다.
- 시나리오 가독성을 유지하면서 보일러플레이트를 줄인다.

### 완료 조건
- 테스트 중복 코드가 감소하고 읽기 난도가 낮아졌다.
- 테스트 실행 시간/신뢰도에 회귀가 없다.

### 최소 검증 명령
- `./gradlew --no-daemon :app:useCaseTest`

---

## 6) 권장 실행 순서
1. R1-R4 (핵심 유스케이스/계약 안정화)
2. R5-R8 (배치/도메인 책임 재배치)
3. R9-R10 (경계 조건/테스트 품질 정리)

## 7) 병렬화 가이드
- 동시에 진행 가능: `R2`, `R4`, `R8`
- 충돌 위험 높음(순차 권장): `R1 -> R5`, `R3 -> R9`, `R7 -> R10`
- 동일 파일 동시 수정 금지: `OrderApprovalService`, `PassExpiryBatchService`, `GlobalExceptionHandler`

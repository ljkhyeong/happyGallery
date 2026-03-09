# Code Review Plan (Unit-driven)

## 1) 목적
- 출시 전 리뷰를 "작업 단위"로 쪼개서, 한 번에 한 주제만 정확히 검토한다.
- 스타일보다 동작 버그, 스펙 불일치, 데이터 정합성, 테스트 누락을 우선한다.

## 2) 기준 문서
- 스펙: `docs/PRD/0001_spec/spec.md`
- 설계 판단: `docs/ADR/*`
- 작업 인수인계: `HANDOFF.md`

## 3) 사용 규칙
- 한 번의 지시는 `단위 1개`만 선택한다.
- 산출물은 아래 3가지만 요청한다.
  - `머지 전 수정 필요`
  - `후속 이슈`
  - `테스트/리스크`
- 단위 완료 후 다음 단위로 넘어간다.
- 큰 범위를 "전체 리뷰"로 한 번에 요청하지 않는다.
- 테스트 코드 리뷰 시 공통 기준:
  - 테스트 메서드는 `@DisplayName` 한글 문장을 사용한다.
  - 다중 검증 테스트는 `SoftAssertions.assertSoftly` 사용을 우선한다.
  - nullable 객체 비교는 `Objects.equals(a, b)`로 null-safe하게 처리한다.

## 4) 지시 템플릿
`코드리뷰 plan의 [단위ID]만 진행해줘. [관점] 기준으로 보고, 결과는 머지 전 수정 필요/후속 이슈/테스트·리스크 형식으로 줘.`

예시:
- `코드리뷰 plan의 U2만 진행해줘. 정합성과 중복 처리 위험 중심으로 보고, 결과는 3분류로 줘.`
- `코드리뷰 plan의 U6만 진행해줘. API 계약과 정보 노출 범위만 집중해서 봐줘.`

## 5) 단위 목록 (우선순위)
- P0: U1, U2, U3, U4, U6
- P1: U5, U8, U9, U10, U11
- P2: U7, U12

---

## U1. 주문 승인/거절 상태 전이 + 승인 이력
### 범위
- `app/src/main/java/com/personal/happygallery/app/order/OrderApprovalService.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/Order.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/OrderApprovalHistory.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/OrderApprovalDecision.java`
- `infra/src/main/java/com/personal/happygallery/infra/order/OrderApprovalHistoryRepository.java`

### 확인 포인트
- 승인/거절 상태 전이가 스펙과 일치하는지
- 승인 이력 누락/중복 가능성이 없는지
- 주문 상태 변경과 이력 저장의 원자성이 깨지지 않는지

### 완료 조건
- 상태 전이 버그/이력 정합성 리스크를 식별하고 우선순위를 제시했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderApprovalUseCaseIT`

---

## U2. 주문 자동환불 배치 건별 처리
### 범위
- `app/src/main/java/com/personal/happygallery/app/order/OrderAutoRefundBatchService.java`
- `app/src/main/java/com/personal/happygallery/app/order/OrderAutoRefundProcessor.java`

### 확인 포인트
- 건별 실패가 전체 배치를 중단시키지 않는지
- 중복 환불/누락 처리 가능성이 없는지
- 성공/실패 집계가 실제 결과와 일치하는지

### 완료 조건
- 배치 정합성 관점의 치명/중요 리스크를 분류했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderApprovalUseCaseIT`

---

## U3. 픽업 만료 배치 + 수동 픽업 완료 충돌
### 범위
- `app/src/main/java/com/personal/happygallery/app/order/PickupExpireBatchService.java`
- `app/src/main/java/com/personal/happygallery/app/order/PickupExpireProcessor.java`
- `app/src/main/java/com/personal/happygallery/app/order/OrderPickupService.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/Fulfillment.java`

### 확인 포인트
- `PICKUP_READY -> PICKED_UP / PICKUP_EXPIRED_REFUNDED` 충돌 시 안전한지
- 환불/재고/fulfillment 상태가 원자적으로 맞물리는지
- 동시 실행 시 중복 상태 전이 위험이 없는지

### 완료 조건
- 경합 시나리오 기준 회귀 위험 여부를 판정했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.PickupExpireBatchUseCaseIT`

---

## U4. 낙관적 락 + 재시도 정책 + 마이그레이션
### 범위
- `domain/src/main/java/com/personal/happygallery/domain/order/Order.java`
- `domain/src/main/java/com/personal/happygallery/domain/order/Fulfillment.java`
- `app/src/main/java/com/personal/happygallery/config/RetryConfig.java`
- `app/src/main/resources/db/migration/V6__add_order_version_columns.sql`

### 확인 포인트
- `@Version`과 DDL이 실제로 일치하는지
- 재시도 횟수/백오프가 과도하거나 부족하지 않은지
- 기존 데이터/운영 배포 관점에서 migration이 안전한지

### 완료 조건
- 락/재시도/DDL의 불일치 여부를 확정했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:compileJava :app:compileTestJava`

---

## U5. 배치 공통 로깅 AOP
### 범위
- `app/src/main/java/com/personal/happygallery/app/batch/BatchJob.java`
- `app/src/main/java/com/personal/happygallery/app/batch/BatchLoggingAspect.java`
- `app/src/main/java/com/personal/happygallery/app/batch/BatchScheduler.java`

### 확인 포인트
- AOP가 예외를 삼키지 않는지
- 로그 메시지가 실제 실행 결과를 왜곡하지 않는지
- 배치명 fallback 규칙이 운영 식별에 충분한지

### 완료 조건
- 로그 정확성/관측성 리스크를 정리했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.batch.BatchLoggingAspectTest`

---

## U6. BatchResult + BatchResponse API 계약
### 범위
- `app/src/main/java/com/personal/happygallery/app/batch/BatchResult.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/dto/BatchResponse.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/AdminOrderController.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/AdminPassController.java`
- `docs/PRD/0001_spec/spec.md` (11-F.3, 11-G.1)

### 확인 포인트
- 내부 결과 모델과 외부 응답 DTO 책임 분리가 적절한지
- `failureReasons`가 스펙 계약과 일치하는지
- 내부 예외 정보 과다 노출/과소 노출 리스크가 없는지

### 완료 조건
- API 계약 불일치 여부와 수정 우선순위를 확정했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.batch.BatchResultTest --tests com.personal.happygallery.app.web.admin.dto.BatchResponseTest`

---

## U7. Admin 보조 DTO (NoShow/FailedRefund)
### 범위
- `app/src/main/java/com/personal/happygallery/app/web/admin/AdminBookingController.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/AdminRefundController.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/dto/BookingNoShowResponse.java`
- `app/src/main/java/com/personal/happygallery/app/web/admin/dto/FailedRefundResponse.java`
- `docs/PRD/0001_spec/spec.md` (11-H)

### 확인 포인트
- DTO 전환 시 기존 계약 회귀가 없는지
- nullable 필드(`bookingId`, `orderId`) 매핑이 안전한지
- 운영자가 필요한 필드가 빠지지 않았는지

### 완료 조건
- 계약 회귀/널 처리 이슈를 분류했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.booking.BookingCancelUseCaseIT`

---

## U8. 8회권 만료 배치
### 범위
- `app/src/main/java/com/personal/happygallery/app/pass/PassExpiryBatchService.java`

### 확인 포인트
- 만료 시 크레딧/ledger 기록이 정확한지
- 중복 만료 처리 위험이 없는지
- 성공 건수와 실제 반영 건수가 일치하는지

### 완료 조건
- 만료 배치 정합성 리스크를 확정했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.pass.PassPurchaseUseCaseIT`

---

## U9. 만료 7일 전 알림 + 중복 발송 방지
### 범위
- `app/src/main/java/com/personal/happygallery/app/pass/PassExpiryBatchService.java`
- `infra/src/main/java/com/personal/happygallery/infra/notification/NotificationLogRepository.java`

### 확인 포인트
- 7일 전 조건 계산이 스펙과 일치하는지
- 동일 대상 하루 1회 보장이 되는지
- 실패/재시도 시 누락 리스크가 없는지

### 완료 조건
- 알림 중복/누락 리스크를 분류했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.pass.PassExpiryNotificationUseCaseIT`

---

## U10. 주문/배치 회귀 테스트 충분성
### 범위
- `app/src/test/java/com/personal/happygallery/app/order/*`

### 확인 포인트
- 승인/자동환불/픽업만료 핵심 시나리오가 커버되는지
- 실패 경로/경합 경로 테스트가 충분한지

### 완료 조건
- 테스트 누락 목록과 추가 우선순위를 제시했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.order.OrderApprovalUseCaseIT --tests com.personal.happygallery.app.order.PickupExpireBatchUseCaseIT --tests com.personal.happygallery.app.order.OrderProductionUseCaseIT`

---

## U11. 패스/예약 환불/Admin 응답 테스트 충분성
### 범위
- `app/src/test/java/com/personal/happygallery/app/pass/*`
- `app/src/test/java/com/personal/happygallery/app/booking/BookingCancelUseCaseIT.java`

### 확인 포인트
- DTO 계약 검증이 핵심 필드를 막는지
- 경계값/예외 경로 검증이 충분한지

### 완료 조건
- 테스트 보강 필요 지점을 시나리오 단위로 정리했다.

### 최소 검증 명령
- `GRADLE_USER_HOME=/tmp/.gradle ./gradlew --no-daemon :app:test --tests com.personal.happygallery.app.booking.BookingCancelUseCaseIT --tests com.personal.happygallery.app.pass.PassCreditUsageUseCaseIT --tests com.personal.happygallery.app.pass.PassPurchaseUseCaseIT`

---

## U12. 테스트 인프라/저장소 운영 규칙
### 범위
- `app/src/test/java/com/personal/happygallery/support/UseCaseIT.java`
- `AGENTS.md`
- `.gitignore`

### 확인 포인트
- 공통 테스트 설정이 테스트 의미를 바꾸지 않는지
- 운영 규칙 문서가 실제 워크플로우와 맞는지

### 완료 조건
- 규칙/인프라 변경을 기능 변경과 분리해 판단했다.

### 최소 검증 명령
- 필요 시 관련 테스트 최소셋만 선택 실행

---

## 6) 리뷰 응답 형식 고정
- `머지 전 수정 필요`
- `후속 이슈`
- `테스트/리스크`

## 7) 단위 선택 가이드
- 배치/환불 동작이 의심되면: `U2`, `U3`, `U6`
- 상태 전이/도메인 규칙이 의심되면: `U1`, `U4`
- 알림/8회권이 의심되면: `U8`, `U9`
- 테스트 충분성이 의심되면: `U10`, `U11`
- 규칙/환경 문서 점검은 마지막에: `U12`

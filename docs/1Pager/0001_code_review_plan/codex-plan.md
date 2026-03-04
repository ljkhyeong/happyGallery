# Code Review Plan

## 목적
- MVP 구현 이후 출시 전 리스크를 빠르게 식별한다.
- 스타일보다 동작 버그, 스펙 불일치, 데이터 정합성, 테스트 누락을 우선 검토한다.

## 리뷰 기준 문서
- 기준 스펙: `docs/PRD/0001_spec/spec.md`
- 설계 판단 참고: `docs/ADR/*`

## 리뷰 순서
1. 변경 범위 정리
- 포맷, 줄바꿈, 문서 전체 재기록처럼 기능과 무관한 diff를 먼저 분리한다.
- 기능 변경 PR은 리뷰 가능한 단위로 유지한다.
- 하나의 PR에 기능 변경, 테스트 인프라 정리, 문서/규칙 변경, 환경 설정 변경이 함께 섞이면 먼저 리뷰 묶음을 다시 나눈다.
- 리뷰 묶음은 "하나의 사용자/운영 시나리오" 또는 "하나의 기술적 의도" 기준으로 자른다.
- 아래 유형은 가능하면 별도 PR 또는 별도 리뷰 단위로 분리한다.
  - 핵심 도메인 기능 변경: 상태 전이, 정책, 배치 처리, DB 마이그레이션
  - API 계약 변경: 요청/응답 DTO, 에러 포맷, 직렬화 필드명
  - 테스트 인프라 변경: `@UseCaseIT`, `MockMvc`, Testcontainers 설정, 공통 픽스처
  - 문서/작업 규칙 변경: PRD, ADR, `AGENTS.md`, `HANDOFF.md`
  - 환경/도구 변경: `.gitignore`, Gradle 의존성, 로깅/스케줄링 공통 설정

### 현재 저장소 기준 권장 리뷰 단위
1. 주문 승인/자동환불/픽업 만료 정합성
- 대상: 낙관적 락, 재시도, 배치 건별 처리, 주문 승인 이력, version migration
- 대표 파일:
  - `app/src/main/java/com/personal/happygallery/app/order/*`
  - `domain/src/main/java/com/personal/happygallery/domain/order/*`
  - `app/src/main/resources/db/migration/V6__add_order_version_columns.sql`

2. 배치 공통화와 운영 응답 계약
- 대상: `BatchJob`, `BatchLoggingAspect`, `BatchResult`, 관리자 배치 DTO 응답
- 대표 파일:
  - `app/src/main/java/com/personal/happygallery/app/batch/*`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/*`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/dto/*`

3. 8회권 만료/알림 동작 수정
- 대상: 만료 시간 계산, 7일 전 알림, 중복 발송 방지, 관련 알림 로그 조회
- 대표 파일:
  - `app/src/main/java/com/personal/happygallery/app/pass/*`
  - `infra/src/main/java/com/personal/happygallery/infra/notification/*`

4. 테스트 보강
- 대상: 회귀 테스트 추가, DTO 응답 계약 검증, 경쟁 조건/실패 경로 보강
- 대표 파일:
  - `app/src/test/java/com/personal/happygallery/app/order/*`
  - `app/src/test/java/com/personal/happygallery/app/pass/*`
  - `app/src/test/java/com/personal/happygallery/app/booking/*`

5. 테스트 인프라/작업 규칙 정리
- 대상: `@UseCaseIT`, `MockMvc` 공통 설정, `AGENTS.md`, `.gitignore`
- 이 묶음은 기능 PR과 분리해서 마지막에 리뷰한다.

## 실제 리뷰 진행 단위

### 공통 원칙
- 한 번의 리뷰 지시는 1개 주제, 최대 3~6개 파일 범위로 제한한다.
- 한 번의 리뷰 결과는 `머지 전 수정 필요`, `후속 이슈`, `테스트/리스크` 3종으로만 받는다.
- 다음 지시로 넘어가기 전에 이전 리뷰에서 확인한 파일 범위와 남은 의문점을 짧게 정리한다.
- 큰 범위를 한 번에 "전체 리뷰"하지 않는다. 먼저 파일 범위와 관점을 고정한 뒤 세부 리뷰로 들어간다.

### 추천 지시 형식
- 범위 지정: 어떤 파일/기능만 볼지 명시한다.
- 관점 지정: 스펙, 정합성, 레이어, API, 테스트 중 무엇을 볼지 명시한다.
- 산출물 지정: "버그 중심", "리스크 중심", "테스트 누락 중심" 중 하나를 명시한다.

예시:
- "`PLAN.md` 기준 1-1 범위만 리뷰해줘. 주문 승인 상태 전이와 승인 이력 저장 로직에서 머지 전 수정이 필요한 버그만 찾아줘."
- "`PLAN.md` 기준 2-2 범위만 리뷰해줘. 관리자 배치 API 응답 DTO와 스펙 문서가 불일치하는 부분만 찾아줘."
- "`PLAN.md` 기준 4-1 범위만 리뷰해줘. 추가된 테스트가 실제 변경을 충분히 막는지 테스트 누락 중심으로 봐줘."

## 세부 리뷰 태스크

### 1. 주문 승인/자동환불/픽업 만료 정합성

#### 1-1. 주문 승인 상태 전이와 승인 이력
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/order/OrderApprovalService.java`
  - `domain/src/main/java/com/personal/happygallery/domain/order/Order.java`
  - `domain/src/main/java/com/personal/happygallery/domain/order/OrderApprovalHistory.java`
  - `domain/src/main/java/com/personal/happygallery/domain/order/OrderApprovalDecision.java`
  - `infra/src/main/java/com/personal/happygallery/infra/order/OrderApprovalHistoryRepository.java`
- 확인 포인트:
  - 승인/거절 상태 전이가 스펙과 맞는지
  - 승인 이력이 누락되거나 중복 저장될 수 없는지
  - 주문 상태와 이력의 원자성이 깨지지 않는지
- 지시 예시:
  - "`PLAN.md` 기준 1-1만 리뷰해줘. 주문 승인/거절 상태 전이와 승인 이력 저장에 머지 전 수정이 필요한 버그가 있는지 봐줘."

#### 1-2. 자동환불 배치 처리와 건별 트랜잭션
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/order/OrderAutoRefundBatchService.java`
  - `app/src/main/java/com/personal/happygallery/app/order/OrderAutoRefundProcessor.java`
- 확인 포인트:
  - 목록 조회 후 건별 처리에서 누락/중복 처리 가능성이 없는지
  - 실패 건이 전체 배치를 망치지 않는지
  - 성공/실패 집계가 실제 처리 결과와 어긋나지 않는지
- 지시 예시:
  - "`PLAN.md` 기준 1-2만 리뷰해줘. 주문 자동환불 배치가 중복 환불이나 누락 처리 위험이 없는지 정합성 중심으로 봐줘."

#### 1-3. 픽업 만료 배치 처리와 환불/재고 복구
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/order/PickupExpireBatchService.java`
  - `app/src/main/java/com/personal/happygallery/app/order/PickupExpireProcessor.java`
  - `app/src/main/java/com/personal/happygallery/app/order/OrderPickupService.java`
  - `domain/src/main/java/com/personal/happygallery/domain/order/Fulfillment.java`
- 확인 포인트:
  - 픽업 준비, 픽업 완료, 픽업 만료 상태 전이 충돌이 없는지
  - 재고 복구/환불/fulfillment 상태 동기화가 항상 같이 움직이는지
  - 수동 픽업 완료와 배치 만료가 동시에 일어날 때 안전한지
- 지시 예시:
  - "`PLAN.md` 기준 1-3만 리뷰해줘. 픽업 만료 배치와 수동 픽업 완료가 충돌할 때 데이터 정합성 문제가 없는지 찾아줘."

#### 1-4. 낙관적 락, 재시도, version migration
- 파일 범위:
  - `domain/src/main/java/com/personal/happygallery/domain/order/Order.java`
  - `domain/src/main/java/com/personal/happygallery/domain/order/Fulfillment.java`
  - `app/src/main/resources/db/migration/V6__add_order_version_columns.sql`
  - `app/src/main/java/com/personal/happygallery/config/RetryConfig.java`
- 확인 포인트:
  - `@Version` 적용 위치와 migration이 일치하는지
  - 재시도 정책이 과도하거나 부족하지 않은지
  - version 기본값/기존 데이터 migration이 안전한지
- 지시 예시:
  - "`PLAN.md` 기준 1-4만 리뷰해줘. version 컬럼 migration과 낙관적 락 설정에 운영 리스크가 있는지 봐줘."

### 2. 배치 공통화와 운영 응답 계약

#### 2-1. 배치 공통 로깅/AOP
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/batch/BatchJob.java`
  - `app/src/main/java/com/personal/happygallery/app/batch/BatchLoggingAspect.java`
  - `app/src/main/java/com/personal/happygallery/app/batch/BatchScheduler.java`
- 확인 포인트:
  - 로깅이 예외를 삼키지 않는지
  - 스케줄러 반환값/예외와 AOP 로그가 어긋나지 않는지
  - 공통화로 인해 개별 배치 의미가 가려지지 않는지
- 지시 예시:
  - "`PLAN.md` 기준 2-1만 리뷰해줘. 배치 AOP 공통화가 예외 처리나 로그 정확도를 깨지 않는지 봐줘."

#### 2-2. BatchResult와 관리자 배치 API 응답
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/batch/BatchResult.java`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/AdminOrderController.java`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/AdminPassController.java`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/dto/BatchResponse.java`
- 확인 포인트:
  - 내부 결과와 외부 응답 DTO 책임이 잘 분리됐는지
  - 필드명/직렬화/빈 맵 응답이 스펙과 충돌하지 않는지
  - 실패 사유 정보가 과소/과대 노출되는지
- 지시 예시:
  - "`PLAN.md` 기준 2-2만 리뷰해줘. BatchResult와 BatchResponse 분리가 적절한지 API 계약 중심으로 봐줘."

#### 2-3. 관리자 부수 응답 DTO 정리
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/web/admin/AdminBookingController.java`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/AdminRefundController.java`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/dto/BookingNoShowResponse.java`
  - `app/src/main/java/com/personal/happygallery/app/web/admin/dto/FailedRefundResponse.java`
- 확인 포인트:
  - 기존 `Map` 응답을 DTO로 바꾸며 계약 회귀가 없는지
  - nullable 필드 매핑이 안전한지
  - 운영자 화면이 의존할 필드가 빠지지 않았는지
- 지시 예시:
  - "`PLAN.md` 기준 2-3만 리뷰해줘. AdminBooking/AdminRefund 응답 DTO 전환에 회귀 위험이 없는지 찾아줘."

### 3. 8회권 만료/알림 동작 수정

#### 3-1. 8회권 만료 배치
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/pass/PassExpiryBatchService.java`
- 확인 포인트:
  - 만료 처리 시 크레딧 차감/ledger 기록이 정확한지
  - 이미 만료된 pass를 중복 처리하지 않는지
  - 성공 건수와 실제 갱신 건수가 일치하는지
- 지시 예시:
  - "`PLAN.md` 기준 3-1만 리뷰해줘. 8회권 만료 배치가 중복 만료나 ledger 오류를 만들 수 있는지 봐줘."

#### 3-2. 만료 7일 전 알림과 중복 발송 방지
- 파일 범위:
  - `app/src/main/java/com/personal/happygallery/app/pass/PassExpiryBatchService.java`
  - `infra/src/main/java/com/personal/happygallery/infra/notification/NotificationLogRepository.java`
- 확인 포인트:
  - 정확히 7일 전 조건이 스펙과 일치하는지
  - 하루 1회만 발송된다는 보장이 실제로 되는지
  - 실패 시 재시도/누락 리스크가 있는지
- 지시 예시:
  - "`PLAN.md` 기준 3-2만 리뷰해줘. 만료 알림이 하루 1회, 7일 전 조건을 정확히 지키는지 봐줘."

### 4. 테스트 보강

#### 4-1. 주문/배치 회귀 테스트
- 파일 범위:
  - `app/src/test/java/com/personal/happygallery/app/order/*`
- 확인 포인트:
  - 승인 이력, 자동환불, 픽업 만료의 핵심 회귀가 잡히는지
  - 실패 경로와 동시성 경로가 충분한지
- 지시 예시:
  - "`PLAN.md` 기준 4-1만 리뷰해줘. 주문/배치 관련 테스트가 실제 회귀를 막기에 충분한지 테스트 누락 중심으로 봐줘."

#### 4-2. 패스/알림/관리자 응답 테스트
- 파일 범위:
  - `app/src/test/java/com/personal/happygallery/app/pass/*`
  - `app/src/test/java/com/personal/happygallery/app/booking/BookingCancelUseCaseIT.java`
- 확인 포인트:
  - 응답 DTO 계약 검증이 핵심 필드를 다 막는지
  - 만료/알림 관련 경계값 테스트가 충분한지
- 지시 예시:
  - "`PLAN.md` 기준 4-2만 리뷰해줘. 패스 만료/알림과 관리자 응답 DTO 테스트가 충분한지 봐줘."

### 5. 테스트 인프라/작업 규칙 정리

#### 5-1. UseCaseIT와 MockMvc 공통 설정
- 파일 범위:
  - `app/src/test/java/com/personal/happygallery/support/UseCaseIT.java`
  - 관련 `*UseCaseIT`
- 확인 포인트:
  - 공통 설정 변경이 기존 테스트 의미를 바꾸지 않는지
  - 필터 적용 여부가 테스트 의도와 맞는지
- 지시 예시:
  - "`PLAN.md` 기준 5-1만 리뷰해줘. UseCaseIT의 MockMvc 공통화가 테스트 의미를 바꾸지 않는지 봐줘."

#### 5-2. 작업 규칙/환경 파일 정리
- 파일 범위:
  - `AGENTS.md`
  - `.gitignore`
- 확인 포인트:
  - 기능 변경과 분리해서 볼 가치가 있는지
  - 저장소 운영 규칙으로 적절한지
- 지시 예시:
  - "`PLAN.md` 기준 5-2만 리뷰해줘. AGENTS.md와 gitignore 변경이 저장소 운영 규칙으로 적절한지만 봐줘."

2. 스펙 일치성 검토
- 상태값, 시간 경계, 예약금 10%, 슬롯 정원 8명, 버퍼 30분, 8회권 90일 만료 규칙이 스펙과 일치하는지 확인한다.
- 구현 변경이 스펙 변경을 동반하면 문서도 함께 갱신했는지 확인한다.

3. 레이어 책임 검토
- `app`은 흐름 제어에 집중하고, 도메인 규칙은 `domain`에, 외부 연동 구현은 `infra`에 유지되는지 확인한다.
- `app`에 정책이 과도하게 들어가거나 `infra`에 업무 규칙이 들어가지 않았는지 본다.

4. 핵심 도메인 리뷰
- 예약/슬롯: 예약 생성, 변경, 취소, 정원 초과, 버퍼 비활성화, 중복 예약 방지
- 주문/환불: 승인 대기, 자동 환불, 재고 복구, 픽업 만료, 제작 시작 후 환불 불가
- 8회권: 결제, 차감, 만료, 환불, 미래 예약 취소

5. 정합성과 동시성 검토
- 재고와 슬롯은 트랜잭션 안에서 일관되게 갱신되는지 확인한다.
- 수동 처리와 배치가 동시에 실행될 때 중복 환불, 중복 차감, 중복 상태 전이가 없는지 본다.

6. API/예외/알림 검토
- 요청/응답 DTO와 에러 포맷이 일관적인지 확인한다.
- 외부 연동 실패 시 상태 기록과 재시도 경로가 남는지 확인한다.

7. 테스트 검토
- 정책 변경은 `policyTest`, 유스케이스/DB 영향은 `useCaseTest`가 따라오는지 확인한다.
- 경계값, 실패 경로, 경쟁 조건 테스트가 있는지 검토한다.

## 우선 리뷰 대상
- `app/src/main/java/com/personal/happygallery/app/booking/*`
- `app/src/main/java/com/personal/happygallery/app/order/*`
- `app/src/main/java/com/personal/happygallery/app/pass/*`
- `domain/src/main/java/com/personal/happygallery/domain/booking/*`
- `domain/src/main/java/com/personal/happygallery/domain/order/*`
- `domain/src/main/java/com/personal/happygallery/domain/pass/*`
- `common/src/main/java/com/personal/happygallery/common/time/TimeBoundary.java`
- `app/src/main/resources/db/migration/*`
- 관련 `*UseCaseIT`, `*PolicyTest`

## 리뷰 산출물
- 머지 전 반드시 수정할 이슈
- 후속 이슈로 넘길 개선 항목
- 실행한 테스트와 남은 리스크

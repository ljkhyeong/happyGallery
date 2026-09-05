---
name: happygallery-batch-flows
description: happyGallery의 공통 scheduler·cron·BatchExecutor·BatchResult·수동 배치 실행을 변경할 때 사용한다. 예약·주문·이용권 정책 변경은 해당 도메인 스킬을 사용한다.
---

# happyGallery 배치

## 규칙

- `application/src/main/java/com/personal/happygallery/application/batch/`와 `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/SchedulingConfig.java`를 확인한다.
- `BatchExecutor`는 후보 목록을 항목별로 독립 처리하고 실패를 집계하는 흐름에 사용한다. 후보 조회·재시도·멱등 처리·결과 집계를 명확히 구분한다.
- 전체 배치를 하나의 트랜잭션으로 묶지 않는다. 항목별 짧은 트랜잭션을 쓰고 외부 호출은 트랜잭션 밖에서 실행한다.
- 대량 정리·보관 만료 처리는 제한된 크기로 조회·커밋하고 후보가 없어질 때까지 반복한다.
- cron과 `Asia/Seoul`, `BatchResult(successCount, failureCount, failureReasons)` 계약을 유지한다. 변경 시 PRD와 해당 도메인 ADR을 갱신한다.
- `NotificationOutboxScheduler`는 별도 polling 흐름이다. 수동 관리자 실행은 정기 실행과 같은 도메인 동작을 호출한다.

## 검증

- scheduler 변경은 배치 등록·cron·zone을 확인하고, 도메인 처리는 영향받는 `*Batch*UseCaseIT`를 선택한다.
- 변경한 일정·중복 처리·항목 실패 집계만 확인한다. 도메인 전체 테스트는 추가 영향이 있을 때 실행한다.

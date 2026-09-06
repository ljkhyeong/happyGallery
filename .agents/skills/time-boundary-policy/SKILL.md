---
name: time-boundary-policy
description: happyGallery의 취소·변경 마감, 이용권 만료, 배치 시간대, Clock 주입과 업무 코드의 now 호출을 변경할 때 사용한다.
---

# happyGallery 시간 기준

## 규칙

- 정책은 PRD-0001과 해당 도메인 ADR을 확인한다. 예약 환불은 전날 00:00, 당일 변경은 시작 1시간 전, 이용권 만료는 구매일 +90일, 만료 알림은 7일 전이 기준이다.
- 업무 시각은 주입한 `Clock`으로 얻는다. 업무 코드에서 인자 없는 `now()`를 호출하지 않는다.
- 공통 계산은 `domain/src/main/java/com/personal/happygallery/domain/time/TimeBoundary.java`, Clock 설정은 `bootstrap/src/main/java/com/personal/happygallery/bootstrap/config/ClockConfig.java`를 확인한다.
- 날짜 기준 정책과 cron에는 `Asia/Seoul`을 명시한다. 시스템 기본 시간대에 의존하지 않는다.
- 업무 `LocalDateTime`은 서울 시각, DB 기본 audit 시각은 UTC일 수 있다. 서로 비교할 때 같은 `Instant`에서 각 컬럼 시간대에 맞는 기준 시각을 계산한다.
- 결제·환불 재시도 시간은 저장된 재시도 상태와 scheduler 설정에서 관리한다. 예약 `TimeBoundary`에 합치지 않는다.

## 검증

- 마감 계산이 바뀌면 고정 Clock으로 직전·정각·직후를 확인하는 해당 policy test를 실행한다.
- DB 처리 시점은 해당 use case로 확인하고, scheduler 변경은 배치 등록·cron·zone을 확인한다. 정책이 바뀌면 PRD와 해당 ADR도 갱신한다.

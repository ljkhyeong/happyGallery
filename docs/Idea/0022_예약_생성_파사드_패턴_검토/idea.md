# 0022 — 예약 생성 Facade 패턴 도입 검토

## 배경

`DefaultMemberBookingService.createMemberBooking()`에서 비관적 락(`SELECT FOR UPDATE`)이
트랜잭션 종료 시까지 유지되는 구조에 대해 검토했다.

## 분석 결과

락 보유 구간 내 작업이 모두 DB 쓰기이며, 외부 I/O(알림 발송)는
`@Async("notificationExecutor")`로 트랜잭션 밖에서 비동기 실행된다.
따라서 **현재 구조에서 락 보유 시간은 이미 충분히 짧다.**

## Facade 도입 시나리오

만약 향후 예약 생성 트랜잭션 안에 외부 API 호출(PG 결제 등)이 추가된다면,
Facade로 분리하여 락 보유 구간을 최소화할 수 있다:

```
BookingFacade (트랜잭션 없음)
  ├─ 검증 단계: 슬롯 활성 여부, 중복 예약 체크
  ├─ 외부 호출 단계: PG 결제 등
  └─ 확정 단계 (@Transactional): 비관적 락 + 정원 확보 + 예약 저장
```

## 결론

현시점에서는 도입 불필요. 외부 I/O가 트랜잭션에 진입하는 시점에 재검토한다.

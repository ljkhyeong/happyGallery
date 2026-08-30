# ADR-0003: 슬롯 정원 동시성 전략 — 비관적 락(SELECT FOR UPDATE) 선택

- **상태**: 확정
- **날짜**: 2026-02-22
- **관련 파일**:
  - `adapter-out-persistence/src/main/java/com/personal/happygallery/adapter/out/persistence/booking/JpaSlotLockAdapter.java` — `lockAllById()`
  - `application/src/main/java/com/personal/happygallery/application/booking/SlotCapacitySupport.java` — `reserveCapacity()`
  - `application/src/main/java/com/personal/happygallery/application/booking/BookingVacancyAlertPublisher.java` — 빈자리 전환 알림 발행
  - `domain/booking/Slot.java` — `incrementBookedCount()`
  - `domain/booking/SlotCapacity.java` — `checkAvailable(int)`

---

## 배경

`slots.booked_count` 증가는 동시성 핵심 구간이다(spec.md §8.2).
ADR-0001에서 낙관적 락용 `bookings.version` 컬럼을 스키마에 확보했지만,
슬롯 정원 강제에 **낙관적 락 vs 비관적 락** 중 어느 쪽을 쓸지는 "추후 확정"으로 남겨뒀었다.

결정 배경:
- 클래스별 회차 정원이 다르더라도 예약 피크 시간에는 동일 슬롯 경쟁이 발생한다.
- "성공 or 즉시 실패(정원 초과)"로 단순하게 처리해야 UX와 운영이 예측 가능하다.

---

## 결정

클래스 등록 시 정하고 슬롯 생성 시 복사한 `capacity` 강제에는 **비관적 쓰기 락(SELECT FOR UPDATE)** 을 사용한다.

### 구현 흐름 (`SlotCapacitySupport.reserveCapacity()`을 포함하는 단일 트랜잭션)

```
1. 원인 슬롯의 scheduling projection에서 classId와 `[startAt, endAt + bufferMin)` 충돌 범위를 계산

2. classes 행을 SELECT FOR UPDATE로 잠금
   → 같은 클래스의 자동 회차 구체화·예약·반납을 직렬화

3. JpaSlotLockAdapter.lockScope(classId, sourceSlotId, windowStart, windowEnd)
   → native scalar ID 조회로 원인 슬롯과 양방향 충돌 범위를 PK 오름차순 SELECT FOR UPDATE
   → MySQL REPEATABLE READ의 이전 스냅샷이 아니라 잠금 현재 읽기로 직전에 생성된 슬롯까지 포함
   → 이미 관리 중인 후보 Slot만 detach하고, 잠근 ID를 같은 트랜잭션에서 한 번에 다시 적재해 최신 상태 사용

4. 주입된 Clock 기준으로 슬롯 시작 전이고 실제 활성 상태인지 재확인
   → 조회 이후 시간이 지났거나 관리자/버퍼 상태가 바뀌었으면 SlotNotAvailableException

5. 잠긴 충돌 슬롯의 예약 점유 확인
   → booked_count > 0인 슬롯이 하나라도 있으면 수업·정리 구간 충돌이므로 SlotNotAvailableException

6. Slot.incrementBookedCount(participantCount)
   → SlotCapacity.checkAvailable(capacity, bookedCount, participantCount)
   → bookedCount += participantCount
   → 현재 점유와 요청 인원의 합이 해당 슬롯 정원을 넘으면 CapacityExceededException

7. slotStorePort.save(slot)                     // booked_count 커밋

8. booked_count가 0 → 양수이면 이미 잠근 양방향 충돌 슬롯의 buffer_block_count++
```

`booked_count`는 예약 건수가 아니라 예약 인원 점유다. 일반 예약은 한 건에 슬롯의 남은 정원까지 점유하고,
8회권 예약은 1명만 점유한다. 예약 취소·변경의 `releaseCapacity()`는 예약의 `participant_count`만큼
반납하며, `booked_count`가 양수 → 0이 될 때 같은 충돌 범위를 잠그고 `buffer_block_count--`를
수행한다. 버퍼가 겹치는 슬롯은 차단 수가 0이 된 뒤에만 실제 활성 상태가 된다.

범위 슬롯만 잠그면 버퍼 ID 조회와 관리자 슬롯 INSERT 사이에 phantom이 생길 수 있다. 반대로 원인 슬롯부터
잠그고 클래스 행을 나중에 잠그면 서로 다른 원인 슬롯 예약이 교차할 때 교착될 수 있다. 따라서 모든
같은 클래스 작업은 `classes → slots(PK 오름차순)` 순서로 잠근다. 클래스 행을 먼저 잡았으므로 뒤의 범위
`FOR UPDATE` 사이에는 같은 클래스 INSERT가 들어오지 않고, 앞뒤 슬롯 동시 예약도 하나씩 직렬화된다.

8회권 크레딧까지 함께 변경하는 예약 생성·취소·전체 환불은 교차 애그리거트 잠금 순서를
`pass_purchases → classes → slots(PK 오름차순)`으로 고정한다. 8회권 행을 먼저 잠가 크레딧 차감·복구·소멸을
직렬화한 뒤 기존 클래스·슬롯 잠금 규칙을 따른다. 예약금 예약과 예약 변경처럼 8회권 잔액을 바꾸지 않는
흐름은 기존 `classes → slots(PK 오름차순)` 순서를 유지하며, 클래스·슬롯을 잡은 뒤 8회권 행을 추가로
잠그는 역순 경로는 두지 않는다.

예약 생성·변경의 빠른 사전 확인과 클래스 ID 수집은 Slot 엔티티 대신 필요한 scheduling projection만 읽어
1차 캐시에 오래된 Slot을 추가하지 않는다. 일반 조회가 먼저 MySQL `REPEATABLE READ` 스냅샷을 열어도 뒤의
native `FOR UPDATE`는 현재 읽기로 실행되므로 클래스 락 대기 중 생성된 버퍼 슬롯까지 찾는다. 예약에서 이미
참조해 관리 중인 Slot이 있으면 잠근 scalar ID만 `getReference`로 식별해 detach한 뒤, 잠근 ID 전체를 한 번에
다시 적재한다. 전체 영속성 컨텍스트를 비우지 않아 함께 처리 중인 다른 애그리거트는 유지된다.

규칙 적용 전에 이미 충돌한 예약 데이터는 자동 취소하지 않는다. 그러나 이후 예약 확정은 매번 잠긴
양방향 충돌 범위의 기존 예약을 확인하므로, 어느 쪽 슬롯이 먼저 예약됐는지와 관계없이 충돌을 새로 만들거나
확대하는 예약은 거절한다.

### 만석 회차 빈자리 알림

빈자리 알림은 좌석을 선점하거나 예약 순서를 보장하는 대기열이 아니다. 고객이 만석인 미래 활성 회차에
알림을 신청하면 `booking_vacancy_alerts`에 대기 상태를 기록하고, 다음 조건을 모두 만족할 때 한 번만 알린다.

- 예약 취소·부분 취소로 잠긴 슬롯의 점유가 `만석 → 예약 가능`으로 바뀐 경우
- 관리자 재활성화로 슬롯이 실제 예약 가능한 상태가 된 경우

좌석 반납은 기존 `classes → slots(PK 오름차순)` 잠금 순서 안에서 처리한다. 전환을 확인한 뒤 해당 슬롯의
대기 알림을 잠그고 `NOTIFIED`로 바꾸며, 같은 트랜잭션에 `BOOKING_VACANCY_AVAILABLE` outbox를 기록한다.
따라서 점유 수가 커밋되지 않았는데 알림만 나가거나, 같은 전환을 여러 번 알리는 상태를 만들지 않는다.
알림을 받은 뒤 다른 고객이 먼저 예약할 수 있으므로 메시지에는 좌석 보장 문구를 넣지 않는다.

예약 시작까지 처리되지 않은 `WAITING` 신청은 더 이상 의미가 없으므로 개인정보 보존 배치에서 삭제한다.
알림 완료·고객 취소 상태는 운영 확인 기간 30일이 지난 뒤 삭제한다.

### 역할 분리

| 락 전략 | 대상 | 이유 |
|---------|------|------|
| **비관적 락 (SELECT FOR UPDATE)** | 클래스 행 | 같은 클래스의 자동 회차 구체화·예약·반납 순서를 직렬화해 phantom 방지 |
| **비관적 락 (SELECT FOR UPDATE)** | 원인 슬롯과 양방향 충돌 슬롯 | 정원과 수업·정리 시간 충돌을 같은 경계에서 직렬화 |
| **비관적 락 (SELECT FOR UPDATE)** | 빈자리 알림 대기 행 | 같은 빈자리 전환의 중복 알림 방지 |
| **비관적 락 (SELECT FOR UPDATE)** | 크레딧을 변경하는 8회권 행 | 서로 다른 클래스의 동시 예약과 예약·환불·만료 경합을 직렬화 |
| **낙관적 락 (`@Version`)** | `bookings` 예약 행 | 동시 변경 드물고 재시도 허용 가능 (§5.3 구현 시) |

---

## Alternatives

| 대안 | 기각 이유 |
|------|-----------|
| 낙관적 락(`@Version` on slots) | 충돌 빈번 시 `OptimisticLockException` → 재시도 → 또 충돌. 서비스 레이어에 재시도 루프 필요. |
| DB COUNT 쿼리 + 제약 | `FOR UPDATE` 없이 COUNT 후 INSERT 시 TOCTOU(Time-of-Check-Time-of-Use) 경쟁 조건 그대로 잔존. |
| 분산 락(Redis) | DB 행이 정원과 버퍼 상태의 원본이므로 별도 잠금 저장소를 두면 장애 지점과 정합성 경계만 늘어난다. |

---

## 결과

**긍정**
- 클래스 행을 먼저 잠그고 원인 슬롯과 버퍼 슬롯을 PK 순서로 잠가 정원, 역방향 충돌, 슬롯 삽입 경쟁을 함께 방어한다.
- 재시도 로직 불필요 → 서비스 레이어 코드 간결.
- `CapacityExceededException` 발생 시 자동 롤백 → `booked_count` 불변 보장.
- 첫 예약과 마지막 예약 전환에서만 버퍼 차단 수를 변경해 같은 슬롯의 여러 예약을 하나의 원인으로 취급한다.
- 공개 조회와 잠금 후 재검증을 함께 적용해 이미 시작한 슬롯이 조회-예약 사이 경쟁 조건으로 확정되지 않는다.
- 뒤 슬롯 선예약 후 앞 슬롯 예약을 시도하는 역방향 순서에서도 버퍼 충돌을 허용하지 않는다.

**부정 / 주의 사항**
- `reserveCapacity()`는 반드시 **예약 엔티티(bookings) save와 동일 트랜잭션** 안에서 호출해야 한다.
  - 이유: `booked_count` 증가와 `booking` 생성이 다른 트랜잭션이면, 정원 초과 롤백 시 `booking` row가 고아로 남는다.
  - 구현 계약: 예약 서비스의 `@Transactional` 메서드 안에서 `reserveCapacity()` 호출 → 예약 save 순서를 지킨다.
- 단일 인스턴스 MySQL을 전제. 샤딩 환경에서는 재검토 필요(현재 MVP 범위 밖).

---

## References

- `docs/PRD/0001_기준_스펙/spec.md` §4.1 (클래스별 슬롯 정원), §8.2 (동시성 전략 방향)
- ADR-0001 (핵심 스키마 — `bookings.version` 낙관적 락 컬럼)
- ADR-0002 (상태 전이 가드 — `SlotCapacity.checkAvailable()`)

# ADR-0003: 슬롯 정원 동시성 전략 — 비관적 락(SELECT FOR UPDATE) 선택

- **상태**: 확정
- **날짜**: 2026-02-22
- **관련 파일**:
  - `infra/booking/SlotRepository.java` — `findByIdWithLock()`
  - `app/booking/SlotManagementService.java` — `confirmBooking()`
  - `domain/booking/Slot.java` — `incrementBookedCount()`
  - `domain/booking/SlotCapacity.java` — `checkAvailable(int)`

---

## Context

`slots.booked_count` 증가는 동시성 핵심 구간이다(spec.md §8.2).
ADR-0001에서 낙관적 락용 `bookings.version` 컬럼을 스키마에 확보했지만,
슬롯 정원 강제에 **낙관적 락 vs 비관적 락** 중 어느 쪽을 쓸지는 "추후 확정"으로 남겨뒀었다.

결정 배경:
- 슬롯 1개에 최대 8명 → 예약 피크 시간에 동일 슬롯 경쟁이 빈번하게 발생한다.
- "성공 or 즉시 실패(정원 초과)"로 단순하게 처리해야 UX와 운영이 예측 가능하다.

---

## Decision

슬롯 정원(capacity=8) 강제에는 **비관적 쓰기 락(SELECT FOR UPDATE)** 을 사용한다.

### 구현 흐름 (`SlotManagementService.confirmBooking()` 단일 트랜잭션)

```
1. SlotRepository.findByIdWithLock(slotId)
   → SELECT … FROM slots WHERE id = ? FOR UPDATE  (row 잠금)

2. Slot.incrementBookedCount()
   → SlotCapacity.checkAvailable(bookedCount)   // bookedCount >= 8 → CapacityExceededException
   → bookedCount++

3. slotRepository.save(slot)                    // booked_count 커밋

4. 버퍼 윈도우 [endAt, endAt + bufferMin) 내 활성 슬롯 deactivate
```

### 역할 분리

| 락 전략 | 대상 | 이유 |
|---------|------|------|
| **비관적 락 (SELECT FOR UPDATE)** | `slots.booked_count` | 정원 경쟁 빈번, 재시도 없이 직렬화 |
| **낙관적 락 (`@Version`)** | `bookings` 예약 행 | 동시 변경 드물고 재시도 허용 가능 (§5.3 구현 시) |

---

## Alternatives

| 대안 | 기각 이유 |
|------|-----------|
| 낙관적 락(`@Version` on slots) | 충돌 빈번 시 `OptimisticLockException` → 재시도 → 또 충돌. 서비스 레이어에 재시도 루프 필요. |
| DB COUNT 쿼리 + 제약 | `FOR UPDATE` 없이 COUNT 후 INSERT 시 TOCTOU(Time-of-Check-Time-of-Use) 경쟁 조건 그대로 잔존. |
| 분산 락(Redis) | 현재 인프라에 Redis 없음. MVP 단계 과도한 복잡도. |

---

## Consequences

**긍정**
- 정원 강제 로직이 단일 트랜잭션 + 단일 row 잠금으로 단순화.
- 재시도 로직 불필요 → 서비스 레이어 코드 간결.
- `CapacityExceededException` 발생 시 자동 롤백 → `booked_count` 불변 보장.

**부정 / 주의 사항**
- `confirmBooking()`은 반드시 **예약 엔티티(bookings) save와 동일 트랜잭션** 안에서 호출해야 한다.
  - 이유: `booked_count` 증가와 `booking` 생성이 다른 트랜잭션이면, 정원 초과 롤백 시 `booking` row가 고아로 남는다.
  - 구현 계약: §5.2 `BookingService`의 `@Transactional` 메서드 안에서 `confirmBooking()` 호출 → `bookingRepository.save()` 순서를 지킨다.
- 단일 인스턴스 MySQL을 전제. 샤딩 환경에서는 재검토 필요(현재 MVP 범위 밖).

---

## References

- `docs/PRD/0001_spec/spec.md` §4.1 (슬롯 정원 8명), §8.2 (동시성 전략 방향)
- ADR-0001 (핵심 스키마 — `bookings.version` 낙관적 락 컬럼)
- ADR-0002 (상태 전이 가드 — `SlotCapacity.checkAvailable()`)

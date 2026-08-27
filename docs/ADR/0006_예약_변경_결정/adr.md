# ADR-0006: §5.3 예약 변경 + 이력 구현 결정

- **날짜**: 2026-02-22
- **상태**: 확정

---

## 배경

예약 변경(reschedule) 기능을 구현하면서 다음 설계 결정이 필요했다.

---

## 결정 1 — 현재 예약 in-place 업데이트

**선택**: `bookings.slot_id`를 새 슬롯으로 UPDATE. 행(row)은 항상 1건 유지.

**이유**:
- DoD: "현재 예약은 1건으로 유지"
- 예약 ID가 변경되지 않으므로 access_token 재발급 불필요
- 이력은 `booking_history`에 별도 append

**트레이드오프**: 예약 상태 스냅샷이 남지 않음 → `booking_history`로 보완.

---

## 결정 2 — 이력 append-only (booking_history)

**선택**: 변경마다 `BookingHistoryAction.RESCHEDULED` 행 INSERT.

**이유**: 감사 로그(ADR-0001 §12 비기능 요구사항) 충족. 이력 삭제/수정 불가.

**초기 예약 이력**: `GuestBookingService.createGuestBooking()`에도 `BOOKED` 이력 추가. §5.2 미완 항목 해소.

---

## 결정 3 — 낙관적 락 (`@Version`) 으로 동시 변경 방어

**선택**: `Booking.version` 컬럼 + Hibernate `@Version`.

**이유**:
- 예약 변경은 동시 빈도가 낮음 → 낙관적 락이 적합
- `ObjectOptimisticLockingFailureException` → `GlobalExceptionHandler` → 409 `BOOKING_CONFLICT`

**트레이드오프**: 충돌 시 클라이언트가 재시도해야 함. 빈도가 높아지면 비관적 락 전환 검토.

---

## 결정 4 — 마지막 예약 반납 시 버퍼 슬롯 자동 재활성화

**선택**: 기존 슬롯의 인원 점유 `booked_count`가 양수에서 0이 되면 그 슬롯과 수업·정리 구간이 겹치는 회차의 차단을 자동 해제한다.

**이유**:
- 예약이 사라졌는데 운영자가 별도로 슬롯을 복구해야 하는 흐름은 불필요한 운영 부담이다.
- `buffer_block_count`로 원인 수를 보존하면 여러 슬롯의 버퍼가 겹쳐도 아직 남은 차단을 지울 필요가 없다.
- 관리자 비활성 상태는 `admin_active`로 분리해 자동 버퍼 해제와 무관하게 유지할 수 있다.

**결과**:
- 같은 슬롯의 첫 인원 점유(`0 → 양수`)만 버퍼 차단 수를 증가시키고 마지막 인원 반납(`양수 → 0`)만 감소시킨다.
- 실제 활성 상태는 `admin_active && buffer_block_count == 0`이다.
- 예약이 잡힌 뒤 버퍼 범위에 생성한 슬롯은 생성 시 기존 원인 예약 수만큼 차단 수를 초기화한다.

---

## 결정 5 — 동일 클래스 안에서만 슬롯 변경

**선택**: 새 슬롯이 현재 예약과 같은 클래스인지 aggregate가 검증한다. 변경 트랜잭션은 관련 클래스 행을
잠근 뒤 예약의 `participantCount`를 그대로 사용해
`SlotCapacitySupport.reserveCapacity(newSlotId, participantCount)` →
`releaseCapacity(oldSlotId, participantCount)`를 실행한다.

**이유**:
- 예약금과 잔금은 예약 생성 당시 클래스 가격의 스냅샷이므로, 다른 클래스 슬롯으로 바꾸면 재결제 없이 가격 계약이 달라질 수 있다.
- 고객 화면은 예약 상세의 `classId`로 같은 클래스의 공개 예약 가능 슬롯만 조회한다.
- 새 슬롯 정원을 먼저 확보한 뒤 기존 슬롯을 반납하는 업무 순서를 유지한다.

**잠금 순서**: 8회권 예약 변경은 `pass_purchases → class → slots(PK ASC)`, 일반 예약 변경은
`class → slots(PK ASC)`이다. 여러 클래스의 미래 예약을 취소하는 8회권 전체 환불은 기존처럼 클래스 행을
PK 오름차순으로 잠근다.

---

## 결정 6 — 시간 경계 판정: `TimeBoundary.isChangeable(currentSlotStart, clock)`

**선택**: 변경 대상인 현재 슬롯의 시작 시각을 기준으로 판정.

**이유**: 현재 슬롯이 1시간 이내 시작 예정이면 변경 불가. `TimeBoundary` 기존 구현 재사용.

**서울 타임존 변환**: `booking.getSlot().getStartAt().atZone(Clocks.SEOUL)`

---

## 새 에러 코드

| 코드 | HTTP | 발생 상황 |
|------|------|-----------|
| `BOOKING_CONFLICT` | 409 | 동시 변경으로 `@Version` 불일치 |

## 새 도메인 메서드

| 메서드 | 위치 | 설명 |
|--------|------|------|
| `Slot.decrementBookedCount(participantCount)` | `Slot.java` | 반납 시 booked_count에서 예약 인원 차감 (비관적 락 후 호출) |
| `Booking.reschedule(Slot)` | `Booking.java` | slot_id 변경, status BOOKED 유지 |

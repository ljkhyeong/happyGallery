---
name: time-boundary-policy
description: >
  Workflow for time boundary calculations, cutoff policy enforcement, and Clock injection in the
  happyGallery backend. Always use this skill when the request involves: 취소 마감 시간 계산,
  환불 가능 여부 판단, 예약 변경 가능 시간 확인, "D-1 00:00 이전에만 환불 가능", "슬롯 시작 1시간 전까지만 변경 가능",
  TimeBoundary.java 수정, Clock 주입 패턴 변경, LocalDateTime.now() 직접 호출 제거, 8회권 90일 만료 계산,
  만료 7일 전 리마인더 타이밍, 시간 기반 정책 테스트 (Clock.fixed), Asia/Seoul 타임존 기준 시각 계산,
  또는 cron zone 설정. Also use this skill when someone asks "왜 취소가 안 돼요?" or "환불 기한이 언제예요"
  if the answer requires examining cutoff calculation logic. Also use this skill when someone says
  "시간이 지났는데도 취소가 됐어요" / "테스트에서 시간을 고정하고 싶어요" / "타임존 설정이 잘못된 것 같아요" /
  "Clock 빈을 어떻게 주입해요?".
---

# happyGallery Time Boundary Policy

## Core references

- Use `docs/PRD/0001_기준_스펙/spec.md` for all time boundary rules (refund cutoff, change cutoff, cron timing).
- Read the needed ADRs:
  - `docs/ADR/0003_슬롯_동시성_전략/adr.md`
  - `docs/ADR/0006_예약_변경_결정/adr.md`
  - `docs/ADR/0007_예약_취소_결정/adr.md`

## Canonical time rules (do not deviate without spec change)

| 규칙 | 기준 |
|------|------|
| 예약 환불 마감 | 슬롯 전날 (D-1) 00:00 Asia/Seoul |
| 당일 예약 변경 마감 | 슬롯 시작 1시간 전 |
| 배치 크론 timezone | `zone = "Asia/Seoul"` 고정 |
| 8회권 만료 | 구매일 기준 90일 |
| 8회권 만료 리마인더 | 만료 7일 전 |

## Cross-cutting rules

`happygallery-spring-backend`의 Respect module boundaries·Repository constraints·Test writing rules, `api-contract`의 Non-negotiable invariants, `happygallery-test-refactor`의 Assertion conventions를 이 스킬에서도 항상 따른다.

## Non-negotiable invariants

- **Never call `LocalDateTime.now()` or `ZonedDateTime.now()` directly in business logic.** Always use the injected `Clock` bean so tests can control time.
- All cutoff comparisons must use `Asia/Seoul` (`ZoneId.of("Asia/Seoul")`); never use UTC or system-default zone.
- `TimeBoundary.java` in `common/` is the canonical entry point for cutoff calculations — reuse it, do not duplicate logic.
- When adding a new time-sensitive rule, add the boundary calculation to `TimeBoundary` and cover it in `TimeBoundaryPolicyTest`.

## Clock injection pattern

```java
// Bean 등록 (production)
@Bean
public Clock clock() {
    return Clock.systemDefaultZone(); // Asia/Seoul via JVM default
}

// 서비스에서 주입
@RequiredArgsConstructor
public class BookingService {
    private final Clock clock;

    public void cancel(Booking booking) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        timeBoundary.requireBeforeRefundCutoff(booking.getSlot(), now);
        ...
    }
}

// 테스트에서 시간 제어
Clock fixedClock = Clock.fixed(Instant.parse("2024-06-01T10:00:00Z"), ZoneId.of("Asia/Seoul"));
```

## Likely code locations

- `common/src/main/java/com/personal/happygallery/common/time/TimeBoundary.java` — Cutoff calculation utilities
- `domain/src/main/java/com/personal/happygallery/domain/booking/` — Booking time policies
- `app/src/main/java/com/personal/happygallery/app/booking/` — Booking service time boundary calls
- `app/src/main/java/com/personal/happygallery/app/pass/` — Pass expiry and reminder timing
- `app/src/main/java/com/personal/happygallery/app/batch/` — Cron job timezone configuration

## High-value tests (for reference)

- `app/src/test/java/com/personal/happygallery/policy/TimeBoundaryPolicyTest.java`
- `app/src/test/java/com/personal/happygallery/app/booking/BookingCancelUseCaseIT.java`
- `app/src/test/java/com/personal/happygallery/app/booking/BookingRescheduleUseCaseIT.java`

## Verification workflow

- Pure time boundary rule changes: `./gradlew :app:policyTest`
- Time boundary affecting booking/pass use cases: `./gradlew --no-daemon :app:useCaseTest --tests "*Booking*" --tests "*Pass*"`

## Doc sync checklist

- Cutoff rules and timezone: `docs/PRD/0001_기준_스펙/spec.md`
- Reschedule/cancel cutoff decisions: `docs/ADR/0006_예약_변경_결정/adr.md`, `docs/ADR/0007_예약_취소_결정/adr.md`
- Session status: `HANDOFF.md`

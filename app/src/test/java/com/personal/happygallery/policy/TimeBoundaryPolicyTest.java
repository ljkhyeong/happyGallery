package com.personal.happygallery.policy;

import com.personal.happygallery.common.time.Clocks;
import com.personal.happygallery.common.time.TimeBoundary;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [PolicyTest] 시간 경계 정책 검증.
 *
 * <p>Spring 컨텍스트 불필요. Clock.fixed()로 경계값을 고정하여 {@link TimeBoundary} 메서드를 검증한다.
 * 각 규칙마다 경계 직전(허용) / 경계 당시(불허) 쌍으로 구성.
 *
 * <p>네이밍 규칙: 클래스명 suffix = PolicyTest, @Tag("policy")
 */
@Tag("policy")
class TimeBoundaryPolicyTest {

    // ── D-1 환불 경계 ─────────────────────────────────────────────────────────

    @Test
    void refund_oneMinuteBefore_allowed() {
        LocalDate experienceDate = LocalDate.of(2026, 3, 1);
        ZonedDateTime deadline = experienceDate.atStartOfDay(Clocks.SEOUL); // 2026-03-01T00:00+09:00

        Clock clock = Clock.fixed(deadline.minusMinutes(1).toInstant(), Clocks.SEOUL);

        assertThat(TimeBoundary.isRefundable(experienceDate, clock)).isTrue();
    }

    @Test
    void refund_atMidnight_notAllowed() {
        LocalDate experienceDate = LocalDate.of(2026, 3, 1);
        ZonedDateTime deadline = experienceDate.atStartOfDay(Clocks.SEOUL);

        Clock clock = Clock.fixed(deadline.toInstant(), Clocks.SEOUL);

        assertThat(TimeBoundary.isRefundable(experienceDate, clock)).isFalse();
    }

    // ── 당일 변경 경계 ────────────────────────────────────────────────────────

    @Test
    void change_61MinutesBefore_allowed() {
        ZonedDateTime slotStart = ZonedDateTime.of(2026, 3, 1, 14, 0, 0, 0, Clocks.SEOUL);

        Clock clock = Clock.fixed(slotStart.minusMinutes(61).toInstant(), Clocks.SEOUL);

        assertThat(angeable(slotStart, clock)).isTrue();
    }

    @Test
    void change_atOneHourBefore_notAllowed() {
        ZonedDateTime slotStart = ZonedDateTime.of(2026, 3, 1, 14, 0, 0, 0, Clocks.SEOUL);
        ZonedDateTime deadline = slotStart.minusHours(1); // 13:00

        Clock clock = Clock.fixed(deadline.toInstant(), Clocks.SEOUL);

        assertThat(TimeBoundary.isChangeable(slotStart, clock)).isFalse();
    }

    // ── 8회권 90일 만료 ───────────────────────────────────────────────────────

    @Test
    void pass_expiresAt_90DaysAfterPurchase() {
        ZonedDateTime purchasedAt = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, Clocks.SEOUL);

        assertThat(TimeBoundary.passExpiresAt(purchasedAt))
                .isEqualTo(ZonedDateTime.of(2026, 4, 1, 12, 0, 0, 0, Clocks.SEOUL));
    }

    // ── 만료 7일 전 알림 ──────────────────────────────────────────────────────

    @Test
    void pass_notificationAt_7DaysBeforeExpiry() {
        ZonedDateTime expiresAt = ZonedDateTime.of(2026, 4, 1, 12, 0, 0, 0, Clocks.SEOUL);

        assertThat(TimeBoundary.passNotificationAt(expiresAt))
                .isEqualTo(ZonedDateTime.of(2026, 3, 25, 12, 0, 0, 0, Clocks.SEOUL));
    }
}

package com.personal.happygallery.policy;

import com.personal.happygallery.common.time.Clocks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [PolicyTest] 시간 경계 정책 검증.
 *
 * <p>Spring 컨텍스트 불필요. Clock.fixed()로 경계값을 재현한다.
 * 타임존 상수: {@link Clocks#Clocks.SEOUL}.
 *
 * <p>네이밍 규칙: 클래스명 suffix = PolicyTest, @Tag("policy")
 */
@Tag("policy")
class TimeBoundaryPolicyTest {

    /**
     * D-1 환불 경계: 체험 전날 00:00 (Asia/Seoul) 이후는 환불 불가.
     */
    @Test
    void dMinus1_oneMinuteBefore_refundAllowed() {
        LocalDate experienceDate = LocalDate.of(2026, 3, 1);
        ZonedDateTime deadline = experienceDate.atStartOfDay(Clocks.SEOUL); // 2026-03-01T00:00+09:00

        Clock before = Clock.fixed(deadline.minusMinutes(1).toInstant(), Clocks.SEOUL);

        assertThat(ZonedDateTime.now(before).isBefore(deadline)).isTrue();
    }

    @Test
    void dMinus1_atMidnight_refundNotAllowed() {
        LocalDate experienceDate = LocalDate.of(2026, 3, 1);
        ZonedDateTime deadline = experienceDate.atStartOfDay(Clocks.SEOUL);

        Clock atDeadline = Clock.fixed(deadline.toInstant(), Clocks.SEOUL);

        assertThat(ZonedDateTime.now(atDeadline).isBefore(deadline)).isFalse();
    }

    /**
     * 당일 변경 경계: 슬롯 시작 1시간 전까지 변경 가능, 이후는 1회 소모.
     */
    @Test
    void sameDay_61MinutesBefore_changeAllowed() {
        ZonedDateTime slotStart = ZonedDateTime.of(2026, 3, 1, 14, 0, 0, 0, Clocks.SEOUL);
        ZonedDateTime changeDeadline = slotStart.minusHours(1); // 13:00

        Clock before = Clock.fixed(slotStart.minusMinutes(61).toInstant(), Clocks.SEOUL);

        assertThat(ZonedDateTime.now(before).isBefore(changeDeadline)).isTrue();
    }

    @Test
    void sameDay_atChangeDeadline_changeNotAllowed() {
        ZonedDateTime slotStart = ZonedDateTime.of(2026, 3, 1, 14, 0, 0, 0, Clocks.SEOUL);
        ZonedDateTime changeDeadline = slotStart.minusHours(1);

        Clock atDeadline = Clock.fixed(changeDeadline.toInstant(), Clocks.SEOUL);

        assertThat(ZonedDateTime.now(atDeadline).isBefore(changeDeadline)).isFalse();
    }
}

package com.personal.happygallery.application.booking;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class KoreanPublicHolidayPolicyTest {

    private final KoreanPublicHolidayPolicy policy = new KoreanPublicHolidayPolicy();

    @DisplayName("2026년 법정 공휴일과 음력 명절 및 대체공휴일을 닫는다")
    @Test
    void publicHolidays_includeCurrentStatutoryAndSubstituteDates() {
        assertSoftly(softly -> {
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 3, 1))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 3, 2))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 5, 1))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 5, 24))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 5, 25))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 7, 17))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 9, 24))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 9, 25))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 9, 26))).isTrue();
            softly.assertThat(policy.isPublicHoliday(LocalDate.of(2026, 10, 5))).isTrue();
        });
    }

    @DisplayName("정기 일요일과 평일은 법정 공휴일 차단 대상으로 보지 않는다")
    @Test
    void regularDays_areNotPublicHolidays() {
        assertThat(policy.isPublicHoliday(LocalDate.of(2026, 8, 30))).isFalse();
        assertThat(policy.isPublicHoliday(LocalDate.of(2026, 8, 31))).isFalse();
    }
}

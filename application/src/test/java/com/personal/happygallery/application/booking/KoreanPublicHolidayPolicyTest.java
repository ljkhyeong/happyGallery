package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("policy")
class KoreanPublicHolidayPolicyTest {

    private final PublicHolidaySnapshotPort snapshotPort = emptySnapshotPort();
    private final KoreanPublicHolidayPolicy policy = new KoreanPublicHolidayPolicy(snapshotPort);

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

    @DisplayName("공식 스냅샷이 있으면 계산 결과 대신 공식 날짜를 사용한다")
    @Test
    void officialSnapshot_overridesCalculatedDates() {
        PublicHolidaySnapshotPort officialSnapshot = mock(PublicHolidaySnapshotPort.class);
        when(officialSnapshot.findDatesByYear(2026))
                .thenReturn(Set.of(LocalDate.of(2026, 8, 17)));
        KoreanPublicHolidayPolicy officialPolicy = new KoreanPublicHolidayPolicy(officialSnapshot);

        assertSoftly(softly -> {
            softly.assertThat(officialPolicy.isPublicHoliday(LocalDate.of(2026, 8, 17))).isTrue();
            softly.assertThat(officialPolicy.isPublicHoliday(LocalDate.of(2026, 3, 1))).isFalse();
        });
    }

    private static PublicHolidaySnapshotPort emptySnapshotPort() {
        PublicHolidaySnapshotPort port = mock(PublicHolidaySnapshotPort.class);
        when(port.findDatesByYear(2026)).thenReturn(Set.of());
        return port;
    }
}

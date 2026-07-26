package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingReminderCandidatePort;
import com.personal.happygallery.application.notification.NotificationOutboxService;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.time.Clocks;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultBookingReminderBatchServiceTest {

    @DisplayName("D-1 배치는 당일 7시 전 재기동 시 놓친 오늘 예약까지 후보로 보충한다")
    @Test
    void sendD1Reminders_beforeSameDayReminder_includesTodayCatchUpWindow() {
        BookingReminderCandidatePort candidatePort =
                mock(BookingReminderCandidatePort.class);
        NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
        Clock clock = Clock.fixed(
                ZonedDateTime.of(2026, 3, 1, 6, 0, 0, 0, Clocks.SEOUL).toInstant(),
                Clocks.SEOUL);
        when(candidatePort.findUnnotifiedBookedAfterId(
                LocalDateTime.of(2026, 3, 1, 6, 0),
                LocalDateTime.of(2026, 3, 3, 0, 0),
                NotificationEventType.REMINDER_D1,
                0L,
                100))
                .thenReturn(List.of());
        DefaultBookingReminderBatchService service =
                new DefaultBookingReminderBatchService(candidatePort, outboxService, clock);

        var result = service.sendD1Reminders();

        assertThat(result.successCount()).isZero();
        verify(candidatePort).findUnnotifiedBookedAfterId(
                LocalDateTime.of(2026, 3, 1, 6, 0),
                LocalDateTime.of(2026, 3, 3, 0, 0),
                NotificationEventType.REMINDER_D1,
                0L,
                100);
    }
}

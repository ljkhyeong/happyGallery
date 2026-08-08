package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationReminderEligibilityPort;
import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationReminderEligibilityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 10, 0);

    @DisplayName("시간 의존 리마인드가 아닌 알림은 aggregate 적격성 조회 대상으로 받지 않는다")
    @Test
    void findEligibleRecipient_nonReminder_rejectsWithoutLookup() {
        NotificationReminderEligibilityPort port = mock(NotificationReminderEligibilityPort.class);
        NotificationReminderEligibility eligibility = new NotificationReminderEligibility(port);

        assertThatThrownBy(() -> eligibility.findEligibleRecipient(
                outbox(NotificationEventType.ORDER_PAID, "ORDER", 10L), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(port);
    }

    @DisplayName("D-1 리마인드는 내일 경계와 현재 예약 수신자를 한 조회로 확인한다")
    @Test
    void findEligibleRecipient_d1Reminder_checksTomorrowAndCurrentRecipient() {
        NotificationReminderEligibilityPort port = mock(NotificationReminderEligibilityPort.class);
        NotificationReminderEligibility eligibility = new NotificationReminderEligibility(port);
        LocalDateTime tomorrowStart = LocalDateTime.of(2026, 3, 2, 0, 0);
        NotificationReminderRecipient currentRecipient = NotificationReminderRecipient.forUser(20L);
        when(port.findD1BookingRecipient(10L, tomorrowStart, tomorrowStart.plusDays(1)))
                .thenReturn(Optional.of(currentRecipient));

        var result = eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REMINDER_D1, "BOOKING", 10L), NOW);

        assertThat(result).contains(currentRecipient);
        verify(port).findD1BookingRecipient(10L, tomorrowStart, tomorrowStart.plusDays(1));
    }

    @DisplayName("당일 리마인드는 오전 7시부터 현재 시각을 제외한 오늘 구간과 현재 수신자를 확인한다")
    @Test
    void findEligibleRecipient_sameDayReminder_checksStrictRemainingWindow() {
        NotificationReminderEligibilityPort port = mock(NotificationReminderEligibilityPort.class);
        NotificationReminderEligibility eligibility = new NotificationReminderEligibility(port);
        LocalDateTime tomorrowStart = LocalDateTime.of(2026, 3, 2, 0, 0);
        NotificationReminderRecipient currentRecipient = NotificationReminderRecipient.forGuest(30L);
        when(port.findSameDayBookingRecipient(10L, NOW, tomorrowStart))
                .thenReturn(Optional.of(currentRecipient));

        var result = eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REMINDER_SAME_DAY, "BOOKING", 10L), NOW);

        assertThat(result).contains(currentRecipient);
        verify(port).findSameDayBookingRecipient(10L, NOW, tomorrowStart);
    }

    @DisplayName("오전 7시 전 당일 리마인드와 aggregate가 맞지 않는 리마인드는 조회하지 않는다")
    @Test
    void findEligibleRecipient_invalidTimeOrAggregate_rejectsWithoutLookup() {
        NotificationReminderEligibilityPort port = mock(NotificationReminderEligibilityPort.class);
        NotificationReminderEligibility eligibility = new NotificationReminderEligibility(port);
        LocalDateTime beforeSeven = LocalDateTime.of(2026, 3, 1, 6, 59);

        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REMINDER_SAME_DAY, "BOOKING", 10L), beforeSeven)).isEmpty();
        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REMINDER_D1, "ORDER", 10L), NOW)).isEmpty();
        verifyNoInteractions(port);
    }

    @DisplayName("8회권과 픽업 리마인드는 현재 구간과 현재 수신자를 한 조회로 확인한다")
    @Test
    void findEligibleRecipient_passAndPickupReminders_checkCurrentWindowsAndRecipients() {
        NotificationReminderEligibilityPort port = mock(NotificationReminderEligibilityPort.class);
        NotificationReminderEligibility eligibility = new NotificationReminderEligibility(port);
        NotificationReminderRecipient passRecipient = NotificationReminderRecipient.forUser(11L);
        NotificationReminderRecipient orderRecipient = NotificationReminderRecipient.forUser(21L);
        when(port.findPassExpiryRecipient(10L, NOW, NOW.plusDays(7)))
                .thenReturn(Optional.of(passRecipient));
        when(port.findPickupDeadlineRecipient(20L, NOW, NOW.plusHours(2)))
                .thenReturn(Optional.of(orderRecipient));

        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.PASS_EXPIRY_SOON, "PASS_PURCHASE", 10L), NOW))
                .contains(passRecipient);
        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.PICKUP_DEADLINE_REMINDER, "ORDER", 20L), NOW))
                .contains(orderRecipient);
        verify(port).findPassExpiryRecipient(10L, NOW, NOW.plusDays(7));
        verify(port).findPickupDeadlineRecipient(20L, NOW, NOW.plusHours(2));
    }

    private static NotificationOutbox outbox(
            NotificationEventType eventType, String aggregateType, Long aggregateId) {
        return NotificationOutbox.from(
                NotificationRequestedEvent.forUser(1L, eventType, aggregateType, aggregateId),
                NOW.minusMinutes(1));
    }
}

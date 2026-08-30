package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingVacancyAlertPort;
import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class BookingVacancyAlertPublisher {

    private final BookingVacancyAlertPort alertPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    BookingVacancyAlertPublisher(
            BookingVacancyAlertPort alertPort,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.alertPort = alertPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void notifyWaitingIfCapacityOpened(Slot slot, boolean wasFull) {
        if (!wasFull) return;
        notifyWaitingIfAvailable(slot);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void notifyWaitingIfAvailable(Slot slot) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!slot.isReservableAt(now) || slot.getBookedCount() >= slot.getCapacity()) {
            return;
        }

        List<BookingVacancyAlert> alerts = alertPort.findWaitingBySlotIdForUpdate(slot.getId());
        if (alerts.isEmpty()) return;

        alerts.forEach(alert -> alert.markNotified(now));
        alertPort.saveAll(alerts);
        alerts.forEach(this::publishNotification);
    }

    private void publishNotification(BookingVacancyAlert alert) {
        NotificationRequestedEvent event = alert.getUserId() != null
                ? NotificationRequestedEvent.forUser(
                        alert.getUserId(),
                        NotificationEventType.BOOKING_VACANCY_AVAILABLE,
                        "VACANCY_ALERT",
                        alert.getId())
                : NotificationRequestedEvent.forGuest(
                        alert.getGuestId(),
                        NotificationEventType.BOOKING_VACANCY_AVAILABLE,
                        "VACANCY_ALERT",
                        alert.getId());
        eventPublisher.publishEvent(event);
    }
}

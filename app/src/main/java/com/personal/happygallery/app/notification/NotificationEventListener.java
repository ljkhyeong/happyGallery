package com.personal.happygallery.app.notification;

import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class NotificationEventListener {

    private final NotificationService notificationService;

    NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(NotificationRequestedEvent event) {
        if (event.guestId() != null && event.phone() != null) {
            notificationService.sendToGuest(event.guestId(), event.phone(), event.name(), event.eventType());
        } else if (event.guestId() != null) {
            notificationService.sendByGuestId(event.guestId(), event.eventType());
        } else if (event.userId() != null) {
            notificationService.sendByUserId(event.userId(), event.eventType());
        }
    }
}

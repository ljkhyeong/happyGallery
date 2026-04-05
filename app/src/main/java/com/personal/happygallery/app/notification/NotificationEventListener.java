package com.personal.happygallery.app.notification;

import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForGuest;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForGuestWithContact;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForUser;
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
        switch (event) {
            case ForGuestWithContact e ->
                    notificationService.sendToGuest(e.guestId(), e.phone(), e.name(), e.eventType());
            case ForGuest e ->
                    notificationService.sendByGuestId(e.guestId(), e.eventType());
            case ForUser e ->
                    notificationService.sendByUserId(e.userId(), e.eventType());
        }
    }
}

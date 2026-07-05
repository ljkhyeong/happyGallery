package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.batch.BatchResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxScheduler {

    private final NotificationOutboxDispatcher dispatcher;

    NotificationOutboxScheduler(NotificationOutboxDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-delay-ms:5000}")
    public BatchResult dispatchPending() {
        return dispatcher.dispatchPending();
    }
}

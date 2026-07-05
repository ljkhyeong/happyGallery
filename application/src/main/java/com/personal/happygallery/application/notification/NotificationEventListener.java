package com.personal.happygallery.application.notification;

import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class NotificationEventListener {

    private final NotificationOutboxService outboxService;
    private final NotificationOutboxDispatcher outboxDispatcher;

    NotificationEventListener(NotificationOutboxService outboxService,
                              NotificationOutboxDispatcher outboxDispatcher) {
        this.outboxService = outboxService;
        this.outboxDispatcher = outboxDispatcher;
    }

    @EventListener
    public void handle(NotificationRequestedEvent event) {
        if (outboxService.enqueue(event)) {
            dispatchAfterCommit();
        }
    }

    private void dispatchAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            outboxDispatcher.dispatchAsync();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                outboxDispatcher.dispatchAsync();
            }
        });
    }
}

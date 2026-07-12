package com.personal.happygallery.application.notification;

import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class NotificationEventListener {

    private final NotificationOutboxService outboxService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationOutboxDispatcher outboxDispatcher;

    NotificationEventListener(NotificationOutboxService outboxService,
                              ApplicationEventPublisher eventPublisher,
                              NotificationOutboxDispatcher outboxDispatcher) {
        this.outboxService = outboxService;
        this.eventPublisher = eventPublisher;
        this.outboxDispatcher = outboxDispatcher;
    }

    @EventListener
    public void handle(NotificationRequestedEvent event) {
        if (outboxService.enqueue(event)) {
            eventPublisher.publishEvent(new NotificationOutboxEnqueuedEvent());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void dispatchAfterCommit(NotificationOutboxEnqueuedEvent event) {
        outboxDispatcher.dispatchAsync();
    }
}

record NotificationOutboxEnqueuedEvent() {}

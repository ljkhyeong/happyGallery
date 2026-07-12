package com.personal.happygallery.application.notification;

import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

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

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void dispatchAfterCommit(NotificationOutboxEnqueuedEvent event) {
        try {
            outboxDispatcher.dispatchPending();
        } catch (Exception e) {
            log.warn("[알림 outbox] 비동기 dispatch 실패", e);
        }
    }
}

record NotificationOutboxEnqueuedEvent() {}

package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationOutboxTransactionService {

    private static final int MAX_BACKOFF_EXPONENT = 5;

    private final NotificationOutboxPort outboxPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    NotificationOutboxTransactionService(NotificationOutboxPort outboxPort,
                                         ApplicationEventPublisher eventPublisher,
                                         Clock clock) {
        this.outboxPort = outboxPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> reserveDispatchableIds(int limit, int processingTimeoutMinutes) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime staleBefore = now.minusMinutes(processingTimeoutMinutes);
        List<NotificationOutbox> outboxes = outboxPort.findDispatchable(now, staleBefore, limit);
        for (NotificationOutbox outbox : outboxes) {
            outbox.markProcessing(now);
        }
        return outboxes.stream()
                .map(NotificationOutbox::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationOutboxDeliveryRequest loadRequest(Long outboxId) {
        NotificationOutbox outbox = findOutbox(outboxId);
        return new NotificationOutboxDeliveryRequest(
                outbox.getId(),
                outbox.getRecipientType(),
                outbox.getGuestId(),
                outbox.getUserId(),
                outbox.getEventType());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long outboxId) {
        NotificationOutbox outbox = findOutbox(outboxId);
        outbox.markSent(LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDeliveryFailed(Long outboxId, String reason, int maxAttempts) {
        NotificationOutbox outbox = findOutbox(outboxId);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean permanentlyFailed = outbox.markDeliveryFailed(
                reason, nextAttemptAt(outbox, now), now, maxAttempts);
        if (permanentlyFailed) {
            eventPublisher.publishEvent(new NotificationOutboxFailedEvent(outbox.getId()));
        }
    }

    private NotificationOutbox findOutbox(Long outboxId) {
        return outboxPort.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("알림 outbox 미존재: " + outboxId));
    }

    private LocalDateTime nextAttemptAt(NotificationOutbox outbox, LocalDateTime now) {
        int backoffExponent = Math.clamp((long) outbox.getAttemptCount(), 0, MAX_BACKOFF_EXPONENT);
        long delayMinutes = 1L << backoffExponent;
        return now.plusMinutes(delayMinutes);
    }
}

record NotificationOutboxDeliveryRequest(Long outboxId,
                                         NotificationRecipientType recipientType,
                                         Long guestId,
                                         Long userId,
                                         NotificationEventType eventType) {}

record NotificationOutboxFailedEvent(Long outboxId) {}

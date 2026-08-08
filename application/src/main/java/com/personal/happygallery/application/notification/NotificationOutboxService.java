package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxInsertPort;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForGuest;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForUser;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxService {

    private final NotificationOutboxInsertPort outboxInsertPort;
    private final NotificationOutboxPort outboxPort;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public NotificationOutboxService(NotificationOutboxInsertPort outboxInsertPort,
                                     NotificationOutboxPort outboxPort,
                                     ApplicationEventPublisher eventPublisher,
                                     Clock clock) {
        this.outboxInsertPort = outboxInsertPort;
        this.outboxPort = outboxPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public boolean enqueue(NotificationRequestedEvent event) {
        if (!hasRecipient(event)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        boolean enqueued = outboxInsertPort.insertIfAbsent(NotificationOutbox.from(event, now));
        if (!enqueued) {
            enqueued = reactivateObsolete(event, now);
        }
        if (enqueued) {
            eventPublisher.publishEvent(new NotificationOutboxEnqueuedEvent());
        }
        return enqueued;
    }

    private boolean reactivateObsolete(NotificationRequestedEvent event, LocalDateTime now) {
        if (!event.eventType().isTimeSensitiveReminder()) {
            return false;
        }
        var existing = outboxPort.findByIdempotencyKeyForUpdate(event.idempotencyKey());
        if (existing.isEmpty() || !existing.get().reactivateObsolete(event, now)) {
            return false;
        }
        outboxPort.save(existing.get());
        return true;
    }

    private boolean hasRecipient(NotificationRequestedEvent event) {
        return switch (event) {
            case ForGuest e -> e.guestId() != null;
            case ForUser e -> e.userId() != null;
        };
    }
}

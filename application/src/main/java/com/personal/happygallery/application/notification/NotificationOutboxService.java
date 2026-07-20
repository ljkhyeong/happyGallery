package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxInsertPort;
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
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public NotificationOutboxService(NotificationOutboxInsertPort outboxInsertPort,
                                     ApplicationEventPublisher eventPublisher,
                                     Clock clock) {
        this.outboxInsertPort = outboxInsertPort;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public boolean enqueue(NotificationRequestedEvent event) {
        if (!hasRecipient(event)) {
            return false;
        }
        boolean inserted = outboxInsertPort.insertIfAbsent(
                NotificationOutbox.from(event, LocalDateTime.now(clock)));
        if (inserted) {
            eventPublisher.publishEvent(new NotificationOutboxEnqueuedEvent());
        }
        return inserted;
    }

    private boolean hasRecipient(NotificationRequestedEvent event) {
        return switch (event) {
            case ForGuest e -> e.guestId() != null;
            case ForUser e -> e.userId() != null;
        };
    }
}

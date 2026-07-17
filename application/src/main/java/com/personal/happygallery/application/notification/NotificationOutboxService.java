package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForGuest;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForUser;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxService {

    private final NotificationOutboxPort outboxPort;
    private final Clock clock;

    public NotificationOutboxService(NotificationOutboxPort outboxPort, Clock clock) {
        this.outboxPort = outboxPort;
        this.clock = clock;
    }

    @Transactional
    public boolean enqueue(NotificationRequestedEvent event) {
        if (!hasRecipient(event) || outboxPort.existsByIdempotencyKey(event.idempotencyKey())) {
            return false;
        }
        outboxPort.save(NotificationOutbox.from(event, LocalDateTime.now(clock)));
        return true;
    }

    private boolean hasRecipient(NotificationRequestedEvent event) {
        return switch (event) {
            case ForGuest e -> e.guestId() != null;
            case ForUser e -> e.userId() != null;
        };
    }
}

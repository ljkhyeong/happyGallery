package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.in.NotificationFailureAdminUseCase;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultNotificationFailureAdminService implements NotificationFailureAdminUseCase {

    private static final int LIST_LIMIT = 100;

    private final NotificationOutboxPort outboxPort;
    private final Clock clock;

    public DefaultNotificationFailureAdminService(NotificationOutboxPort outboxPort, Clock clock) {
        this.outboxPort = outboxPort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationOutbox> listFailed() {
        return outboxPort.findFailed(LIST_LIMIT);
    }

    @Override
    @Transactional
    public NotificationOutbox retry(Long outboxId) {
        NotificationOutbox outbox = outboxPort.findByIdForUpdate(outboxId)
                .orElseThrow(NotFoundException.supplier("알림 outbox"));
        outbox.retryFailed(LocalDateTime.now(clock));
        return outboxPort.save(outbox);
    }
}

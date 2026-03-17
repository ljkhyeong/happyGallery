package com.personal.happygallery.app.notification;

import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.app.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationLogRepository}(infra) → {@link NotificationLogReaderPort} + {@link NotificationLogStorePort}(app) 브릿지 어댑터.
 */
@Component
class NotificationLogPersistencePortAdapter implements NotificationLogReaderPort, NotificationLogStorePort {

    private final NotificationLogRepository notificationLogRepository;

    NotificationLogPersistencePortAdapter(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public boolean existsSentNotification(Long guestId, NotificationEventType eventType,
                                          LocalDateTime sentStart, LocalDateTime sentEnd) {
        return notificationLogRepository.existsByGuestIdAndEventTypeAndStatusAndSentAtBetween(
                guestId, eventType, "SUCCESS", sentStart, sentEnd);
    }

    @Override
    public NotificationLog save(NotificationLog log) {
        return notificationLogRepository.save(log);
    }
}

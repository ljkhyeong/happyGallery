package com.personal.happygallery.app.notification;

import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
class NotificationLogReaderPortAdapter implements NotificationLogReaderPort {

    private final NotificationLogRepository notificationLogRepository;

    NotificationLogReaderPortAdapter(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public boolean existsSentNotification(Long guestId, NotificationEventType eventType,
                                          LocalDateTime sentStart, LocalDateTime sentEnd) {
        return notificationLogRepository.existsByGuestIdAndEventTypeAndStatusAndSentAtBetween(
                guestId, eventType, "SUCCESS", sentStart, sentEnd);
    }
}

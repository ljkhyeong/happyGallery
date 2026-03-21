package com.personal.happygallery.infra.notification;

import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationLogRepository} → {@link NotificationLogReaderPort} 어댑터.
 * "SUCCESS" 하드코딩 파라미터 변환이 필요하여 Repository extends 방식이 아닌 별도 어댑터로 구현.
 */
@Component
class JpaNotificationLogAdapter implements NotificationLogReaderPort {

    private final NotificationLogRepository notificationLogRepository;

    JpaNotificationLogAdapter(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public boolean existsSentNotification(Long guestId, NotificationEventType eventType,
                                          LocalDateTime sentStart, LocalDateTime sentEnd) {
        return notificationLogRepository.existsByGuestIdAndEventTypeAndStatusAndSentAtBetween(
                guestId, eventType, "SUCCESS", sentStart, sentEnd);
    }

    @Override
    public boolean existsSentUserNotification(Long userId, NotificationEventType eventType,
                                              LocalDateTime sentStart, LocalDateTime sentEnd) {
        return notificationLogRepository.existsByUserIdAndEventTypeAndStatusAndSentAtBetween(
                userId, eventType, "SUCCESS", sentStart, sentEnd);
    }
}

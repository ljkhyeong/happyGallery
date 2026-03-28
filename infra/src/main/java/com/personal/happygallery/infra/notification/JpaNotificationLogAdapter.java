package com.personal.happygallery.infra.notification;

import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link NotificationLogRepository} ��� {@link NotificationLogReaderPort} 어댑터.
 * "SUCCESS" 하드코딩 파라미터 변환�� 필요하여 Repository extends 방식이 아닌 별��� 어댑터로 구현.
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

    @Override
    public Optional<NotificationLog> findById(Long id) {
        return notificationLogRepository.findById(id);
    }

    @Override
    public List<NotificationLog> findByUserIdOrderBySentAtDesc(Long userId, int limit, int offset) {
        return notificationLogRepository.findByUserIdOrderBySentAtDesc(
                userId, PageRequest.of(offset / limit, limit));
    }

    @Override
    public List<NotificationLog> findByGuestIdOrderBySentAtDesc(Long guestId, int limit, int offset) {
        return notificationLogRepository.findByGuestIdOrderBySentAtDesc(
                guestId, PageRequest.of(offset / limit, limit));
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        return notificationLogRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    public long countUnreadByGuestId(Long guestId) {
        return notificationLogRepository.countByGuestIdAndReadAtIsNull(guestId);
    }

    @Override
    @Transactional
    public void markAllReadByUserId(Long userId, LocalDateTime now) {
        notificationLogRepository.markAllReadByUserId(userId, now);
    }

    @Override
    @Transactional
    public void markAllReadByGuestId(Long guestId, LocalDateTime now) {
        notificationLogRepository.markAllReadByGuestId(guestId, now);
    }
}

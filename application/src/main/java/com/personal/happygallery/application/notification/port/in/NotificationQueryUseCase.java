package com.personal.happygallery.application.notification.port.in;

import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationQueryUseCase {

    record NotificationView(Long id, NotificationEventType eventType,
                            String aggregateType, Long aggregateId,
                            LocalDateTime deliveredAt, LocalDateTime readAt,
                            String contextTitle, LocalDateTime scheduledAt) {
        public NotificationView(Long id, NotificationEventType eventType, String aggregateType,
                                Long aggregateId, LocalDateTime deliveredAt, LocalDateTime readAt) {
            this(id, eventType, aggregateType, aggregateId, deliveredAt, readAt, null, null);
        }
        public boolean isRead() { return readAt != null; }
    }

    List<NotificationView> listNotifications(Long userId, Long guestId, int page, int size, boolean unreadOnly);

    long countUnread(Long userId, Long guestId);

    void markAsRead(Long notificationId, Long userId, Long guestId);

    void markAllAsRead(Long userId, Long guestId);
}

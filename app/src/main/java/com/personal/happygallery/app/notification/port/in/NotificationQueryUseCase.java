package com.personal.happygallery.app.notification.port.in;

import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationQueryUseCase {

    record NotificationView(Long id, NotificationChannel channel, NotificationEventType eventType,
                            String status, LocalDateTime sentAt, LocalDateTime readAt) {
        public boolean isRead() { return readAt != null; }
    }

    List<NotificationView> listNotifications(Long userId, Long guestId, int page, int size);

    long countUnread(Long userId, Long guestId);

    void markAsRead(Long notificationId, Long userId, Long guestId);

    void markAllAsRead(Long userId, Long guestId);
}

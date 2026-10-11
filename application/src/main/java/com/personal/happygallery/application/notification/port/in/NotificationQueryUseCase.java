package com.personal.happygallery.application.notification.port.in;

import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationQueryUseCase {

    record NotificationView(Long id, NotificationEventType eventType,
                            NotificationAggregateType aggregateType, Long aggregateId,
                            LocalDateTime deliveredAt, LocalDateTime readAt,
                            String contextTitle, LocalDateTime scheduledAt,
                            NotificationTarget target) {
        public boolean isRead() { return readAt != null; }
    }

    /** 알림을 눌렀을 때 열 본인 기록. 원본이 없거나 타인 소유이면 값을 비운다. */
    record NotificationTarget(Long orderId, Long bookingId, Long productId, Long productVariantId,
                              Long classId, Long slotId) {
        public static final NotificationTarget NONE = new NotificationTarget(null, null, null, null, null, null);
    }

    List<NotificationView> listNotifications(Long userId, int page, int size, boolean unreadOnly);

    long countUnread(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}

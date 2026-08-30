package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultNotificationQueryService implements NotificationQueryUseCase {

    private final NotificationOutboxPort outboxPort;
    private final Clock clock;

    public DefaultNotificationQueryService(NotificationOutboxPort outboxPort, Clock clock) {
        this.outboxPort = outboxPort;
        this.clock = clock;
    }

    @Override
    public List<NotificationView> listNotifications(Long userId, Long guestId, int page, int size) {
        int offset = PageParams.offset(page, size);
        List<NotificationOutbox> notifications = (userId != null)
                ? outboxPort.findSentByUserId(userId, size, offset)
                : outboxPort.findSentByGuestId(guestId, size, offset);

        return notifications.stream()
                .map(outbox -> new NotificationView(
                        outbox.getId(), outbox.getEventType(),
                        outbox.getAggregateType(), outbox.getAggregateId(),
                        outbox.getProcessedAt(), outbox.getReadAt()))
                .toList();
    }

    @Override
    public long countUnread(Long userId, Long guestId) {
        return (userId != null)
                ? outboxPort.countUnreadSentByUserId(userId)
                : outboxPort.countUnreadSentByGuestId(guestId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId, Long guestId) {
        NotificationOutbox notification = outboxPort.findById(notificationId)
                .orElseThrow(NotFoundException.supplier("알림"));

        boolean isOwner = (userId != null && userId.equals(notification.getUserId()))
                || (guestId != null && guestId.equals(notification.getGuestId()));
        if (!isOwner || notification.getStatus() != NotificationOutboxStatus.SENT) {
            throw new NotFoundException("알림");
        }

        notification.markRead(LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId, Long guestId) {
        LocalDateTime readAt = LocalDateTime.now(clock);
        if (userId != null) {
            outboxPort.markAllSentReadByUserId(userId, readAt);
        } else {
            outboxPort.markAllSentReadByGuestId(guestId, readAt);
        }
    }
}

package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.application.notification.port.out.NotificationContextPort;
import com.personal.happygallery.application.notification.port.out.NotificationContextPort.Context;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultNotificationQueryService implements NotificationQueryUseCase {

    private final NotificationOutboxPort outboxPort;
    private final Clock clock;
    private final NotificationContextPort contextPort;

    public DefaultNotificationQueryService(NotificationOutboxPort outboxPort, Clock clock, NotificationContextPort contextPort) {
        this.outboxPort = outboxPort;
        this.clock = clock;
        this.contextPort = contextPort;
    }

    @Override
    public List<NotificationView> listNotifications(Long userId, Long guestId, int page, int size, boolean unreadOnly) {
        int offset = PageParams.offset(page, size);
        List<NotificationOutbox> notifications = (userId != null)
                ? outboxPort.findSentByUserId(userId, unreadOnly, size, offset)
                : outboxPort.findSentByGuestId(guestId, unreadOnly, size, offset);

        var contexts = contextPort.findContexts(
                        notifications.stream().map(NotificationOutbox::getId).toList(), userId, guestId)
                .stream().collect(Collectors.toMap(Context::notificationId, Function.identity()));
        return notifications.stream().map(outbox -> {
            Context context = contexts.get(outbox.getId());
            return new NotificationView(outbox.getId(), outbox.getEventType(),
                    outbox.getAggregateType(), outbox.getAggregateId(),
                    outbox.getProcessedAt(), outbox.getReadAt(), contextTitle(outbox, context),
                    context == null ? null : context.scheduledAt());
        }).toList();
    }

    private String contextTitle(NotificationOutbox outbox, Context context) {
        if (context == null || context.name() == null) return null;
        return switch (outbox.getAggregateType()) {
            case "ORDER" -> "주문 #" + outbox.getAggregateId() + " · " + context.name()
                    + (context.itemCount() > 1 ? " 외 " + (context.itemCount() - 1) + "건" : "");
            case "BOOKING" -> "예약 #" + outbox.getAggregateId() + " · " + context.name();
            case "RESTOCK_ALERT" -> "재입고 · " + context.name();
            default -> null;
        };
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

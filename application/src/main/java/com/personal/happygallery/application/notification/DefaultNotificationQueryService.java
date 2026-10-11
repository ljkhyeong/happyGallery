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
    public List<NotificationView> listNotifications(Long userId, int page, int size, boolean unreadOnly) {
        // 페이지 번호·크기와 offset 범위를 검증한다.
        PageParams.offset(page, size);
        List<NotificationOutbox> notifications = outboxPort.findSentByUserId(userId, unreadOnly, page, size);

        var contexts = contextPort.findContexts(
                        notifications.stream().map(NotificationOutbox::getId).toList(), userId)
                .stream().collect(Collectors.toMap(Context::notificationId, Function.identity()));
        return notifications.stream().map(outbox -> {
            Context context = contexts.get(outbox.getId());
            return new NotificationView(outbox.getId(), outbox.getEventType(),
                    outbox.getAggregateType(), outbox.getAggregateId(),
                    outbox.getProcessedAt(), outbox.getReadAt(), contextTitle(outbox, context),
                    context == null ? null : context.scheduledAt(), target(context));
        }).toList();
    }

    // 클레임·환불 알림도 원래 주문·예약 번호로 보여 준다.
    private String contextTitle(NotificationOutbox outbox, Context context) {
        if (context == null || context.name() == null) return null;
        if (context.orderId() != null) {
            return "주문 #" + context.orderId() + " · " + context.name()
                    + (context.itemCount() > 1 ? " 외 " + (context.itemCount() - 1) + "건" : "");
        }
        if (context.bookingId() != null) return "예약 #" + context.bookingId() + " · " + context.name();
        return switch (outbox.getAggregateType()) {
            case VACANCY_ALERT -> "빈자리 · " + context.name();
            case RESTOCK_ALERT -> "재입고 · " + context.name();
            case PRODUCT_QNA -> "상품 문의 · " + context.name();
            case null, default -> null;
        };
    }

    private static NotificationTarget target(Context context) {
        if (context == null) return NotificationTarget.NONE;
        return new NotificationTarget(context.orderId(), context.bookingId(), context.productId(),
                context.productVariantId(), context.classId(), context.slotId());
    }

    @Override
    public long countUnread(Long userId) {
        return outboxPort.countUnreadSentByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        NotificationOutbox notification = outboxPort.findById(notificationId)
                .orElseThrow(NotFoundException.supplier("알림"));

        if (!userId.equals(notification.getUserId())
                || notification.getStatus() != NotificationOutboxStatus.SENT) {
            throw new NotFoundException("알림");
        }

        notification.markRead(LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        outboxPort.markAllSentReadByUserId(userId, LocalDateTime.now(clock));
    }
}

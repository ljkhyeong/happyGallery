package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.notification.NotificationOutboxService;
import com.personal.happygallery.application.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.PickupReminderTarget;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 픽업 마감 2시간 전 알림 배치 서비스 (PRD §3.3).
 *
 * <p>매시간 실행되며, {@code pickup_deadline_at}이 now~now+2h 범위인
 * {@code PICKUP_READY} 주문에 알림을 발송한다. 주문 ID 기반 outbox 멱등키로 중복을 방지한다.
 */
@Service
public class DefaultPickupDeadlineReminderBatchService implements PickupDeadlineReminderBatchUseCase {

    private final FulfillmentPort fulfillmentPort;
    private final NotificationOutboxService notificationOutboxService;
    private final Clock clock;

    public DefaultPickupDeadlineReminderBatchService(FulfillmentPort fulfillmentPort,
                                                      NotificationOutboxService notificationOutboxService,
                                                      Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.notificationOutboxService = notificationOutboxService;
        this.clock = clock;
    }

    @Override
    public BatchResult sendPickupDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime twoHoursLater = now.plusHours(2);
        List<PickupReminderTarget> candidates = fulfillmentPort.findPickupReminderTargets(now, twoHoursLater);

        return BatchExecutor.execute(candidates,
                PickupReminderTarget::orderId,
                this::processReminder,
                "픽업 마감 알림");
    }

    private boolean processReminder(PickupReminderTarget target) {
        Long orderId = target.orderId();
        NotificationEventType eventType = NotificationEventType.PICKUP_DEADLINE_REMINDER;

        NotificationRequestedEvent event = target.userId() != null
                ? NotificationRequestedEvent.forUserOncePerAggregate(
                        target.userId(), eventType, "ORDER", orderId)
                : NotificationRequestedEvent.forGuestOncePerAggregate(
                        target.guestId(), eventType, "ORDER", orderId);
        return notificationOutboxService.enqueue(event);
    }
}

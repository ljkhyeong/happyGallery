package com.personal.happygallery.app.order;

import com.personal.happygallery.app.batch.BatchExecutor;
import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.app.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.app.order.port.out.FulfillmentPort;
import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 픽업 마감 2시간 전 알림 배치 서비스 (PRD §3.3).
 *
 * <p>매시간 실행되며, {@code pickup_deadline_at}이 now~now+2h 범위인
 * {@code PICKUP_READY} 주문에 알림을 발송한다. 24시간 내 중복 발송을 방지한다.
 */
@Service
public class DefaultPickupDeadlineReminderBatchService implements PickupDeadlineReminderBatchUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPickupDeadlineReminderBatchService.class);

    private final FulfillmentPort fulfillmentPort;
    private final OrderReaderPort orderReaderPort;
    private final NotificationService notificationService;
    private final NotificationLogReaderPort notificationLogReader;
    private final Clock clock;

    public DefaultPickupDeadlineReminderBatchService(FulfillmentPort fulfillmentPort,
                                                      OrderReaderPort orderReaderPort,
                                                      NotificationService notificationService,
                                                      NotificationLogReaderPort notificationLogReader,
                                                      Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.orderReaderPort = orderReaderPort;
        this.notificationService = notificationService;
        this.notificationLogReader = notificationLogReader;
        this.clock = clock;
    }

    @Override
    public BatchResult sendPickupDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime twoHoursLater = now.plusHours(2);
        List<Fulfillment> candidates = fulfillmentPort.findPickupsApproachingDeadline(now, twoHoursLater);

        return BatchExecutor.execute(candidates,
                Fulfillment::getOrderId,
                f -> processReminder(f, now),
                "픽업 마감 알림");
    }

    private boolean processReminder(Fulfillment fulfillment, LocalDateTime now) {
        Long orderId = fulfillment.getOrderId();
        Order order = orderReaderPort.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("주문 미존재: " + orderId));

        LocalDateTime deduplicationStart = now.minusHours(24);
        NotificationEventType eventType = NotificationEventType.PICKUP_DEADLINE_REMINDER;

        if (order.getUserId() != null) {
            if (notificationLogReader.existsSentUserNotification(
                    order.getUserId(), eventType, deduplicationStart, now)) {
                log.info("픽업 마감 알림 중복 스킵 [orderId={} userId={}]", orderId, order.getUserId());
                return false;
            }
            notificationService.notifyByUserId(order.getUserId(), eventType);
        } else if (order.getGuestId() != null) {
            if (notificationLogReader.existsSentNotification(
                    order.getGuestId(), eventType, deduplicationStart, now)) {
                log.info("픽업 마감 알림 중복 스킵 [orderId={} guestId={}]", orderId, order.getGuestId());
                return false;
            }
            notificationService.notifyByGuestId(order.getGuestId(), eventType);
        } else {
            log.warn("픽업 마감 알림 대상 없음 [orderId={}]", orderId);
            return false;
        }
        return true;
    }
}

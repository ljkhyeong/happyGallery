package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.application.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationLogReaderPort notificationLogReader;
    private final Clock clock;

    public DefaultPickupDeadlineReminderBatchService(FulfillmentPort fulfillmentPort,
                                                      OrderReaderPort orderReaderPort,
                                                      ApplicationEventPublisher eventPublisher,
                                                      NotificationLogReaderPort notificationLogReader,
                                                      Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.orderReaderPort = orderReaderPort;
        this.eventPublisher = eventPublisher;
        this.notificationLogReader = notificationLogReader;
        this.clock = clock;
    }

    @Override
    public BatchResult sendPickupDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime twoHoursLater = now.plusHours(2);
        List<Fulfillment> candidates = fulfillmentPort.findPickupsApproachingDeadline(now, twoHoursLater);
        Map<Long, Order> ordersById = findOrders(candidates);
        LocalDateTime deduplicationStart = now.minusHours(24);
        Set<Long> notifiedUserIds = findNotifiedUserIds(ordersById, deduplicationStart, now);
        Set<Long> notifiedGuestIds = findNotifiedGuestIds(ordersById, deduplicationStart, now);

        return BatchExecutor.execute(candidates,
                Fulfillment::getOrderId,
                fulfillment -> processReminder(
                        fulfillment, ordersById, notifiedUserIds, notifiedGuestIds),
                "픽업 마감 알림");
    }

    private boolean processReminder(Fulfillment fulfillment,
                                    Map<Long, Order> ordersById,
                                    Set<Long> notifiedUserIds,
                                    Set<Long> notifiedGuestIds) {
        Long orderId = fulfillment.getOrderId();
        Order order = ordersById.get(orderId);
        if (order == null) {
            throw new IllegalStateException("주문 미존재: " + orderId);
        }

        NotificationEventType eventType = NotificationEventType.PICKUP_DEADLINE_REMINDER;

        if (order.getUserId() != null) {
            if (notifiedUserIds.contains(order.getUserId())) {
                log.info("픽업 마감 알림 중복 스킵 [orderId={} userId={}]", orderId, order.getUserId());
                return false;
            }
            eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                    order.getUserId(), eventType, "ORDER", orderId));
        } else if (order.getGuestId() != null) {
            if (notifiedGuestIds.contains(order.getGuestId())) {
                log.info("픽업 마감 알림 중복 스킵 [orderId={} guestId={}]", orderId, order.getGuestId());
                return false;
            }
            eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(
                    order.getGuestId(), eventType, "ORDER", orderId));
        } else {
            log.warn("픽업 마감 알림 대상 없음 [orderId={}]", orderId);
            return false;
        }
        return true;
    }

    private Map<Long, Order> findOrders(List<Fulfillment> candidates) {
        List<Long> orderIds = candidates.stream()
                .map(Fulfillment::getOrderId)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderReaderPort.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
    }

    private Set<Long> findNotifiedUserIds(Map<Long, Order> ordersById,
                                          LocalDateTime sentStart,
                                          LocalDateTime sentEnd) {
        List<Long> userIds = ordersById.values().stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(notificationLogReader.findSentUserIds(
                userIds, NotificationEventType.PICKUP_DEADLINE_REMINDER, sentStart, sentEnd));
    }

    private Set<Long> findNotifiedGuestIds(Map<Long, Order> ordersById,
                                           LocalDateTime sentStart,
                                           LocalDateTime sentEnd) {
        List<Long> guestIds = ordersById.values().stream()
                .map(Order::getGuestId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (guestIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(notificationLogReader.findSentGuestIds(
                guestIds, NotificationEventType.PICKUP_DEADLINE_REMINDER, sentStart, sentEnd));
    }
}

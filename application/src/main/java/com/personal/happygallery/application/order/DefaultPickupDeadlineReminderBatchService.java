package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.notification.port.out.NotificationLogReaderPort;
import com.personal.happygallery.application.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.PickupReminderTarget;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationLogReaderPort notificationLogReader;
    private final Clock clock;

    public DefaultPickupDeadlineReminderBatchService(FulfillmentPort fulfillmentPort,
                                                      ApplicationEventPublisher eventPublisher,
                                                      NotificationLogReaderPort notificationLogReader,
                                                      Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.eventPublisher = eventPublisher;
        this.notificationLogReader = notificationLogReader;
        this.clock = clock;
    }

    @Override
    public BatchResult sendPickupDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime twoHoursLater = now.plusHours(2);
        List<PickupReminderTarget> candidates = fulfillmentPort.findPickupReminderTargets(now, twoHoursLater);
        LocalDateTime deduplicationStart = now.minusHours(24);
        Set<Long> notifiedUserIds = findNotifiedUserIds(candidates, deduplicationStart, now);
        Set<Long> notifiedGuestIds = findNotifiedGuestIds(candidates, deduplicationStart, now);

        return BatchExecutor.execute(candidates,
                PickupReminderTarget::orderId,
                target -> processReminder(target, notifiedUserIds, notifiedGuestIds),
                "픽업 마감 알림");
    }

    private boolean processReminder(PickupReminderTarget target,
                                    Set<Long> notifiedUserIds,
                                    Set<Long> notifiedGuestIds) {
        Long orderId = target.orderId();
        NotificationEventType eventType = NotificationEventType.PICKUP_DEADLINE_REMINDER;

        if (target.userId() != null) {
            if (notifiedUserIds.contains(target.userId())) {
                log.info("픽업 마감 알림 중복 스킵 [orderId={} userId={}]", orderId, target.userId());
                return false;
            }
            eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                    target.userId(), eventType, "ORDER", orderId));
        } else if (target.guestId() != null) {
            if (notifiedGuestIds.contains(target.guestId())) {
                log.info("픽업 마감 알림 중복 스킵 [orderId={} guestId={}]", orderId, target.guestId());
                return false;
            }
            eventPublisher.publishEvent(NotificationRequestedEvent.forGuest(
                    target.guestId(), eventType, "ORDER", orderId));
        } else {
            log.warn("픽업 마감 알림 대상 없음 [orderId={}]", orderId);
            return false;
        }
        return true;
    }

    private Set<Long> findNotifiedUserIds(List<PickupReminderTarget> candidates,
                                          LocalDateTime sentStart,
                                          LocalDateTime sentEnd) {
        List<Long> userIds = candidates.stream()
                .map(PickupReminderTarget::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(notificationLogReader.findSentUserIds(
                userIds, NotificationEventType.PICKUP_DEADLINE_REMINDER, sentStart, sentEnd));
    }

    private Set<Long> findNotifiedGuestIds(List<PickupReminderTarget> candidates,
                                           LocalDateTime sentStart,
                                           LocalDateTime sentEnd) {
        List<Long> guestIds = candidates.stream()
                .map(PickupReminderTarget::guestId)
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

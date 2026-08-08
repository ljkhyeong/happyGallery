package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationReminderEligibilityPort;
import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class NotificationReminderEligibility {

    private static final String BOOKING = "BOOKING";
    private static final String PASS_PURCHASE = "PASS_PURCHASE";
    private static final String ORDER = "ORDER";
    private static final LocalTime SAME_DAY_REMINDER_START = LocalTime.of(7, 0);

    private final NotificationReminderEligibilityPort eligibilityPort;

    NotificationReminderEligibility(NotificationReminderEligibilityPort eligibilityPort) {
        this.eligibilityPort = eligibilityPort;
    }

    Optional<NotificationReminderRecipient> findEligibleRecipient(
            NotificationOutbox outbox, LocalDateTime now) {
        return switch (outbox.getEventType()) {
            case REMINDER_D1 -> findD1BookingRecipient(outbox, now);
            case REMINDER_SAME_DAY -> findSameDayBookingRecipient(outbox, now);
            case PASS_EXPIRY_SOON -> findPassExpiryRecipient(outbox, now);
            case PICKUP_DEADLINE_REMINDER -> findPickupDeadlineRecipient(outbox, now);
            case BOOKING_CONFIRMED,
                    BOOKING_RESCHEDULED,
                    BOOKING_CANCELED,
                    DEPOSIT_REFUNDED,
                    ORDER_PAID,
                    ORDER_APPROVED,
                    ORDER_PICKUP_READY,
                    ORDER_SHIPPED,
                    ORDER_DELAY_REQUESTED,
                    ORDER_REFUNDED,
                    ORDER_CLAIM_RESOLVED,
                    ORDER_EXCHANGE_COMPLETED,
                    PASS_PURCHASED,
                    PASS_REFUNDED,
                    INQUIRY_ANSWERED,
                    PRODUCT_QNA_ANSWERED -> throw new IllegalArgumentException(
                            "시간 의존 리마인드만 현재 적격성을 조회할 수 있습니다.");
        };
    }

    private Optional<NotificationReminderRecipient> findD1BookingRecipient(
            NotificationOutbox outbox, LocalDateTime now) {
        if (!hasAggregate(outbox, BOOKING)) {
            return Optional.empty();
        }
        LocalDateTime tomorrowStart = now.toLocalDate().plusDays(1).atStartOfDay();
        return eligibilityPort.findD1BookingRecipient(
                outbox.getAggregateId(), tomorrowStart, tomorrowStart.plusDays(1));
    }

    private Optional<NotificationReminderRecipient> findSameDayBookingRecipient(
            NotificationOutbox outbox, LocalDateTime now) {
        if (now.toLocalTime().isBefore(SAME_DAY_REMINDER_START)
                || !hasAggregate(outbox, BOOKING)) {
            return Optional.empty();
        }
        return eligibilityPort.findSameDayBookingRecipient(
                outbox.getAggregateId(), now, now.toLocalDate().plusDays(1).atStartOfDay());
    }

    private Optional<NotificationReminderRecipient> findPassExpiryRecipient(
            NotificationOutbox outbox, LocalDateTime now) {
        return hasAggregate(outbox, PASS_PURCHASE)
                ? eligibilityPort.findPassExpiryRecipient(
                        outbox.getAggregateId(), now, now.plusDays(7))
                : Optional.empty();
    }

    private Optional<NotificationReminderRecipient> findPickupDeadlineRecipient(
            NotificationOutbox outbox, LocalDateTime now) {
        return hasAggregate(outbox, ORDER)
                ? eligibilityPort.findPickupDeadlineRecipient(
                        outbox.getAggregateId(), now, now.plusHours(2))
                : Optional.empty();
    }

    private boolean hasAggregate(NotificationOutbox outbox, String aggregateType) {
        return aggregateType.equals(outbox.getAggregateType()) && outbox.getAggregateId() != null;
    }
}

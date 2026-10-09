package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.application.notification.port.out.ReviewNotificationEligibilityPort;
import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class ReviewNotificationEligibility {

    private final ReviewNotificationEligibilityPort eligibilityPort;

    ReviewNotificationEligibility(ReviewNotificationEligibilityPort eligibilityPort) {
        this.eligibilityPort = eligibilityPort;
    }

    Optional<NotificationReminderRecipient> findEligibleRecipient(NotificationOutbox outbox) {
        return switch (outbox.getEventType()) {
            case REVIEW_REQUEST -> findRequestRecipient(outbox);
            case REVIEW_HIDDEN -> hasAggregate(outbox, NotificationAggregateType.REVIEW_MODERATION_ACTION)
                    ? eligibilityPort.findHiddenReviewRecipient(outbox.getAggregateId())
                    : Optional.empty();
            case REVIEW_REPUBLISHED -> hasAggregate(outbox, NotificationAggregateType.REVIEW_MODERATION_ACTION)
                    ? eligibilityPort.findRepublishedReviewRecipient(outbox.getAggregateId())
                    : Optional.empty();
            case REVIEW_OWNER_REPLIED -> hasAggregate(outbox, NotificationAggregateType.REVIEW)
                    ? eligibilityPort.findOwnerRepliedReviewRecipient(outbox.getAggregateId())
                    : Optional.empty();
            case BOOKING_CONFIRMED,
                    BOOKING_RESCHEDULED,
                    BOOKING_CANCELED,
                    BOOKING_VACANCY_AVAILABLE,
                    PRODUCT_RESTOCK_AVAILABLE,
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
                    PRODUCT_QNA_ANSWERED,
                    REMINDER_D1,
                    REMINDER_SAME_DAY,
                    PASS_EXPIRY_SOON,
                    PICKUP_DEADLINE_REMINDER -> throw new IllegalArgumentException(
                            "후기 알림만 현재 적격성을 조회할 수 있습니다.");
        };
    }

    private Optional<NotificationReminderRecipient> findRequestRecipient(NotificationOutbox outbox) {
        if (hasAggregate(outbox, NotificationAggregateType.ORDER)) {
            return eligibilityPort.findOrderRequestRecipient(outbox.getAggregateId());
        }
        if (hasAggregate(outbox, NotificationAggregateType.BOOKING)) {
            return eligibilityPort.findBookingRequestRecipient(outbox.getAggregateId());
        }
        return Optional.empty();
    }

    private boolean hasAggregate(NotificationOutbox outbox, NotificationAggregateType aggregateType) {
        return outbox.getAggregateType() == aggregateType && outbox.getAggregateId() != null;
    }
}

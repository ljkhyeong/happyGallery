package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.application.notification.port.out.ReviewNotificationEligibilityPort;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcReviewNotificationEligibilityAdapter implements ReviewNotificationEligibilityPort {

    private final JdbcClient jdbc;

    JdbcReviewNotificationEligibilityAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<NotificationReminderRecipient> findOrderRequestRecipient(Long orderId) {
        return jdbc.sql("""
                        SELECT o.user_id
                        FROM orders o
                        WHERE o.id = :orderId
                          AND o.user_id IS NOT NULL
                          AND o.status IN ('DELIVERED', 'PICKED_UP', 'COMPLETED')
                          AND EXISTS (
                              SELECT 1
                              FROM order_items oi
                              WHERE oi.order_id = o.id
                                AND NOT EXISTS (
                                    SELECT 1
                                    FROM reviews r
                                    WHERE r.order_item_id = oi.id
                                      AND (r.deleted_at IS NULL
                                           OR r.recreation_blocked = TRUE)
                                )
                          )
                        """)
                .param("orderId", orderId)
                .query(Long.class)
                .optional()
                .map(NotificationReminderRecipient::forUser);
    }

    @Override
    public Optional<NotificationReminderRecipient> findBookingRequestRecipient(Long bookingId) {
        return jdbc.sql("""
                        SELECT b.user_id
                        FROM bookings b
                        WHERE b.id = :bookingId
                          AND b.user_id IS NOT NULL
                          AND b.status = 'COMPLETED'
                          AND NOT EXISTS (
                              SELECT 1
                              FROM reviews r
                              WHERE r.booking_id = b.id
                                AND (r.deleted_at IS NULL
                                     OR r.recreation_blocked = TRUE)
                          )
                        """)
                .param("bookingId", bookingId)
                .query(Long.class)
                .optional()
                .map(NotificationReminderRecipient::forUser);
    }

    @Override
    public Optional<NotificationReminderRecipient> findHiddenReviewRecipient(Long moderationActionId) {
        return findModerationRecipient(moderationActionId, "HIDE", "HIDDEN");
    }

    @Override
    public Optional<NotificationReminderRecipient> findRepublishedReviewRecipient(Long moderationActionId) {
        return findModerationRecipient(moderationActionId, "REPUBLISH", "PUBLISHED");
    }

    @Override
    public Optional<NotificationReminderRecipient> findOwnerRepliedReviewRecipient(Long reviewId) {
        return jdbc.sql("""
                        SELECT user_id
                        FROM reviews
                        WHERE id = :reviewId
                          AND deleted_at IS NULL
                          AND reply_content IS NOT NULL
                          AND TRIM(reply_content) <> ''
                        """)
                .param("reviewId", reviewId)
                .query(Long.class)
                .optional()
                .map(NotificationReminderRecipient::forUser);
    }

    private Optional<NotificationReminderRecipient> findModerationRecipient(
            Long moderationActionId, String action, String currentStatus) {
        return jdbc.sql("""
                        SELECT r.user_id
                        FROM review_moderation_actions a
                        JOIN reviews r ON r.id = a.review_id
                        WHERE a.id = :moderationActionId
                          AND a.action = :action
                          AND r.deleted_at IS NULL
                          AND r.status = :currentStatus
                          AND a.id = (
                              SELECT MAX(latest.id)
                              FROM review_moderation_actions latest
                              WHERE latest.review_id = r.id
                          )
                        """)
                .param("moderationActionId", moderationActionId)
                .param("action", action)
                .param("currentStatus", currentStatus)
                .query(Long.class)
                .optional()
                .map(NotificationReminderRecipient::forUser);
    }
}

package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationReminderEligibilityPort;
import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcNotificationReminderEligibilityAdapter implements NotificationReminderEligibilityPort {

    private final JdbcClient jdbc;

    JdbcNotificationReminderEligibilityAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<NotificationReminderRecipient> findD1BookingRecipient(
            Long bookingId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return jdbc.sql("""
                        SELECT b.user_id, b.guest_id
                        FROM bookings b
                        JOIN slots s ON s.id = b.slot_id
                        WHERE b.id = :bookingId
                          AND b.status = 'BOOKED'
                          AND s.start_at >= :startInclusive
                          AND s.start_at < :endExclusive
                          AND ((b.user_id IS NOT NULL AND b.guest_id IS NULL)
                               OR (b.user_id IS NULL AND b.guest_id IS NOT NULL))
                        """)
                .param("bookingId", bookingId)
                .param("startInclusive", startInclusive)
                .param("endExclusive", endExclusive)
                .query(RecipientIds.class)
                .optional()
                .map(RecipientIds::toRecipient);
    }

    @Override
    public Optional<NotificationReminderRecipient> findSameDayBookingRecipient(
            Long bookingId, LocalDateTime startExclusive, LocalDateTime endExclusive) {
        return jdbc.sql("""
                        SELECT b.user_id, b.guest_id
                        FROM bookings b
                        JOIN slots s ON s.id = b.slot_id
                        WHERE b.id = :bookingId
                          AND b.status = 'BOOKED'
                          AND s.start_at > :startExclusive
                          AND s.start_at < :endExclusive
                          AND ((b.user_id IS NOT NULL AND b.guest_id IS NULL)
                               OR (b.user_id IS NULL AND b.guest_id IS NOT NULL))
                        """)
                .param("bookingId", bookingId)
                .param("startExclusive", startExclusive)
                .param("endExclusive", endExclusive)
                .query(RecipientIds.class)
                .optional()
                .map(RecipientIds::toRecipient);
    }

    @Override
    public Optional<NotificationReminderRecipient> findPassExpiryRecipient(
            Long passId, LocalDateTime nowExclusive, LocalDateTime latestExpiryInclusive) {
        return jdbc.sql("""
                        SELECT p.user_id
                        FROM pass_purchases p
                        WHERE p.id = :passId
                          AND p.expires_at > :nowExclusive
                          AND p.expires_at <= :latestExpiryInclusive
                          AND p.remaining_credits > 0
                          AND p.user_id IS NOT NULL
                        """)
                .param("passId", passId)
                .param("nowExclusive", nowExclusive)
                .param("latestExpiryInclusive", latestExpiryInclusive)
                .query(Long.class)
                .optional()
                .map(NotificationReminderRecipient::forUser);
    }

    @Override
    public Optional<NotificationReminderRecipient> findPickupDeadlineRecipient(
            Long orderId, LocalDateTime nowExclusive, LocalDateTime latestDeadlineInclusive) {
        return jdbc.sql("""
                        SELECT o.user_id, o.guest_id
                        FROM fulfillments f
                        JOIN orders o ON o.id = f.order_id
                        WHERE o.id = :orderId
                          AND o.status = 'PICKUP_READY'
                          AND f.pickup_deadline_at > :nowExclusive
                          AND f.pickup_deadline_at <= :latestDeadlineInclusive
                          AND ((o.user_id IS NOT NULL AND o.guest_id IS NULL)
                               OR (o.user_id IS NULL AND o.guest_id IS NOT NULL))
                        """)
                .param("orderId", orderId)
                .param("nowExclusive", nowExclusive)
                .param("latestDeadlineInclusive", latestDeadlineInclusive)
                .query(RecipientIds.class)
                .optional()
                .map(RecipientIds::toRecipient);
    }

    private record RecipientIds(Long userId, Long guestId) {

        NotificationReminderRecipient toRecipient() {
            return userId != null
                    ? NotificationReminderRecipient.forUser(userId)
                    : NotificationReminderRecipient.forGuest(guestId);
        }
    }
}

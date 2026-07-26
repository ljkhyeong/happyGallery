package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingReminderCandidatePort;
import com.personal.happygallery.application.booking.port.out.BookingReminderTarget;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import static com.personal.happygallery.domain.notification.NotificationRequestedEvent.oncePerAggregateKeyPrefix;

@Repository
class JdbcBookingReminderCandidateAdapter implements BookingReminderCandidatePort {

    private static final String AGGREGATE_TYPE = "BOOKING";
    private final JdbcClient jdbc;

    JdbcBookingReminderCandidateAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<BookingReminderTarget> findUnnotifiedBookedAfterId(
            LocalDateTime start,
            LocalDateTime end,
            NotificationEventType eventType,
            Long afterId,
            int limit) {
        return jdbc.sql("""
                        SELECT b.id AS booking_id, b.user_id, b.guest_id
                        FROM bookings b
                        JOIN slots s ON s.id = b.slot_id
                        WHERE b.status = 'BOOKED'
                          AND s.start_at >= :start
                          AND s.start_at < :end
                          AND b.id > :afterId
                          AND NOT EXISTS (
                              SELECT 1
                              FROM notification_outbox n
                              WHERE n.idempotency_key = CONCAT(:idempotencyPrefix, b.id)
                          )
                        ORDER BY b.id
                        LIMIT :limit
                        """)
                .param("start", start)
                .param("end", end)
                .param("afterId", afterId)
                .param("idempotencyPrefix",
                        oncePerAggregateKeyPrefix(eventType, AGGREGATE_TYPE))
                .param("limit", limit)
                .query((rs, rowNum) -> new BookingReminderTarget(
                        rs.getLong("booking_id"),
                        rs.getObject("user_id", Long.class),
                        rs.getObject("guest_id", Long.class)))
                .list();
    }
}

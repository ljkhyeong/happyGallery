package com.personal.happygallery.adapter.out.persistence.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxInsertPort;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcNotificationOutboxInsertAdapter implements NotificationOutboxInsertPort {

    private final JdbcClient jdbc;

    JdbcNotificationOutboxInsertAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(NotificationOutbox outbox) {
        try {
            jdbc.sql("""
                            INSERT INTO notification_outbox (
                                recipient_type, guest_id, user_id, event_type,
                                aggregate_type, aggregate_id, idempotency_key,
                                status, attempt_count, next_attempt_at
                            ) VALUES (
                                :recipientType, :guestId, :userId, :eventType,
                                :aggregateType, :aggregateId, :idempotencyKey,
                                :status, :attemptCount, :nextAttemptAt
                            )
                            """)
                    .param("recipientType", outbox.getRecipientType().name())
                    .param("guestId", outbox.getGuestId())
                    .param("userId", outbox.getUserId())
                    .param("eventType", outbox.getEventType().name())
                    .param("aggregateType", outbox.getAggregateType())
                    .param("aggregateId", outbox.getAggregateId())
                    .param("idempotencyKey", outbox.getIdempotencyKey())
                    .param("status", outbox.getStatus().name())
                    .param("attemptCount", outbox.getAttemptCount())
                    .param("nextAttemptAt", outbox.getNextAttemptAt())
                    .update();
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}

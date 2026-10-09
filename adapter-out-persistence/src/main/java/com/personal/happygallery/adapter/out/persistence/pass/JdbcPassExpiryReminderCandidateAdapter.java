package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassExpiryReminderCandidatePort;
import com.personal.happygallery.application.pass.port.out.PassExpiryReminderTarget;
import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPassExpiryReminderCandidateAdapter implements PassExpiryReminderCandidatePort {

    private final JdbcClient jdbc;

    JdbcPassExpiryReminderCandidateAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PassExpiryReminderTarget> findUnnotifiedExpiringAfterId(
            LocalDateTime now,
            LocalDateTime latestExpiry,
            Long afterId,
            int limit) {
        return jdbc.sql("""
                        SELECT p.id AS pass_id, p.user_id
                        FROM pass_purchases p
                        WHERE p.expires_at > :now
                          AND p.expires_at <= :latestExpiry
                          AND p.remaining_credits > 0
                          AND p.id > :afterId
                          AND NOT EXISTS (
                              SELECT 1
                              FROM notification_outbox n
                              WHERE n.event_type = :eventType
                                AND n.aggregate_type = :aggregateType
                                AND n.aggregate_id = p.id
                                AND n.status <> 'OBSOLETE'
                          )
                        ORDER BY p.id
                        LIMIT :limit
                        """)
                .param("now", now)
                .param("latestExpiry", latestExpiry)
                .param("afterId", afterId)
                .param("eventType", NotificationEventType.PASS_EXPIRY_SOON.name())
                .param("aggregateType", NotificationAggregateType.PASS_PURCHASE.name())
                .param("limit", limit)
                .query(PassExpiryReminderTarget.class)
                .list();
    }
}

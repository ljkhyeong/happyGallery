package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassExpiryReminderCandidatePort;
import com.personal.happygallery.application.pass.port.out.PassExpiryReminderTarget;
import com.personal.happygallery.domain.notification.NotificationEventType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPassExpiryReminderCandidateAdapter implements PassExpiryReminderCandidatePort {

    private static final String AGGREGATE_TYPE = "PASS_PURCHASE";
    private final JdbcClient jdbc;

    JdbcPassExpiryReminderCandidateAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PassExpiryReminderTarget> findUnnotifiedExpiringAfterId(
            LocalDateTime now,
            LocalDateTime latestExpiry,
            int minimumCredits,
            Long afterId,
            int limit) {
        return jdbc.sql("""
                        SELECT p.id AS pass_id, p.user_id
                        FROM pass_purchases p
                        WHERE p.expires_at > :now
                          AND p.expires_at <= :latestExpiry
                          AND p.remaining_credits > :minimumCredits
                          AND p.user_id IS NOT NULL
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
                .param("minimumCredits", minimumCredits)
                .param("afterId", afterId)
                .param("eventType", NotificationEventType.PASS_EXPIRY_SOON.name())
                .param("aggregateType", AGGREGATE_TYPE)
                .param("limit", limit)
                .query(PassExpiryReminderTarget.class)
                .list();
    }
}

package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.RestockAlertDeliveryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class RestockAlertDeliveryRepository implements RestockAlertDeliveryPort {
    private static final String AVAILABLE = """
            p.status = 'ACTIVE' AND u.withdrawn_at IS NULL AND u.phone_verified = TRUE
            AND ((p.type = 'READY_STOCK' AND a.product_variant_id IS NULL AND i.quantity > 0)
                OR (p.type = 'MADE_TO_ORDER' AND v.active = TRUE AND v.quantity > 0))
            """;
    private static final String SOURCE = """
            FROM product_restock_alerts a
            JOIN users u ON u.id = a.user_id
            JOIN products p ON p.id = a.product_id
            LEFT JOIN inventory i ON i.product_id = p.id
            LEFT JOIN product_variants v ON v.id = a.product_variant_id AND v.product_id = p.id
            """;
    private static final String OUTBOX = """
            o.event_type = 'PRODUCT_RESTOCK_AVAILABLE' AND o.aggregate_type = 'RESTOCK_ALERT' AND o.aggregate_id = a.id
            """;
    private final JdbcClient jdbc;

    RestockAlertDeliveryRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Long> findCandidateIds(long afterId, int limit) {
        return jdbc.sql("SELECT a.id " + SOURCE + """
                WHERE a.id > :afterId AND a.status IN ('WAITING', 'QUEUED')
                AND (EXISTS (SELECT 1 FROM notification_outbox o WHERE
                """ + OUTBOX + " AND o.status = 'SENT') OR (" + AVAILABLE + """
                AND NOT EXISTS (SELECT 1 FROM notification_outbox o WHERE
                """ + OUTBOX + " AND o.status <> 'OBSOLETE'))) ORDER BY a.id LIMIT :limit")
                .param("afterId", afterId).param("limit", limit).query(Long.class).list();
    }

    @Override
    public Optional<Long> findEligibleUserId(Long alertId) {
        return jdbc.sql("SELECT a.user_id " + SOURCE
                        + " WHERE a.id = :alertId AND a.status IN ('WAITING', 'QUEUED') AND " + AVAILABLE)
                .param("alertId", alertId).query(Long.class).optional();
    }

    @Override
    public Optional<LocalDateTime> findSentAt(Long alertId) {
        return jdbc.sql("""
                SELECT processed_at FROM notification_outbox
                WHERE event_type = 'PRODUCT_RESTOCK_AVAILABLE' AND aggregate_type = 'RESTOCK_ALERT'
                  AND aggregate_id = :alertId AND status = 'SENT'
                """)
                .param("alertId", alertId).query(LocalDateTime.class).optional();
    }
}

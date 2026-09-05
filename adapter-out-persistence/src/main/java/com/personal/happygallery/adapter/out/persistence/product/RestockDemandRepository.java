package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.in.RestockDemandUseCase.Demand;
import com.personal.happygallery.application.product.port.out.RestockDemandPort;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class RestockDemandRepository implements RestockDemandPort {
    private static final String GROUPED = """
            FROM product_restock_alerts a
            JOIN products p ON p.id = a.product_id
            JOIN users u ON u.id = a.user_id
            LEFT JOIN product_variants v ON v.id = a.product_variant_id AND v.product_id = p.id
            WHERE a.status IN ('WAITING', 'QUEUED') AND u.withdrawn_at IS NULL AND u.phone_verified = TRUE
              AND p.status = 'ACTIVE' AND (:productId IS NULL OR p.id = :productId)
              AND ((p.type = 'READY_STOCK' AND a.product_variant_id IS NULL) OR (p.type = 'MADE_TO_ORDER' AND v.active = TRUE))
              AND NOT EXISTS (SELECT 1 FROM notification_outbox o WHERE o.event_type = 'PRODUCT_RESTOCK_AVAILABLE'
                  AND o.aggregate_type = 'RESTOCK_ALERT' AND o.aggregate_id = a.id AND o.status = 'SENT')
            GROUP BY p.id, p.name, a.product_variant_id
            """;
    private final JdbcClient jdbc;
    RestockDemandRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Demand> list(Long productId, int offset, int size) {
        return jdbc.sql("""
                SELECT p.id AS product_id, p.name AS product_name, a.product_variant_id,
                       MAX(a.option_label) AS option_label, COUNT(DISTINCT a.user_id) AS waiting_count
                """ + GROUPED + " ORDER BY waiting_count DESC, p.id, a.product_variant_id LIMIT :size OFFSET :offset")
                .param("productId", productId).param("size", size).param("offset", offset).query(Demand.class).list();
    }

    @Override
    public long count(Long productId) {
        return jdbc.sql("SELECT COUNT(*) FROM (SELECT p.id " + GROUPED + ") demand_groups")
                .param("productId", productId).query(Long.class).single();
    }
}

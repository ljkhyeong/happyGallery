package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcSmartStoreStockSyncQueueAdapter implements SmartStoreStockSyncQueuePort {

    private static final String REQUEST_SQL = """
            insert into smartstore_stock_syncs (
                product_id, request_version, status, attempt_count, next_attempt_at, row_version
            )
            select ?, 1, 'PENDING', 0, ?, 0
             where exists (
                select 1
                  from smartstore_stock_mappings
                 where product_id = ?
                   and enabled = true
             )
            on duplicate key update
                request_version = request_version + 1,
                status = 'PENDING',
                attempt_count = 0,
                next_attempt_at = values(next_attempt_at),
                processing_started_at = null,
                last_error = null,
                row_version = row_version + 1
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSmartStoreStockSyncQueueAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void requestIfMapped(Collection<Long> productIds, LocalDateTime now) {
        Timestamp requestedAt = Timestamp.valueOf(now);
        productIds.stream().distinct().forEach(productId -> jdbcTemplate.update(
                REQUEST_SQL, productId, requestedAt, productId));
    }
}

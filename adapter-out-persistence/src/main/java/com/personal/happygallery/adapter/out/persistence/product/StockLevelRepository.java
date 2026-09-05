package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.StockLevel;
import com.personal.happygallery.application.product.port.out.StockLevelReaderPort;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class StockLevelRepository implements StockLevelReaderPort {
    private final JdbcClient jdbc;

    StockLevelRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public List<StockLevel> findStockLevels(Long productId) {
        return jdbc.sql("""
                SELECT p.id AS product_id, NULL AS product_variant_id, p.name AS product_name, p.type,
                       i.quantity, i.minimum_stock, i.version, (p.status = 'ACTIVE') AS active
                FROM products p JOIN inventory i ON i.product_id = p.id
                WHERE p.type = 'READY_STOCK' AND (:productId IS NULL OR p.id = :productId)
                UNION ALL
                SELECT p.id, v.id, p.name, p.type, v.quantity, v.minimum_stock, v.version,
                       (p.status = 'ACTIVE' AND v.active = TRUE)
                FROM products p JOIN product_variants v ON v.product_id = p.id
                WHERE p.type = 'MADE_TO_ORDER' AND (:productId IS NULL OR p.id = :productId)
                ORDER BY product_id, product_variant_id
                """)
                .param("productId", productId)
                .query((row, index) -> new StockLevel(row.getLong("product_id"),
                        row.getObject("product_variant_id", Long.class), row.getString("product_name"),
                        ProductType.valueOf(row.getString("type")), row.getInt("quantity"),
                        row.getObject("minimum_stock", Integer.class), row.getLong("version"), row.getBoolean("active")))
                .list();
    }
}

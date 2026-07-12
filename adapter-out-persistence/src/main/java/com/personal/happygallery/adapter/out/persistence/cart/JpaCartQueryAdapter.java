package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartQueryPort;
import com.personal.happygallery.domain.product.ProductStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCartQueryAdapter implements CartQueryPort {

    private final EntityManager entityManager;

    public JpaCartQueryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<CartItemDetail> findDetailsByUserId(Long userId) {
        return entityManager.createQuery("""
                        SELECT product.id AS productId,
                               product.name AS productName,
                               product.price AS price,
                               item.qty AS qty,
                               product.status AS productStatus,
                               inventory.quantity AS inventoryQuantity
                        FROM CartItem item
                        JOIN Product product ON product.id = item.productId
                        LEFT JOIN Inventory inventory ON inventory.productId = item.productId
                        WHERE item.userId = :userId
                        ORDER BY item.createdAt, item.id
                        """, Tuple.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .map(this::toDetail)
                .toList();
    }

    private CartItemDetail toDetail(Tuple row) {
        return new CartItemDetail(
                row.get("productId", Long.class),
                row.get("productName", String.class),
                row.get("price", Long.class),
                row.get("qty", Integer.class),
                row.get("productStatus", ProductStatus.class),
                row.get("inventoryQuantity", Integer.class));
    }
}

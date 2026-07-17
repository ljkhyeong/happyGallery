package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemPort {

    @Override OrderItem save(OrderItem item);

    @Override List<OrderItem> findByOrder(Order order);

    /** 주문 내 제작 상품 존재 여부 — N+1 방지용 단일 쿼리 */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END
            FROM OrderItem oi
            JOIN Product p ON oi.productId = p.id
            WHERE oi.order = :order
              AND p.type = com.personal.happygallery.domain.product.ProductType.MADE_TO_ORDER
            """)
    boolean existsMadeToOrderItem(@Param("order") Order order);
}

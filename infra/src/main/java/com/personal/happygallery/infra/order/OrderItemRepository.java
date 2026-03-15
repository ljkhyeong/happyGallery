package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    /** 주문 내 특정 상품 타입 존재 여부 — N+1 방지용 단일 쿼리 */
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END "
         + "FROM OrderItem oi JOIN Product p ON oi.productId = p.id "
         + "WHERE oi.order = :order AND p.type = :type")
    boolean existsByOrderAndProductType(@Param("order") Order order,
                                        @Param("type") ProductType type);
}

package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemPort {

    @Override
    <S extends OrderItem> S save(S item);

    @Override
    List<OrderItem> findByOrder(Order order);

    @Override
    List<OrderItem> findByIdIn(Collection<Long> ids);

    @Override
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id IN :orderIds ORDER BY oi.id")
    List<OrderItem> findByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);

    /** 결제 당시 주문제작 상품 포함 여부. 구주문은 주문제작 동의 스냅샷으로 보완한다. */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END
            FROM OrderItem oi
            WHERE oi.order = :order
              AND (
                  oi.productType = com.personal.happygallery.domain.product.ProductType.MADE_TO_ORDER
                  OR (
                      oi.productType IS NULL
                      AND oi.order.madeToOrderConsentAt IS NOT NULL
                  )
              )
            """)
    boolean existsMadeToOrderItem(@Param("order") Order order);
}

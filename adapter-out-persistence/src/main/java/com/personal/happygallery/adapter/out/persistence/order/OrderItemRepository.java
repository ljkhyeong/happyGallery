package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.ProductType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemPort {

    @Override
    <S extends OrderItem> S save(S item);

    @Override
    <S extends OrderItem> List<S> saveAll(Iterable<S> items);

    @Override
    @Query("""
            SELECT DISTINCT oi
            FROM OrderItem oi
            LEFT JOIN FETCH oi.optionSnapshots
            WHERE oi.order = :order
            ORDER BY oi.id
            """)
    List<OrderItem> findByOrder(@Param("order") Order order);

    @Override
    @Query("""
            SELECT DISTINCT oi
            FROM OrderItem oi
            LEFT JOIN FETCH oi.optionSnapshots
            WHERE oi.id IN :ids
            ORDER BY oi.id
            """)
    List<OrderItem> findByIdIn(@Param("ids") Collection<Long> ids);

    @Override
    @Query("""
            SELECT DISTINCT oi
            FROM OrderItem oi
            LEFT JOIN FETCH oi.optionSnapshots
            WHERE oi.order.id IN :orderIds
            ORDER BY oi.id
            """)
    List<OrderItem> findByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);

    /** 결제 당시 주문제작 상품 포함 여부. */
    @Override
    default boolean existsMadeToOrderItem(Order order) {
        return existsByOrderAndProductType(order, ProductType.MADE_TO_ORDER);
    }

    boolean existsByOrderAndProductType(Order order, ProductType productType);
}

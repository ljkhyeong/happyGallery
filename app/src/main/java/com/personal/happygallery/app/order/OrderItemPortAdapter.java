package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.OrderItemPort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.infra.order.OrderItemRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link OrderItemRepository}(infra) → {@link OrderItemPort}(app) 브릿지 어댑터.
 */
@Component
class OrderItemPortAdapter implements OrderItemPort {

    private final OrderItemRepository orderItemRepository;

    OrderItemPortAdapter(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public OrderItem save(OrderItem item) {
        return orderItemRepository.save(item);
    }

    @Override
    public List<OrderItem> findByOrder(Order order) {
        return orderItemRepository.findByOrder(order);
    }

    @Override
    public boolean existsByOrderAndProductType(Order order, ProductType type) {
        return orderItemRepository.existsByOrderAndProductType(order, type);
    }
}

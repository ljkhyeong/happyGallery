package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaOrderItemPersistenceAdapter implements OrderItemPort {

    private final OrderItemRepository repository;

    JpaOrderItemPersistenceAdapter(OrderItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderItem save(OrderItem item) {
        return repository.save(item);
    }

    @Override
    public List<OrderItem> findByOrder(Order order) {
        return repository.findByOrder(order);
    }

    @Override
    public List<OrderItem> findByOrderIdIn(Collection<Long> orderIds) {
        return repository.findByOrderIdIn(orderIds);
    }

    @Override
    public List<OrderItem> findByIdIn(Collection<Long> ids) {
        return repository.findByIdIn(ids);
    }

    @Override
    public boolean existsMadeToOrderItem(Order order) {
        return repository.existsMadeToOrderItem(order);
    }
}

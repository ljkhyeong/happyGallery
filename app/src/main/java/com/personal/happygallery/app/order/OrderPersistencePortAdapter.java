package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.out.OrderReaderPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.order.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link OrderRepository}(infra) → {@link OrderReaderPort} + {@link OrderStorePort}(app) 브릿지 어댑터.
 */
@Component
class OrderPersistencePortAdapter implements OrderReaderPort, OrderStorePort {

    private final OrderRepository orderRepository;

    OrderPersistencePortAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    public List<Order> findByStatusAndApprovalDeadlineAtBefore(OrderStatus status, LocalDateTime deadline) {
        return orderRepository.findByStatusAndApprovalDeadlineAtBefore(status, deadline);
    }

    @Override
    public List<Order> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public List<Order> findAllByOrderByCreatedAtDesc() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order saveAndFlush(Order order) {
        return orderRepository.saveAndFlush(order);
    }
}

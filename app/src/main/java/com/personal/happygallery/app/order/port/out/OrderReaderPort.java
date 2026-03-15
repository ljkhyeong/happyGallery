package com.personal.happygallery.app.order.port.out;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderReaderPort {
    Optional<Order> findById(Long id);
    List<Order> findByStatusAndApprovalDeadlineAtBefore(OrderStatus status, LocalDateTime deadline);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();
}

package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.Collection;
import java.util.List;

public interface OrderItemPort {
    OrderItem save(OrderItem item);
    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);
    boolean existsMadeToOrderItem(Order order);
}

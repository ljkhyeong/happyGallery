package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import java.util.Collection;
import java.util.List;

public interface OrderItemPort {
    <S extends OrderItem> S save(S item);
    <S extends OrderItem> List<S> saveAll(Iterable<S> items);
    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);
    List<OrderItem> findByIdIn(Collection<Long> ids);
    boolean existsMadeToOrderItem(Order order);
}

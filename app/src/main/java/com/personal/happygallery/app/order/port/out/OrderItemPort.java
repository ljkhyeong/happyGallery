package com.personal.happygallery.app.order.port.out;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;

public interface OrderItemPort {
    OrderItem save(OrderItem item);
    List<OrderItem> findByOrder(Order order);
    boolean existsByOrderAndProductType(Order order, ProductType type);
}

package com.personal.happygallery.app.order.port.out;

import com.personal.happygallery.domain.order.Order;

public interface OrderStorePort {
    Order save(Order order);
    Order saveAndFlush(Order order);
}

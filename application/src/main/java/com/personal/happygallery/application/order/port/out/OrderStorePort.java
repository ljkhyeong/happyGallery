package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.Order;

public interface OrderStorePort {
    <S extends Order> S save(S order);
    <S extends Order> S saveAndFlush(S order);
}

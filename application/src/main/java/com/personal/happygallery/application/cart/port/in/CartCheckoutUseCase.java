package com.personal.happygallery.application.cart.port.in;

import com.personal.happygallery.domain.order.Order;

public interface CartCheckoutUseCase {

    Order checkout(Long userId);
}

package com.personal.happygallery.app.cart.port.in;

import com.personal.happygallery.domain.order.Order;

public interface CartCheckoutUseCase {

    Order checkout(Long userId);
}

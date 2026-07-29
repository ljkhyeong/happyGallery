package com.personal.happygallery.application.cart.port.out;

import java.util.List;

public interface CartReadModelPort {

    List<CartItemDetail> findDetailsByUserId(Long userId);
}

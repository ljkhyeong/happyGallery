package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.cart.CartItem;
import java.util.Optional;

public interface CartItemReaderPort {

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
}

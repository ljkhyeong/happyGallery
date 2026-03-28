package com.personal.happygallery.app.cart.port.out;

import com.personal.happygallery.domain.cart.CartItem;
import java.util.List;
import java.util.Optional;

public interface CartItemReaderPort {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
}

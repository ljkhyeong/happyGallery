package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.cart.CartItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CartItemReaderPort {

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    Optional<CartItem> findByUserIdAndProductIdForUpdate(Long userId, Long productId);

    List<CartItem> findAllByUserIdAndProductIdInForUpdate(Long userId, Collection<Long> productIds);

    List<CartItem> findAllByUserIdAndIdInOrderByIdAsc(Long userId, Collection<Long> cartItemIds);
}

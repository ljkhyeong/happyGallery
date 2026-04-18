package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.cart.CartItem;

public interface CartItemStorePort {

    CartItem save(CartItem item);

    void delete(CartItem item);

    void deleteAllByUserId(Long userId);
}

package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.cart.CartItem;

public interface CartItemStorePort {

    <S extends CartItem> S save(S item);

    void delete(CartItem item);
}

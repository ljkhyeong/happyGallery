package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.cart.CartItem;
import java.util.List;

public interface CartItemStorePort {

    <S extends CartItem> S save(S item);

    <S extends CartItem> List<S> saveAll(Iterable<S> items);

    void delete(CartItem item);
}

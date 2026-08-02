package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.domain.cart.CartItem;
import org.springframework.stereotype.Repository;

@Repository
class JpaCartItemPersistenceAdapter implements CartItemStorePort {

    private final CartItemRepository repository;

    JpaCartItemPersistenceAdapter(CartItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public CartItem save(CartItem item) {
        return repository.save(item);
    }

    @Override
    public void delete(CartItem item) {
        repository.delete(item);
    }
}

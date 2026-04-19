package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.domain.cart.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long>,
        CartItemReaderPort, CartItemStorePort {

    @Override
    List<CartItem> findByUserId(Long userId);

    @Override
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @Override
    void deleteAllByUserId(Long userId);
}

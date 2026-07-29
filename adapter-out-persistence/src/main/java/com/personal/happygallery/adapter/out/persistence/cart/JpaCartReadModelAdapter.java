package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartItemDetail;
import com.personal.happygallery.application.cart.port.out.CartReadModelPort;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCartReadModelAdapter implements CartReadModelPort {

    private final CartItemRepository cartItemRepository;

    public JpaCartReadModelAdapter(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public List<CartItemDetail> findDetailsByUserId(Long userId) {
        return cartItemRepository.findDetailsByUserId(userId);
    }
}

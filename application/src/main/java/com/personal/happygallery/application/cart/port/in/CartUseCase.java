package com.personal.happygallery.application.cart.port.in;

import java.util.List;

public interface CartUseCase {

    record CartItemView(Long productId, String productName, long price, int qty, boolean available) {
        public long subtotal() { return price * qty; }
    }

    record CartView(List<CartItemView> items, long totalAmount) {}

    CartView getCart(Long userId);

    void addItem(Long userId, Long productId, int qty);

    void updateItemQty(Long userId, Long productId, int qty);

    void removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}

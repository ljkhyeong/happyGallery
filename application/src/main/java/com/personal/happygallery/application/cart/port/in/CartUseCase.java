package com.personal.happygallery.application.cart.port.in;

import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import java.util.UUID;

public interface CartUseCase {

    record CartItemView(Long productId, String productName, ProductType productType,
                        long price, int qty, boolean available) {
        public long subtotal() { return OrderAmountCalculator.addLine(0L, qty, price); }
    }

    record CartView(List<CartItemView> items, long totalAmount) {}

    record CartPurchaseItem(Long cartItemId, Long productId, int qty) {}

    record PurchasedItem(Long cartItemId, int qty) {}

    record MergeItem(Long productId, int qty) {}

    CartView getCart(Long userId);

    List<CartPurchaseItem> getPurchasableItems(Long userId);

    void addItem(Long userId, Long productId, int qty);

    void mergeItems(Long userId, UUID idempotencyKey, List<MergeItem> items);

    void updateItemQty(Long userId, Long productId, int qty);

    void removeItem(Long userId, Long productId);

    void removePurchasedItems(Long userId, List<PurchasedItem> items);
}

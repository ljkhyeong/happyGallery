package com.personal.happygallery.application.cart.port.in;

import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import java.util.UUID;

public interface CartUseCase {

    record CartItemView(Long productId, String productName, ProductType productType,
                        long price, String specification, String careInstructions,
                        Integer productionLeadDays, int qty, boolean available) {
        public CartItemView(Long productId, String productName, ProductType productType,
                            long price, int qty, boolean available) {
            this(productId, productName, productType, price, null, null, null, qty, available);
        }

        public long subtotal() { return OrderAmountCalculator.addLine(0L, qty, price); }
    }

    record CartView(List<CartItemView> items, long totalAmount, String cartVersion) {}

    record CartPurchaseItem(Long cartItemId, Long productId, int qty) {}

    record PurchasableCart(List<CartPurchaseItem> items, String cartVersion) {}

    record PurchasedItem(Long cartItemId, int qty) {}

    record MergeItem(Long productId, int qty) {}

    CartView getCart(Long userId);

    PurchasableCart getPurchasableCart(Long userId);

    void addItem(Long userId, Long productId, int qty);

    void mergeItems(Long userId, UUID idempotencyKey, List<MergeItem> items);

    void updateItemQty(Long userId, Long productId, int qty);

    void removeItem(Long userId, Long productId);

    void removePurchasedItems(Long userId, List<PurchasedItem> items);
}

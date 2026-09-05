package com.personal.happygallery.application.cart.port.in;

import com.personal.happygallery.application.product.ProductOptions.OptionSnapshot;
import com.personal.happygallery.application.product.ProductOptions.TextInput;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import java.util.UUID;

public interface CartUseCase {

    record CartItemView(Long cartItemId, Long productId, Long productVariantId,
                        String productName, ProductType productType,
                        long basePrice, long variantPriceAdjustment,
                        long textOptionPriceAdjustment, long price,
                        String specification, String careInstructions,
                        Integer productionLeadDays, List<OptionSnapshot> options,
                        int qty, boolean available, int availableQuantity) {
        public CartItemView {
            options = List.copyOf(options);
        }

        public CartItemView(Long productId, String productName, ProductType productType,
                            long price, int qty, boolean available, int availableQuantity) {
            this(null, productId, null, productName, productType,
                    price, 0L, 0L, price, null, null, null, List.of(), qty, available, availableQuantity);
        }

        public long subtotal() { return OrderAmountCalculator.addLine(0L, qty, price); }
    }

    record CartView(List<CartItemView> items, long totalAmount, String cartVersion) {}

    record CartPurchaseItem(Long cartItemId, Long productId, Long productVariantId,
                            List<TextInput> textInputs, int qty) {
        public CartPurchaseItem {
            textInputs = List.copyOf(textInputs);
        }
    }

    record PurchasableCart(List<CartPurchaseItem> items, String cartVersion) {}

    record PurchasedItem(Long cartItemId, int qty) {}

    record MergeItem(Long productId, Long productVariantId,
                     List<TextInput> textInputs, int qty) {
        public MergeItem {
            textInputs = textInputs == null ? List.of() : List.copyOf(textInputs);
        }

        public MergeItem(Long productId, int qty) {
            this(productId, null, List.of(), qty);
        }
    }

    CartView getCart(Long userId);

    PurchasableCart getPurchasableCart(Long userId, List<Long> selectedCartItemIds);

    void addItem(Long userId, Long productId, Long productVariantId,
                 List<TextInput> textInputs, int qty);

    default void addItem(Long userId, Long productId, int qty) {
        addItem(userId, productId, null, List.of(), qty);
    }

    void mergeItems(Long userId, UUID idempotencyKey, List<MergeItem> items);

    void updateItemQty(Long userId, Long cartItemId, int qty);

    void removeItem(Long userId, Long cartItemId);

    void removePurchasedItems(Long userId, List<PurchasedItem> items);
}

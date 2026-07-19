package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartPurchaseItem;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasedItem;
import com.personal.happygallery.application.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.cart.port.out.CartReadModelPort;
import com.personal.happygallery.application.cart.port.out.CartReadModelPort.CartItemDetail;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.product.ProductStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Service
@Transactional
public class DefaultCartService implements CartUseCase {

    private final CartItemReaderPort cartItemReader;
    private final CartItemStorePort cartItemStore;
    private final CartReadModelPort cartReadModel;
    private final ProductReaderPort productReader;
    private final Clock clock;

    public DefaultCartService(CartItemReaderPort cartItemReader,
                              CartItemStorePort cartItemStore,
                              CartReadModelPort cartReadModel,
                              ProductReaderPort productReader,
                              Clock clock) {
        this.cartItemReader = cartItemReader;
        this.cartItemStore = cartItemStore;
        this.cartReadModel = cartReadModel;
        this.productReader = productReader;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView getCart(Long userId) {
        List<CartItemView> views = cartReadModel.findDetailsByUserId(userId).stream()
                .map(item -> new CartItemView(
                        item.productId(), item.productName(), item.price(), item.qty(), isAvailable(item)))
                .toList();

        long total = views.stream()
                .filter(CartItemView::available)
                .mapToLong(CartItemView::subtotal)
                .sum();
        return new CartView(views, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartPurchaseItem> getPurchasableItems(Long userId) {
        return cartReadModel.findDetailsByUserId(userId).stream()
                .filter(DefaultCartService::isAvailable)
                .map(item -> new CartPurchaseItem(item.cartItemId(), item.productId(), item.qty()))
                .toList();
    }

    @Override
    public void addItem(Long userId, Long productId, int qty) {
        productReader.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        LocalDateTime changedAt = LocalDateTime.now(clock);

        cartItemReader.findByUserIdAndProductIdForUpdate(userId, productId)
                .ifPresentOrElse(
                        existing -> existing.addQty(qty, changedAt),
                        () -> cartItemStore.save(new CartItem(userId, productId, qty, changedAt)));
    }

    @Override
    public void updateItemQty(Long userId, Long productId, int qty) {
        CartItem item = cartItemReader.findByUserIdAndProductIdForUpdate(userId, productId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        item.updateQty(qty, updatedAt);
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        CartItem item = cartItemReader.findByUserIdAndProductIdForUpdate(userId, productId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        cartItemStore.delete(item);
    }

    @Override
    public void removePurchasedItems(Long userId, List<PurchasedItem> items) {
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        Map<Long, Integer> purchasedQuantities = items.stream()
                .filter(item -> item.cartItemId() != null)
                .collect(toMap(PurchasedItem::cartItemId, PurchasedItem::qty));
        if (purchasedQuantities.isEmpty()) {
            return;
        }
        List<CartItem> purchasedCartItems = cartItemReader.findAllByUserIdAndIdInOrderByIdAsc(
                userId, purchasedQuantities.keySet());
        for (CartItem cartItem : purchasedCartItems) {
            removePurchasedQuantity(
                    cartItem,
                    purchasedQuantities.get(cartItem.getId()),
                    updatedAt);
        }
    }

    private void removePurchasedQuantity(CartItem cartItem, int purchasedQty, LocalDateTime updatedAt) {
        if (cartItem.getQty() > purchasedQty) {
            cartItem.updateQty(cartItem.getQty() - purchasedQty, updatedAt);
            return;
        }
        cartItemStore.delete(cartItem);
    }

    private static boolean isAvailable(CartItemDetail item) {
        return item.productStatus() == ProductStatus.ACTIVE
                && item.inventoryQuantity() != null
                && item.inventoryQuantity() >= item.qty();
    }
}

package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.cart.port.out.CartQueryPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.product.ProductStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCartService implements CartUseCase {

    private final CartItemReaderPort cartItemReader;
    private final CartItemStorePort cartItemStore;
    private final CartQueryPort cartQuery;
    private final ProductReaderPort productReader;
    private final Clock clock;

    public DefaultCartService(CartItemReaderPort cartItemReader,
                              CartItemStorePort cartItemStore,
                              CartQueryPort cartQuery,
                              ProductReaderPort productReader,
                              Clock clock) {
        this.cartItemReader = cartItemReader;
        this.cartItemStore = cartItemStore;
        this.cartQuery = cartQuery;
        this.productReader = productReader;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView getCart(Long userId) {
        List<CartItemView> views = cartQuery.findDetailsByUserId(userId).stream()
                .map(item -> {
                    boolean available = item.productStatus() == ProductStatus.ACTIVE
                            && item.inventoryQuantity() != null
                            && item.inventoryQuantity() > 0;
                    return new CartItemView(
                            item.productId(), item.productName(), item.price(), item.qty(), available);
                })
                .toList();

        long total = views.stream().mapToLong(CartItemView::subtotal).sum();
        return new CartView(views, total);
    }

    @Override
    public void addItem(Long userId, Long productId, int qty) {
        productReader.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        LocalDateTime changedAt = LocalDateTime.now(clock);

        cartItemReader.findByUserIdAndProductId(userId, productId)
                .ifPresentOrElse(
                        existing -> existing.addQty(qty, changedAt),
                        () -> cartItemStore.save(new CartItem(userId, productId, qty, changedAt)));
    }

    @Override
    public void updateItemQty(Long userId, Long productId, int qty) {
        CartItem item = cartItemReader.findByUserIdAndProductId(userId, productId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        item.updateQty(qty, updatedAt);
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        CartItem item = cartItemReader.findByUserIdAndProductId(userId, productId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        cartItemStore.delete(item);
    }

    @Override
    public void clearCart(Long userId) {
        cartItemStore.deleteAllByUserId(userId);
    }
}

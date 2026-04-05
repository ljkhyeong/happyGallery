package com.personal.happygallery.app.cart;

import com.personal.happygallery.app.cart.port.in.CartUseCase;
import com.personal.happygallery.app.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.app.cart.port.out.CartItemStorePort;
import com.personal.happygallery.app.product.port.out.InventoryReaderPort;
import com.personal.happygallery.app.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCartService implements CartUseCase {

    private final CartItemReaderPort cartItemReader;
    private final CartItemStorePort cartItemStore;
    private final ProductReaderPort productReader;
    private final InventoryReaderPort inventoryReader;
    private final Clock clock;

    public DefaultCartService(CartItemReaderPort cartItemReader,
                       CartItemStorePort cartItemStore,
                       ProductReaderPort productReader,
                       InventoryReaderPort inventoryReader,
                       Clock clock) {
        this.cartItemReader = cartItemReader;
        this.cartItemStore = cartItemStore;
        this.productReader = productReader;
        this.inventoryReader = inventoryReader;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView getCart(Long userId) {
        List<CartItem> items = cartItemReader.findByUserId(userId);
        if (items.isEmpty()) {
            return new CartView(List.of(), 0);
        }

        List<Long> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<Long, Product> products = productIds.stream()
                .map(pid -> productReader.findById(pid).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, Inventory> inventories = inventoryReader.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        List<CartItemView> views = items.stream()
                .filter(ci -> products.containsKey(ci.getProductId()))
                .map(ci -> {
                    Product p = products.get(ci.getProductId());
                    Inventory inv = inventories.get(ci.getProductId());
                    boolean available = p.getStatus() == ProductStatus.ACTIVE
                            && inv != null && inv.getQuantity() > 0;
                    return new CartItemView(p.getId(), p.getName(), p.getPrice(), ci.getQty(), available);
                })
                .toList();

        long total = views.stream().mapToLong(CartItemView::subtotal).sum();
        return new CartView(views, total);
    }

    @Override
    public void addItem(Long userId, Long productId, int qty) {
        productReader.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        LocalDateTime now = LocalDateTime.now(clock);

        cartItemReader.findByUserIdAndProductId(userId, productId)
                .ifPresentOrElse(
                        existing -> existing.addQty(qty, now),
                        () -> cartItemStore.save(new CartItem(userId, productId, qty, now)));
    }

    @Override
    public void updateItemQty(Long userId, Long productId, int qty) {
        CartItem item = cartItemReader.findByUserIdAndProductId(userId, productId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        item.updateQty(qty, LocalDateTime.now(clock));
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

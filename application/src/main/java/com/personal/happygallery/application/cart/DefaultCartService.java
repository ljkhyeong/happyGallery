package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartPurchaseItem;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartItemView;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasableCart;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasedItem;
import com.personal.happygallery.application.cart.port.out.CartItemDetail;
import com.personal.happygallery.application.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort;
import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort.Registration;
import com.personal.happygallery.application.cart.port.out.CartOwnerLockPort;
import com.personal.happygallery.application.cart.port.out.CartReadModelPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

@Service
@Transactional
public class DefaultCartService implements CartUseCase {

    private final CartItemReaderPort cartItemReader;
    private final CartItemStorePort cartItemStore;
    private final CartMergeRequestStorePort cartMergeRequestStore;
    private final CartOwnerLockPort cartOwnerLock;
    private final CartReadModelPort cartReadModel;
    private final ProductReaderPort productReader;
    private final Clock clock;

    public DefaultCartService(CartItemReaderPort cartItemReader,
                              CartItemStorePort cartItemStore,
                              CartMergeRequestStorePort cartMergeRequestStore,
                              CartOwnerLockPort cartOwnerLock,
                              CartReadModelPort cartReadModel,
                              ProductReaderPort productReader,
                              Clock clock) {
        this.cartItemReader = cartItemReader;
        this.cartItemStore = cartItemStore;
        this.cartMergeRequestStore = cartMergeRequestStore;
        this.cartOwnerLock = cartOwnerLock;
        this.cartReadModel = cartReadModel;
        this.productReader = productReader;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CartView getCart(Long userId) {
        CartSnapshot snapshot = readSnapshot(userId);

        long total = 0L;
        for (CartItemView item : snapshot.views()) {
            if (item.available()) {
                total = OrderAmountCalculator.addLine(total, item.qty(), item.price());
            }
        }
        return new CartView(snapshot.views(), total, snapshot.version());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchasableCart getPurchasableCart(Long userId) {
        CartSnapshot snapshot = readSnapshot(userId);
        List<CartPurchaseItem> items = snapshot.details().stream()
                .filter(DefaultCartService::isAvailable)
                .map(item -> new CartPurchaseItem(item.cartItemId(), item.productId(), item.qty()))
                .toList();
        return new PurchasableCart(items, snapshot.version());
    }

    private CartSnapshot readSnapshot(Long userId) {
        List<CartItemDetail> details = cartReadModel.findDetailsByUserId(userId);
        List<CartItemView> views = details.stream()
                .map(item -> new CartItemView(
                        item.productId(), item.productName(), item.productType(),
                        item.price(), item.specification(), item.careInstructions(),
                        item.productionLeadDays(), item.qty(), isAvailable(item)))
                .toList();
        return new CartSnapshot(details, views, CartSnapshotVersion.from(views));
    }

    @Override
    public void addItem(Long userId, Long productId, int qty) {
        productReader.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        LocalDateTime changedAt = LocalDateTime.now(clock);

        cartOwnerLock.lock(userId);
        addItem(userId, productId, qty, changedAt);
    }

    @Override
    public void mergeItems(Long userId, UUID idempotencyKey, List<MergeItem> items) {
        Map<Long, Integer> quantitiesByProductId = new TreeMap<>();
        try {
            for (MergeItem item : items) {
                int quantity = quantitiesByProductId.merge(item.productId(), item.qty(), Math::addExact);
                OrderAmountCalculator.requireQuantity(quantity);
            }
        } catch (ArithmeticException e) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "장바구니 수량이 너무 큽니다.");
        }
        LocalDateTime changedAt = LocalDateTime.now(clock);
        cartOwnerLock.lock(userId);
        Registration registration = cartMergeRequestStore.register(
                userId, idempotencyKey, payloadHash(quantitiesByProductId), changedAt);
        if (registration == Registration.REPLAY) {
            return;
        }
        if (registration == Registration.CONFLICT) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "같은 멱등키를 다른 장바구니 요청에 사용할 수 없습니다.");
        }
        if (productReader.findAllById(quantitiesByProductId.keySet()).size()
                != quantitiesByProductId.size()) {
            throw new NotFoundException("상품");
        }

        Map<Long, CartItem> existingItemsByProductId = new HashMap<>();
        for (CartItem item : cartItemReader.findAllByUserIdAndProductIdInForUpdate(
                userId, quantitiesByProductId.keySet())) {
            existingItemsByProductId.put(item.getProductId(), item);
        }
        List<CartItem> newItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantitiesByProductId.entrySet()) {
            CartItem existingItem = existingItemsByProductId.get(entry.getKey());
            if (existingItem != null) {
                existingItem.addQty(entry.getValue(), changedAt);
                continue;
            }
            newItems.add(new CartItem(
                    userId, entry.getKey(), entry.getValue(), changedAt));
        }
        cartItemStore.saveAll(newItems);
    }

    private static String payloadHash(Map<Long, Integer> quantitiesByProductId) {
        String payload = quantitiesByProductId.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(joining("&"));
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private void addItem(Long userId, Long productId, int qty, LocalDateTime changedAt) {
        cartItemReader.findByUserIdAndProductIdForUpdate(userId, productId)
                .ifPresentOrElse(
                        existing -> existing.addQty(qty, changedAt),
                        () -> cartItemStore.save(new CartItem(userId, productId, qty, changedAt)));
    }

    @Override
    public void updateItemQty(Long userId, Long productId, int qty) {
        cartOwnerLock.lock(userId);
        CartItem item = cartItemReader.findByUserIdAndProductIdForUpdate(userId, productId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        item.updateQty(qty, updatedAt);
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        cartOwnerLock.lock(userId);
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
        cartOwnerLock.lock(userId);
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

    private record CartSnapshot(
            List<CartItemDetail> details,
            List<CartItemView> views,
            String version
    ) {}
}

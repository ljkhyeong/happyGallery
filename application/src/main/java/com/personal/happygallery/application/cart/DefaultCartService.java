package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartItemView;
import com.personal.happygallery.application.cart.port.in.CartUseCase.CartPurchaseItem;
import com.personal.happygallery.application.cart.port.in.CartUseCase.MergeItem;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasableCart;
import com.personal.happygallery.application.cart.port.in.CartUseCase.PurchasedItem;
import com.personal.happygallery.application.cart.port.out.CartItemReaderPort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort;
import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort.Registration;
import com.personal.happygallery.application.cart.port.out.CartOwnerLockPort;
import com.personal.happygallery.application.product.ProductOptionConfigurationService;
import com.personal.happygallery.application.product.ProductOptions.PurchaseRequest;
import com.personal.happygallery.application.product.ProductOptions.ResolvedLine;
import com.personal.happygallery.application.product.ProductOptions.ResolvedPurchase;
import com.personal.happygallery.application.product.ProductOptions.TextInput;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.cart.CartItemTextInput;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.order.OrderAmountCalculator;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCartService implements CartUseCase {

    private final CartItemReaderPort cartItemReader;
    private final CartItemStorePort cartItemStore;
    private final CartMergeRequestStorePort cartMergeRequestStore;
    private final CartOwnerLockPort cartOwnerLock;
    private final ProductReaderPort productReader;
    private final InventoryReaderPort inventoryReader;
    private final ProductOptionConfigurationService optionConfigurationService;
    private final Clock clock;

    public DefaultCartService(CartItemReaderPort cartItemReader,
                              CartItemStorePort cartItemStore,
                              CartMergeRequestStorePort cartMergeRequestStore,
                              CartOwnerLockPort cartOwnerLock,
                              ProductReaderPort productReader,
                              InventoryReaderPort inventoryReader,
                              ProductOptionConfigurationService optionConfigurationService,
                              Clock clock) {
        this.cartItemReader = cartItemReader;
        this.cartItemStore = cartItemStore;
        this.cartMergeRequestStore = cartMergeRequestStore;
        this.cartOwnerLock = cartOwnerLock;
        this.productReader = productReader;
        this.inventoryReader = inventoryReader;
        this.optionConfigurationService = optionConfigurationService;
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
        Map<Long, CartItemView> viewsById = snapshot.views().stream()
                .collect(Collectors.toMap(CartItemView::cartItemId, Function.identity()));
        List<CartPurchaseItem> items = snapshot.items().stream()
                .filter(item -> viewsById.get(item.getId()).available())
                .map(item -> new CartPurchaseItem(
                        item.getId(),
                        item.getProductId(),
                        item.getProductVariantId(),
                        item.getTextInputs().stream()
                                .map(input -> new TextInput(input.getOptionKey(), input.getValue()))
                                .toList(),
                        item.getQty()))
                .toList();
        return new PurchasableCart(items, snapshot.version());
    }

    private CartSnapshot readSnapshot(Long userId) {
        List<CartItem> items = cartItemReader.findAllWithTextInputsByUserId(userId);
        if (items.isEmpty()) {
            return new CartSnapshot(List.of(), List.of(), CartSnapshotVersion.from(List.of()));
        }
        List<Long> productIds = items.stream().map(CartItem::getProductId).distinct().toList();
        Map<Long, Product> productsById = productReader.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Inventory> inventoriesByProductId = inventoryReader.findByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));
        Map<Integer, ResolvedLine> resolvedByIndex = optionConfigurationService
                .resolvePurchasesForCart(toCartPurchaseRequests(items)).stream()
                .collect(Collectors.toMap(ResolvedLine::index, Function.identity()));
        Map<StockKey, Long> quantitiesByStockKey = quantitiesByStockKey(items);

        List<CartItemView> views = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            CartItem item = items.get(index);
            Product product = productsById.get(item.getProductId());
            ResolvedLine resolved = resolvedByIndex.get(index);
            if (product == null || resolved == null) {
                if (product == null) {
                    throw new NotFoundException("상품");
                }
                views.add(unavailableView(item, product));
                continue;
            }
            ResolvedPurchase purchase = resolved.purchase();
            long stockQuantity = quantitiesByStockKey.get(stockKey(item));
            boolean stockAvailable = product.getType() == ProductType.READY_STOCK
                    ? readyStockAvailable(inventoriesByProductId.get(product.getId()), stockQuantity)
                    : purchase.variantActive() && purchase.availableQuantity() >= stockQuantity;
            views.add(new CartItemView(
                    item.getId(),
                    product.getId(),
                    purchase.variantId(),
                    product.getName(),
                    product.getType(),
                    purchase.basePrice(),
                    purchase.variantPriceAdjustment(),
                    purchase.textOptionPriceAdjustment(),
                    purchase.unitPrice(),
                    product.getSpecification(),
                    product.getCareInstructions(),
                    product.getProductionLeadDays(),
                    purchase.optionSnapshots(),
                    item.getQty(),
                    product.getStatus() == ProductStatus.ACTIVE && stockAvailable));
        }
        List<CartItemView> immutableViews = List.copyOf(views);
        return new CartSnapshot(items, immutableViews, CartSnapshotVersion.from(immutableViews));
    }

    @Override
    public void addItem(Long userId, Long productId, int qty) {
        addItem(userId, productId, null, List.of(), qty);
    }

    @Override
    public void addItem(Long userId, Long productId, Long productVariantId,
                        List<TextInput> textInputs, int qty) {
        OrderAmountCalculator.requireQuantity(qty);
        cartOwnerLock.lock(userId);
        ResolvedLine resolved = optionConfigurationService.resolvePurchases(List.of(
                new PurchaseRequest(0, productId, productVariantId, textInputs))).getFirst();
        LocalDateTime changedAt = LocalDateTime.now(clock);
        List<CartItemTextInput> cartInputs = cartInputs(resolved.purchase());
        CartItem candidate = new CartItem(
                userId, productId, resolved.purchase().variantId(), cartInputs, qty, changedAt);

        List<CartItem> existingItems = cartItemReader.findAllByUserIdAndProductIdInForUpdate(
                userId, List.of(productId));
        StockKey stockKey = stockKey(resolved);
        long desiredQuantity = quantitiesByStockKey(existingItems)
                .getOrDefault(stockKey, 0L) + qty;
        requireAvailableStock(
                Map.of(stockKey, desiredQuantity), Map.of(stockKey, resolved));

        Optional.ofNullable(indexCartLines(existingItems).get(candidate.getLineKey()))
                .ifPresentOrElse(
                        existing -> existing.addQty(qty, changedAt),
                        () -> cartItemStore.save(candidate));
    }

    @Override
    public void mergeItems(Long userId, UUID idempotencyKey, List<MergeItem> items) {
        if (items.isEmpty()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "장바구니 병합 항목이 비었습니다.");
        }
        for (MergeItem item : items) {
            OrderAmountCalculator.requireQuantity(item.qty());
        }
        LocalDateTime changedAt = LocalDateTime.now(clock);
        cartOwnerLock.lock(userId);
        Registration registration = cartMergeRequestStore.register(
                userId, idempotencyKey, payloadHash(items), changedAt);
        if (registration == Registration.REPLAY) {
            return;
        }
        if (registration == Registration.CONFLICT) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "같은 멱등키를 다른 장바구니 요청에 사용할 수 없습니다.");
        }

        List<ResolvedLine> resolvedLines = optionConfigurationService.resolvePurchases(
                toMergePurchaseRequests(items));
        Map<String, PendingCartLine> pendingByLineKey = new LinkedHashMap<>();
        for (ResolvedLine resolved : resolvedLines) {
            MergeItem requested = items.get(resolved.index());
            List<CartItemTextInput> textInputs = cartInputs(resolved.purchase());
            String lineKey = CartItem.lineKey(
                    requested.productId(), resolved.purchase().variantId(), textInputs);
            PendingCartLine pending = pendingByLineKey.get(lineKey);
            if (pending == null) {
                pendingByLineKey.put(lineKey, new PendingCartLine(
                        requested.productId(), resolved.purchase().variantId(),
                        textInputs, requested.qty()));
                continue;
            }
            try {
                int quantity = Math.addExact(pending.quantity(), requested.qty());
                OrderAmountCalculator.requireQuantity(quantity);
                pendingByLineKey.put(lineKey, pending.withQuantity(quantity));
            } catch (ArithmeticException exception) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "장바구니 수량이 너무 큽니다.");
            }
        }

        List<Long> productIds = pendingByLineKey.values().stream()
                .map(PendingCartLine::productId)
                .distinct()
                .toList();
        List<CartItem> existingItems = cartItemReader.findAllByUserIdAndProductIdInForUpdate(
                userId, productIds);
        Map<StockKey, Long> desiredQuantities = quantitiesByStockKey(existingItems);
        Map<StockKey, ResolvedLine> resolvedByStockKey = new HashMap<>();
        for (ResolvedLine resolved : resolvedLines) {
            StockKey stockKey = stockKey(resolved);
            desiredQuantities.merge(
                    stockKey, (long) items.get(resolved.index()).qty(), Long::sum);
            resolvedByStockKey.putIfAbsent(stockKey, resolved);
        }
        requireAvailableStock(desiredQuantities, resolvedByStockKey);

        Map<String, CartItem> existingByLineKey = indexCartLines(existingItems);
        List<CartItem> newItems = new ArrayList<>();
        for (Map.Entry<String, PendingCartLine> entry : pendingByLineKey.entrySet()) {
            CartItem existing = existingByLineKey.get(entry.getKey());
            if (existing != null) {
                existing.addQty(entry.getValue().quantity(), changedAt);
                continue;
            }
            PendingCartLine pending = entry.getValue();
            newItems.add(new CartItem(
                    userId, pending.productId(), pending.productVariantId(), pending.textInputs(),
                    pending.quantity(), changedAt));
        }
        cartItemStore.saveAll(newItems);
    }

    @Override
    public void updateItemQty(Long userId, Long cartItemId, int qty) {
        cartOwnerLock.lock(userId);
        CartItem item = cartItemReader.findByUserIdAndIdForUpdate(userId, cartItemId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        OrderAmountCalculator.requireQuantity(qty);
        if (qty > item.getQty()) {
            ResolvedLine resolved = optionConfigurationService.resolvePurchases(List.of(
                    toPurchaseRequest(0, item))).getFirst();
            StockKey stockKey = stockKey(resolved);
            List<CartItem> existingItems = cartItemReader.findAllByUserIdAndProductIdInForUpdate(
                    userId, List.of(item.getProductId()));
            long desiredQuantity = quantitiesByStockKey(existingItems).get(stockKey)
                    - item.getQty() + qty;
            requireAvailableStock(
                    Map.of(stockKey, desiredQuantity), Map.of(stockKey, resolved));
        }
        item.updateQty(qty, LocalDateTime.now(clock));
    }

    @Override
    public void removeItem(Long userId, Long cartItemId) {
        cartOwnerLock.lock(userId);
        CartItem item = cartItemReader.findByUserIdAndIdForUpdate(userId, cartItemId)
                .orElseThrow(NotFoundException.supplier("장바구니 항목"));
        cartItemStore.delete(item);
    }

    @Override
    public void removePurchasedItems(Long userId, List<PurchasedItem> items) {
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        Map<Long, Integer> purchasedQuantities = items.stream()
                .filter(item -> item.cartItemId() != null)
                .collect(Collectors.toMap(PurchasedItem::cartItemId, PurchasedItem::qty));
        if (purchasedQuantities.isEmpty()) {
            return;
        }
        cartOwnerLock.lock(userId);
        List<CartItem> purchasedCartItems = cartItemReader.findAllByUserIdAndIdInOrderByIdAsc(
                userId, purchasedQuantities.keySet());
        for (CartItem cartItem : purchasedCartItems) {
            int purchasedQty = purchasedQuantities.get(cartItem.getId());
            if (cartItem.getQty() > purchasedQty) {
                cartItem.updateQty(cartItem.getQty() - purchasedQty, updatedAt);
            } else {
                cartItemStore.delete(cartItem);
            }
        }
    }

    private static List<PurchaseRequest> toCartPurchaseRequests(List<CartItem> items) {
        List<PurchaseRequest> requests = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            requests.add(toPurchaseRequest(index, items.get(index)));
        }
        return List.copyOf(requests);
    }

    private static PurchaseRequest toPurchaseRequest(int index, CartItem item) {
        return new PurchaseRequest(
                index,
                item.getProductId(),
                item.getProductVariantId(),
                item.getTextInputs().stream()
                        .map(input -> new TextInput(input.getOptionKey(), input.getValue()))
                        .toList());
    }

    private static List<PurchaseRequest> toMergePurchaseRequests(List<MergeItem> items) {
        List<PurchaseRequest> requests = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            MergeItem item = items.get(index);
            requests.add(new PurchaseRequest(
                    index, item.productId(), item.productVariantId(), item.textInputs()));
        }
        return List.copyOf(requests);
    }

    private static List<CartItemTextInput> cartInputs(ResolvedPurchase purchase) {
        return purchase.textInputs().stream()
                .map(input -> new CartItemTextInput(
                        input.groupId(), input.groupKey(), input.value(), input.sortOrder()))
                .toList();
    }

    private static Map<String, CartItem> indexCartLines(List<CartItem> items) {
        return items.stream().collect(Collectors.toMap(
                item -> CartItem.lineKey(item.getProductId(), item.getProductVariantId(), item.getTextInputs()),
                Function.identity(),
                BinaryOperator.minBy(Comparator.comparing(CartItem::getId))));
    }

    private void requireAvailableStock(
            Map<StockKey, Long> desiredQuantities,
            Map<StockKey, ResolvedLine> resolvedByStockKey
    ) {
        for (StockKey stockKey : resolvedByStockKey.keySet()) {
            requireSkuQuantity(desiredQuantities.get(stockKey));
        }

        List<Long> readyStockProductIds = resolvedByStockKey.entrySet().stream()
                .filter(entry -> entry.getValue().product().getType() == ProductType.READY_STOCK)
                .map(entry -> entry.getKey().productId())
                .toList();
        Map<Long, Inventory> inventoriesByProductId = readyStockProductIds.isEmpty()
                ? Map.of()
                : inventoryReader.findByProductIdIn(readyStockProductIds).stream()
                        .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        for (Map.Entry<StockKey, ResolvedLine> entry : resolvedByStockKey.entrySet()) {
            StockKey stockKey = entry.getKey();
            ResolvedLine resolved = entry.getValue();
            long availableQuantity = resolved.product().getType() == ProductType.READY_STOCK
                    ? inventoryQuantity(inventoriesByProductId.get(stockKey.productId()))
                    : resolved.purchase().availableQuantity();
            if (desiredQuantities.get(stockKey) > availableQuantity) {
                throw new InventoryNotEnoughException();
            }
        }
    }

    private static void requireSkuQuantity(long quantity) {
        if (quantity > Integer.MAX_VALUE) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "장바구니 수량이 너무 큽니다.");
        }
        OrderAmountCalculator.requireQuantity((int) quantity);
    }

    private static Map<StockKey, Long> quantitiesByStockKey(List<CartItem> items) {
        Map<StockKey, Long> quantities = new HashMap<>();
        for (CartItem item : items) {
            quantities.merge(stockKey(item), (long) item.getQty(), Long::sum);
        }
        return quantities;
    }

    private static StockKey stockKey(CartItem item) {
        return new StockKey(item.getProductId(), item.getProductVariantId());
    }

    private static StockKey stockKey(ResolvedLine resolved) {
        return new StockKey(resolved.product().getId(), resolved.purchase().variantId());
    }

    private static int inventoryQuantity(Inventory inventory) {
        return inventory == null ? 0 : inventory.getQuantity();
    }

    private static boolean readyStockAvailable(Inventory inventory, long quantity) {
        return inventory != null && inventory.getQuantity() >= quantity;
    }

    private static CartItemView unavailableView(CartItem item, Product product) {
        return new CartItemView(
                item.getId(), item.getProductId(), item.getProductVariantId(),
                product.getName(), product.getType(), product.getPrice(), 0L, 0L,
                product.getPrice(), product.getSpecification(), product.getCareInstructions(),
                product.getProductionLeadDays(), List.of(), item.getQty(), false);
    }

    private static String payloadHash(List<MergeItem> items) {
        String canonicalItems = items.stream()
                .map(item -> {
                    String inputs = item.textInputs().stream()
                            .sorted(Comparator.comparing(TextInput::groupKey))
                            .map(input -> Base64.getEncoder().encodeToString(
                                    input.groupKey().getBytes(StandardCharsets.UTF_8)) + "="
                                    + Base64.getEncoder().encodeToString((input.value() == null
                                            ? "" : input.value().strip()).getBytes(StandardCharsets.UTF_8)))
                            .collect(Collectors.joining(";"));
                    return item.productId() + "|" + item.productVariantId()
                            + "|" + inputs + "|" + item.qty();
                })
                .sorted()
                .collect(Collectors.joining("&"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalItems.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private record CartSnapshot(
            List<CartItem> items,
            List<CartItemView> views,
            String version
    ) {}

    private record PendingCartLine(
            Long productId,
            Long productVariantId,
            List<CartItemTextInput> textInputs,
            int quantity
    ) {
        private PendingCartLine {
            textInputs = List.copyOf(textInputs);
        }

        private PendingCartLine withQuantity(int newQuantity) {
            return new PendingCartLine(productId, productVariantId, textInputs, newQuantity);
        }
    }

    private record StockKey(Long productId, Long productVariantId) {}
}

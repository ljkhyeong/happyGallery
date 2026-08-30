package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.product.ProductOptions.TextInput;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
@Transactional
class CartQueryUseCaseIT {

    @Autowired CartUseCase cartUseCase;
    @Autowired CartItemStorePort cartItemStore;
    @Autowired UserStorePort userStore;
    @Autowired ProductStorePort productStore;
    @Autowired ProductAdminUseCase productAdminUseCase;
    @Autowired InventoryStorePort inventoryStore;
    @Autowired Clock clock;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("각인 구분문자를 보존하고 기존 장바구니 키 전환 뒤에도 같은 항목에 수량을 더한다")
    void textInputKeys_preserveStructureAndMigrateExistingItems() {
        User user = userStore.save(new User(
                "cart-encoding@example.com", "hashed", "각인 고객", "01012345678"));
        var registered = productAdminUseCase.register(new ProductAdminUseCase.SaveProductCommand(
                "구분문자 키링", ProductType.MADE_TO_ORDER, null,
                10_000L, 10, null, null, "키링", null, 5,
                List.of(
                        new ProductAdminUseCase.OptionGroupDefinition(
                                "a", ProductOptionType.TEXT, "앞면", false, 0, null, 200, 0L, List.of()),
                        new ProductAdminUseCase.OptionGroupDefinition(
                                "b", ProductOptionType.TEXT, "뒷면", false, 1, null, 200, 0L, List.of())),
                List.of()));
        Long productId = registered.product().getId();
        Long variantId = registered.options().variants().getFirst().id();
        String message = "각인🙂".repeat(25);
        List<TextInput> singleInput = List.of(new TextInput("a", message + ";b=B"));
        List<TextInput> twoInputs = List.of(new TextInput("a", message), new TextInput("b", "B"));
        cartUseCase.addItem(user.getId(), productId, variantId, singleInput, 1);
        entityManager.flush();
        Long itemId = cartUseCase.getCart(user.getId()).items().getFirst().cartItemId();
        String expectedKey = jdbc.queryForObject("SELECT line_key FROM cart_items WHERE id = ?", String.class, itemId);
        jdbc.update("UPDATE cart_items SET line_key = SHA2(?, 256) WHERE id = ?",
                "product=" + productId + "|variant=" + variantId + "|inputs=a=" + message + ";b=B;", itemId);

        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V162__encode_cart_text_input_keys.sql")).execute(jdbc.getDataSource());
        entityManager.clear();
        assertThat(jdbc.queryForObject("SELECT line_key FROM cart_items WHERE id = ?", String.class, itemId))
                .isEqualTo(expectedKey);
        cartUseCase.addItem(user.getId(), productId, variantId, singleInput, 1);
        UUID mergeKey = UUID.randomUUID();
        cartUseCase.mergeItems(user.getId(), mergeKey,
                List.of(new CartUseCase.MergeItem(productId, variantId, twoInputs, 1)));
        assertThat(cartUseCase.getCart(user.getId()).items())
                .extracting(CartUseCase.CartItemView::qty).containsExactlyInAnyOrder(2, 1);
        assertThat(cartUseCase.getCart(user.getId()).items())
                .anySatisfy(item -> {
                    assertThat(item.cartItemId()).isEqualTo(itemId);
                    assertThat(item.qty()).isEqualTo(2);
                });
        assertThatThrownBy(() -> cartUseCase.mergeItems(user.getId(), mergeKey,
                List.of(new CartUseCase.MergeItem(productId, variantId, singleInput, 1))))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @DisplayName("장바구니 조회는 상품과 재고 정보를 함께 조회해 가용성과 합계를 반환한다")
    @Test
    void getCart_returnsProductAndInventoryDetails() {
        User user = userStore.save(new User(
                "cart-query@example.com", "hashed", "장바구니 회원", "01012345678"));
        Product availableProduct = productStore.save(readyStockProduct("재고 상품", 39_000L));
        inventoryStore.save(inventory(availableProduct, 3));
        Product unavailableProduct = productStore.save(readyStockProduct("재고 없는 상품", 15_000L));

        LocalDateTime createdAt = LocalDateTime.now(clock);
        CartItem availableItem = cartItemStore.save(
                new CartItem(user.getId(), availableProduct.getId(), 2, createdAt));
        CartItem unavailableItem = cartItemStore.save(
                new CartItem(user.getId(), unavailableProduct.getId(), 1, createdAt.plusSeconds(1)));

        CartUseCase.CartView cart = cartUseCase.getCart(user.getId());

        assertSoftly(softly -> {
            softly.assertThat(cart.items())
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("cartItemId")
                    .containsExactly(
                    new CartUseCase.CartItemView(
                            availableProduct.getId(), "재고 상품", availableProduct.getType(),
                            39_000L, 2, true),
                    new CartUseCase.CartItemView(
                            unavailableProduct.getId(), "재고 없는 상품", unavailableProduct.getType(),
                            15_000L, 1, false));
            softly.assertThat(cart.items())
                    .extracting(CartUseCase.CartItemView::cartItemId)
                    .containsExactly(availableItem.getId(), unavailableItem.getId());
            softly.assertThat(cart.totalAmount()).isEqualTo(78_000L);
            softly.assertThat(cart.cartVersion()).matches("[0-9a-f]{64}");
        });
    }

    @DisplayName("장바구니 스냅샷 버전은 조회 결과가 같으면 유지되고 수량이 바뀌면 변경된다")
    @Test
    void getCart_cartVersionTracksVisibleSnapshot() {
        User user = userStore.save(new User(
                "cart-version@example.com", "hashed", "버전 회원", "01011112222"));
        Product product = productStore.save(readyStockProduct("버전 상품", 19_000L));
        Inventory inventory = inventoryStore.save(inventory(product, 5));
        CartItem cartItem = cartItemStore.save(new CartItem(
                user.getId(), product.getId(), 1, LocalDateTime.now(clock)));

        String firstVersion = cartUseCase.getCart(user.getId()).cartVersion();
        String repeatedVersion = cartUseCase.getCart(user.getId()).cartVersion();
        cartUseCase.updateItemQty(user.getId(), cartItem.getId(), 2);
        CartUseCase.CartView quantityChanged = cartUseCase.getCart(user.getId());
        product.updateDetails("버전 상품", null, 20_000L, null, null);
        productStore.save(product);
        CartUseCase.CartView priceChanged = cartUseCase.getCart(user.getId());
        inventory.deduct(4);
        inventoryStore.save(inventory);
        CartUseCase.CartView availabilityChanged = cartUseCase.getCart(user.getId());

        assertSoftly(softly -> {
            softly.assertThat(repeatedVersion).isEqualTo(firstVersion);
            softly.assertThat(quantityChanged.cartVersion()).isNotEqualTo(firstVersion);
            softly.assertThat(quantityChanged.items()).singleElement()
                    .extracting(CartUseCase.CartItemView::qty)
                    .isEqualTo(2);
            softly.assertThat(priceChanged.cartVersion())
                    .isNotEqualTo(quantityChanged.cartVersion());
            softly.assertThat(availabilityChanged.cartVersion())
                    .isNotEqualTo(priceChanged.cartVersion());
            softly.assertThat(availabilityChanged.items()).singleElement()
                    .extracting(CartUseCase.CartItemView::available)
                    .isEqualTo(false);
        });
    }

    @DisplayName("장바구니 병합은 기존 수량과 새 상품을 한 번만 반영하고 멱등키 재사용을 거절한다")
    @Test
    void mergeItems_isIdempotent() {
        User user = userStore.save(new User(
                "cart-merge@example.com", "hashed", "병합 회원", "01098765432"));
        Product existingProduct = productStore.save(readyStockProduct("기존 병합 상품", 10_000L));
        Product newProduct = productStore.save(readyStockProduct("새 병합 상품", 20_000L));
        inventoryStore.save(inventory(existingProduct, 5));
        inventoryStore.save(inventory(newProduct, 5));
        cartItemStore.save(new CartItem(
                user.getId(), existingProduct.getId(), 1, LocalDateTime.now(clock)));
        UUID idempotencyKey = UUID.randomUUID();
        List<CartUseCase.MergeItem> items = List.of(
                new CartUseCase.MergeItem(existingProduct.getId(), 2),
                new CartUseCase.MergeItem(newProduct.getId(), 1));

        cartUseCase.mergeItems(user.getId(), idempotencyKey, items);
        cartUseCase.mergeItems(user.getId(), idempotencyKey, items);

        assertThat(cartUseCase.getCart(user.getId()).items())
                .extracting(CartUseCase.CartItemView::productId, CartUseCase.CartItemView::qty)
                .containsExactly(
                        tuple(existingProduct.getId(), 3),
                        tuple(newProduct.getId(), 1));
        assertThatThrownBy(() -> cartUseCase.mergeItems(
                user.getId(), idempotencyKey,
                List.of(new CartUseCase.MergeItem(existingProduct.getId(), 3))))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));
    }

    @DisplayName("장바구니 추가·병합·수량 변경은 SKU 재고를 초과할 수 없다")
    @Test
    void cartMutations_rejectQuantityOverStock() {
        User user = userStore.save(new User(
                "cart-stock@example.com", "hashed", "재고 장바구니", "01022223333"));
        Product product = productStore.save(readyStockProduct("재고 제한 상품", 12_000L));
        inventoryStore.save(inventory(product, 2));
        cartUseCase.addItem(user.getId(), product.getId(), 1);
        Long cartItemId = cartUseCase.getCart(user.getId()).items().getFirst().cartItemId();

        assertThatThrownBy(() -> cartUseCase.addItem(user.getId(), product.getId(), 2))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVENTORY_NOT_ENOUGH));
        assertThatThrownBy(() -> cartUseCase.mergeItems(
                user.getId(), UUID.randomUUID(),
                List.of(new CartUseCase.MergeItem(product.getId(), 2))))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVENTORY_NOT_ENOUGH));
        assertThatThrownBy(() -> cartUseCase.updateItemQty(user.getId(), cartItemId, 3))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVENTORY_NOT_ENOUGH));

        assertThat(cartUseCase.getCart(user.getId()).items()).singleElement()
                .extracting(CartUseCase.CartItemView::qty)
                .isEqualTo(1);
    }

    @DisplayName("직접입력 값이 다른 주문제작 품목도 같은 SKU 재고를 함께 사용한다")
    @Test
    void getCart_separatesTextOptionLinesAndCalculatesPrice() {
        User user = userStore.save(new User(
                "cart-options@example.com", "hashed", "옵션 장바구니", "01087654321"));
        ProductAdminUseCase.ProductResult registered = productAdminUseCase.register(
                new ProductAdminUseCase.SaveProductCommand(
                        "각인 키링", ProductType.MADE_TO_ORDER, null,
                        20_000L, 2, null, null, "소가죽 키링", null, 5,
                        List.of(new ProductAdminUseCase.OptionGroupDefinition(
                                "engraving", ProductOptionType.TEXT, "각인 문구", true, 0,
                                null, 20, 2_000L, List.of())),
                        List.of()));
        Long variantId = registered.options().variants().getFirst().id();

        cartUseCase.addItem(
                user.getId(), registered.product().getId(), variantId,
                List.of(new TextInput("engraving", "HAPPY")), 1);
        cartUseCase.addItem(
                user.getId(), registered.product().getId(), variantId,
                List.of(new TextInput("engraving", "GALLERY")), 1);
        assertThatThrownBy(() -> cartUseCase.addItem(
                user.getId(), registered.product().getId(), variantId,
                List.of(new TextInput("engraving", "HAPPY")), 1))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVENTORY_NOT_ENOUGH));

        CartUseCase.CartView cart = cartUseCase.getCart(user.getId());

        assertSoftly(softly -> {
            softly.assertThat(cart.items()).hasSize(2)
                    .allSatisfy(item -> {
                        softly.assertThat(item.productVariantId()).isEqualTo(variantId);
                        softly.assertThat(item.price()).isEqualTo(22_000L);
                        softly.assertThat(item.available()).isTrue();
                    });
            softly.assertThat(cart.items().stream()
                            .flatMap(item -> item.options().stream())
                            .map(option -> option.value())
                            .toList())
                    .containsExactlyInAnyOrder("HAPPY", "GALLERY");
            softly.assertThat(cart.totalAmount()).isEqualTo(44_000L);
        });

        productAdminUseCase.adjustInventory(new ProductAdminUseCase.AdjustInventoryCommand(
                registered.product().getId(), variantId, InventoryAdjustmentType.DECREASE, 1,
                "오프라인 판매", null, "local-api-key"));
        CartUseCase.CartView stockChanged = cartUseCase.getCart(user.getId());

        assertSoftly(softly -> {
            softly.assertThat(stockChanged.items()).hasSize(2)
                    .allSatisfy(item -> softly.assertThat(item.available()).isFalse());
            softly.assertThat(stockChanged.totalAmount()).isZero();
        });
    }
}

package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
@Transactional
class CartQueryUseCaseIT {

    @Autowired CartUseCase cartUseCase;
    @Autowired CartItemStorePort cartItemStore;
    @Autowired UserStorePort userStore;
    @Autowired ProductStorePort productStore;
    @Autowired InventoryStorePort inventoryStore;
    @Autowired Clock clock;

    @DisplayName("장바구니 조회는 상품과 재고 정보를 함께 조회해 가용성과 합계를 반환한다")
    @Test
    void getCart_returnsProductAndInventoryDetails() {
        User user = userStore.save(new User(
                "cart-query@example.com", "hashed", "장바구니 회원", "01012345678"));
        Product availableProduct = productStore.save(readyStockProduct("재고 상품", 39_000L));
        inventoryStore.save(inventory(availableProduct, 3));
        Product unavailableProduct = productStore.save(readyStockProduct("재고 없는 상품", 15_000L));

        LocalDateTime createdAt = LocalDateTime.now(clock);
        cartItemStore.save(new CartItem(user.getId(), availableProduct.getId(), 2, createdAt));
        cartItemStore.save(new CartItem(user.getId(), unavailableProduct.getId(), 1, createdAt.plusSeconds(1)));

        CartUseCase.CartView cart = cartUseCase.getCart(user.getId());

        assertSoftly(softly -> {
            softly.assertThat(cart.items()).containsExactly(
                    new CartUseCase.CartItemView(
                            availableProduct.getId(), "재고 상품", 39_000L, 2, true),
                    new CartUseCase.CartItemView(
                            unavailableProduct.getId(), "재고 없는 상품", 15_000L, 1, false));
            softly.assertThat(cart.totalAmount()).isEqualTo(78_000L);
        });
    }

    @DisplayName("장바구니 병합은 같은 요청을 한 번만 반영하고 멱등키 재사용을 거절한다")
    @Test
    void mergeItems_isIdempotent() {
        User user = userStore.save(new User(
                "cart-merge@example.com", "hashed", "병합 회원", "01098765432"));
        Product product = productStore.save(readyStockProduct("병합 상품", 10_000L));
        UUID idempotencyKey = UUID.randomUUID();
        List<CartUseCase.MergeItem> items = List.of(
                new CartUseCase.MergeItem(product.getId(), 2));

        cartUseCase.mergeItems(user.getId(), idempotencyKey, items);
        cartUseCase.mergeItems(user.getId(), idempotencyKey, items);

        assertThat(cartUseCase.getCart(user.getId()).items())
                .singleElement()
                .extracting(CartUseCase.CartItemView::qty)
                .isEqualTo(2);
        assertThatThrownBy(() -> cartUseCase.mergeItems(
                user.getId(), idempotencyKey,
                List.of(new CartUseCase.MergeItem(product.getId(), 3))))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CONFLICT));
    }
}

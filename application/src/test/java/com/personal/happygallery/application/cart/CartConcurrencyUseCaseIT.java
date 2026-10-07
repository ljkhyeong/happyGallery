package com.personal.happygallery.application.cart;

import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.ConcurrentExecution.runSimultaneously;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static java.util.Collections.nCopies;
import static org.assertj.core.api.Assertions.assertThat;

@UseCaseIT
class CartConcurrencyUseCaseIT {

    private static final int CONCURRENT_REQUEST_COUNT = 10;

    @Autowired CartUseCase cartUseCase;
    @Autowired UserStorePort userStore;
    @Autowired ProductStorePort productStore;
    @Autowired InventoryStorePort inventoryStore;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearCartData();
        cleanupSupport.clearProductData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("같은 회원이 없는 장바구니 상품을 동시에 추가해도 한 행에 수량이 모두 합산된다")
    @Test
    void addItem_concurrently_createsOneItemWithSummedQuantity() throws Exception {
        User user = userStore.save(new User(
                "cart-concurrency@example.com", "hashed", "동시 장바구니 회원", "01055556666"));
        Product product = productStore.save(readyStockProduct("동시 추가 상품", 12_000L));
        inventoryStore.save(inventory(product, CONCURRENT_REQUEST_COUNT));
        assertThat(runSimultaneously(nCopies(CONCURRENT_REQUEST_COUNT, () -> cartUseCase.mergeItems(
                user.getId(), UUID.randomUUID(),
                List.of(new CartUseCase.MergeItem(product.getId(), null, List.of(), 1))))))
                .containsOnlyNulls();

        assertThat(cartUseCase.getCart(user.getId()).items())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.productId()).isEqualTo(product.getId());
                    assertThat(item.qty()).isEqualTo(CONCURRENT_REQUEST_COUNT);
                });
    }
}

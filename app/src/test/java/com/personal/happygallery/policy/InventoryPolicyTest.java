package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [PolicyTest] 재고 정책 검증.
 *
 * <p>단일 작품(quantity=1)에 대한 중복 주문 방지.
 * 재고가 요청 수량 이상이면 통과, 부족하면 {@link InventoryNotEnoughException}이 발생한다.
 */
@Tag("policy")
class InventoryPolicyTest {

    private Inventory inventory(int quantity) {
        return new Inventory(new Product("테스트 상품", ProductType.READY_STOCK, 10_000L), quantity);
    }

    @DisplayName("재고가 충분하면 재고 차감에서 예외가 발생하지 않는다")
    @Test
    void deduct_whenAvailable_noException() {
        assertThatCode(() -> inventory(1).deduct(1))
                .doesNotThrowAnyException();
    }

    @DisplayName("재고가 없으면 재고 차감에서 예외가 발생한다")
    @Test
    void deduct_whenOutOfStock_throws() {
        assertThatThrownBy(() -> inventory(0).deduct(1))
                .isInstanceOf(InventoryNotEnoughException.class);
    }

    @DisplayName("요청 수량이 가용 재고를 초과하면 예외가 발생한다")
    @Test
    void deduct_whenRequestExceedsAvailable_throws() {
        assertThatThrownBy(() -> inventory(1).deduct(2))
                .isInstanceOf(InventoryNotEnoughException.class);
    }
}

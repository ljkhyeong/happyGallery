package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    @DisplayName("재고 차감은 요청 수량이 가용 재고 이내일 때만 허용된다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("deductCases")
    void deduct_validatesAvailableQuantity(String caseName, int currentQuantity, int requestQuantity, boolean expectedSuccess) {
        Inventory inventory = inventory(currentQuantity);

        if (expectedSuccess) {
            assertThatCode(() -> inventory.deduct(requestQuantity))
                    .doesNotThrowAnyException();
            return;
        }
        assertThatThrownBy(() -> inventory.deduct(requestQuantity))
                .isInstanceOf(InventoryNotEnoughException.class);
    }

    @DisplayName("재고 생성과 변경은 음수 또는 0인 변경 수량을 허용하지 않는다")
    @Test
    void inventory_rejectsInvalidQuantities() {
        Inventory inventory = inventory(1);

        assertThatThrownBy(() -> inventory(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventory.deduct(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inventory.restore(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> deductCases() {
        return Stream.of(
                Arguments.of("재고가 충분하면 차감할 수 있다", 1, 1, true),
                Arguments.of("재고가 없으면 차감할 수 없다", 0, 1, false),
                Arguments.of("요청 수량이 가용 재고를 초과하면 차감할 수 없다", 1, 2, false)
        );
    }
}

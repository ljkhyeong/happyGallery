package com.personal.happygallery.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.personal.happygallery.adapter.out.persistence.order.SmartStoreProductOrderRepository;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductVariantRepository;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderChange;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.ProductVariantStockService;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

@UseCaseIT
class SmartStoreOrderInventoryUseCaseIT {

    private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 8, 31, 12, 0);

    @Autowired ProductAdminUseCase productAdminUseCase;
    @Autowired SmartStoreInventoryUseCase inventoryUseCase;
    @Autowired SmartStoreOrderTransactionService orderService;
    @Autowired SmartStoreProductOrderRepository orderRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired InventoryService inventoryService;
    @Autowired ProductVariantStockService variantStockService;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
        cleanupSupport.clearProductData();
    }

    @ParameterizedTest
    @EnumSource(ProductType.class)
    @DisplayName("재고 부족 주문을 저장하고 다음 주문을 처리하며 입고 후 재시도는 한 번만 차감한다")
    void shortage_isPersistedAndRetriedWithoutBlockingOtherOrders(ProductType type) {
        boolean madeToOrder = type == ProductType.MADE_TO_ORDER;
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "채널 주문 재고", type, null, 35000L, 1, null, null,
                madeToOrder ? "가죽 제품" : null, null, madeToOrder ? 7 : null,
                List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = madeToOrder ? registered.options().variants().getFirst().id() : null;
        Long optionId = madeToOrder ? 11L : null;
        inventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true,
                madeToOrder ? List.of(new VariantMapping(variantId, optionId)) : List.of()));

        orderService.synchronize(detail("shortage", optionId, 2), change("shortage"));

        var shortage = orderRepository.findById("shortage").orElseThrow();
        assertThat(shortage.getAttentionReason()).isEqualTo(SmartStoreOrderAttentionReason.STOCK_SHORTAGE);
        assertThat(shortage.getInventoryAppliedQuantity()).isZero();
        assertThat(quantity(productId, variantId)).isEqualTo(1);

        orderService.synchronize(detail("next-order", optionId, 1), change("next-order"));
        assertThat(orderRepository.findById("next-order").orElseThrow().getInventoryAppliedQuantity()).isEqualTo(1);
        assertThat(quantity(productId, variantId)).isZero();

        if (madeToOrder) {
            variantStockService.restoreAll(List.of(new VariantAdjustment(variantId, 2)));
        } else {
            inventoryService.restore(productId, 2);
        }
        orderService.retryInventory("shortage");
        orderService.retryInventory("shortage");

        var retried = orderRepository.findById("shortage").orElseThrow();
        assertThat(retried.getAttentionReason()).isNull();
        assertThat(retried.getInventoryAppliedQuantity()).isEqualTo(2);
        assertThat(quantity(productId, variantId)).isZero();
    }

    private int quantity(Long productId, Long variantId) {
        return variantId == null
                ? inventoryRepository.findById(productId).orElseThrow().getQuantity()
                : variantRepository.findById(variantId).orElseThrow().getQuantity();
    }

    @ParameterizedTest
    @CsvSource({"READY_STOCK, true", "READY_STOCK, false", "MADE_TO_ORDER, true", "MADE_TO_ORDER, false"})
    @DisplayName("반품 검수 결과는 재시도와 재수집 후에도 유지하고 추가 반품 수량만 새로 검수한다")
    void resolvedReturn_survivesRetryAndRefresh_andReviewsOnlyAdditionalQuantity(
            ProductType type, boolean restoreStock) {
        boolean madeToOrder = type == ProductType.MADE_TO_ORDER;
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "반품 검수 재고", type, null, 35000L, 3, null, null,
                madeToOrder ? "가죽 제품" : null, null, madeToOrder ? 7 : null,
                List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = madeToOrder ? registered.options().variants().getFirst().id() : null;
        Long optionId = madeToOrder ? 11L : null;
        inventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true,
                madeToOrder ? List.of(new VariantMapping(variantId, optionId)) : List.of()));
        orderService.synchronize(detail("returned", optionId, 3), change("returned"));
        ProductOrderDetail returned = returnedDetail(optionId, 1);
        ProductOrderChange returnChange = new ProductOrderChange(
                "returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(1));
        orderService.synchronize(returned, returnChange);

        orderService.resolveReturn("returned", restoreStock);
        orderService.retryInventory("returned");
        orderService.synchronize(returned, returnChange);
        orderService.synchronize(returned, new ProductOrderChange(
                "returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(2)));

        assertThat(orderRepository.findById("returned").orElseThrow().getAttentionReason()).isNull();
        assertThat(quantity(productId, variantId)).isEqualTo(restoreStock ? 2 : 0);
        assertThatThrownBy(() -> orderService.resolveReturn("returned", !restoreStock))
                .isInstanceOf(IllegalArgumentException.class);

        orderService.synchronize(returnedDetail(optionId, 0), new ProductOrderChange(
                "returned", "CLAIM_COMPLETED", CHANGED_AT.plusMinutes(3)));
        assertThat(orderRepository.findById("returned").orElseThrow().getAttentionReason())
                .isEqualTo(SmartStoreOrderAttentionReason.RETURN_REVIEW);
        orderService.resolveReturn("returned", true);
        orderService.retryInventory("returned");

        var resolved = orderRepository.findById("returned").orElseThrow();
        assertThat(resolved.getAttentionReason()).isNull();
        assertThat(resolved.getInventoryAppliedQuantity()).isEqualTo(restoreStock ? 0 : 2);
        assertThat(quantity(productId, variantId)).isEqualTo(restoreStock ? 3 : 1);
    }

    private static ProductOrderDetail returnedDetail(Long optionId, int remainQuantity) {
        return new ProductOrderDetail(
                "returned", "order-returned", 123L, optionId, "반품 검수 재고", null, null, "RETURNED",
                null, "RETURN", "RETURN_DONE", null, 3, remainQuantity, CHANGED_AT.minusMinutes(1), null,
                null, null, null, null, null, null, null, null, null);
    }

    private static ProductOrderDetail detail(String id, Long optionId, int quantity) {
        return new ProductOrderDetail(
                id, "order-" + id, 123L, optionId, "채널 주문 재고", null, null, "PAYED",
                null, null, null, null, quantity, quantity, CHANGED_AT.minusMinutes(1), null,
                null, null, null, null, null, null, null, null, null);
    }

    private static ProductOrderChange change(String id) {
        return new ProductOrderChange(id, "PAYED", CHANGED_AT);
    }
}

package com.personal.happygallery.application.order;

import static org.assertj.core.api.Assertions.assertThat;

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

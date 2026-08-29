package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderChange;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.ProductVariantStockService;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartStoreOrderTransactionServiceTest {

    private static final LocalDateTime CHANGED_AT = LocalDateTime.of(2026, 8, 29, 12, 0);

    private SmartStoreProductOrderPort orderPort;
    private SmartStoreStockMappingPort mappingPort;
    private InventoryService inventoryService;
    private SmartStoreOrderTransactionService service;

    @BeforeEach
    void setUp() {
        orderPort = mock(SmartStoreProductOrderPort.class);
        mappingPort = mock(SmartStoreStockMappingPort.class);
        inventoryService = mock(InventoryService.class);
        service = new SmartStoreOrderTransactionService(
                orderPort, mappingPort, inventoryService, mock(ProductVariantStockService.class),
                mock(SmartStoreDeliveryInfoProtector.class));
        when(orderPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("같은 결제 완료 변경을 다시 받아도 기성품 재고는 한 번만 차감한다")
    void synchronize_duplicatePaidChange_deductsOnce() {
        when(mappingPort.findByOriginProductNoAndProductVariantIdIsNull(123L))
                .thenReturn(Optional.of(new SmartStoreStockMapping(7L, null, 123L, null, true)));
        when(orderPort.findByProductOrderIdWithLock("po-1"))
                .thenReturn(Optional.empty());

        SmartStoreProductOrder order = service.synchronize(detail("PAYED", 1), change("PAYED", CHANGED_AT));
        when(orderPort.findByProductOrderIdWithLock("po-1")).thenReturn(Optional.of(order));
        service.synchronize(detail("PAYED", 1), change("PAYED", CHANGED_AT));

        verify(inventoryService).deduct(7L, 1);
        assertThat(order.getInventoryAppliedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("결제 완료 주문이 취소되면 차감했던 수량만 한 번 복원한다")
    void synchronize_canceledOrder_restoresAppliedQuantityOnce() {
        SmartStoreProductOrder order = new SmartStoreProductOrder(
                "po-1", "order-1", 123L, null, "가죽 지갑", null, "PAYED",
                null, null, 2, 2, "PAYED", CHANGED_AT.minusMinutes(1), CHANGED_AT);
        order.mapTo(7L, null);
        order.applyInventoryQuantity(2);
        when(orderPort.findByProductOrderIdWithLock("po-1")).thenReturn(Optional.of(order));

        service.synchronize(detail("CANCELED", 0),
                change("CLAIM_COMPLETED", CHANGED_AT.plusMinutes(1)));
        service.synchronize(detail("CANCELED", 0),
                change("CLAIM_COMPLETED", CHANGED_AT.plusMinutes(1)));

        verify(inventoryService).restore(7L, 2);
        assertThat(order.getInventoryAppliedQuantity()).isZero();
    }

    @Test
    @DisplayName("연동 매핑이 없는 결제 주문은 재고를 건드리지 않고 관리자 확인 대상으로 남긴다")
    void synchronize_unmappedOrder_marksAttention() {
        when(orderPort.findByProductOrderIdWithLock("po-1")).thenReturn(Optional.empty());
        when(mappingPort.findByOriginProductNoAndProductVariantIdIsNull(123L))
                .thenReturn(Optional.empty());

        SmartStoreProductOrder order = service.synchronize(detail("PAYED", 1), change("PAYED", CHANGED_AT));

        verify(inventoryService, never()).deduct(any(), any(Integer.class));
        assertThat(order.getAttentionReason()).isEqualTo(SmartStoreOrderAttentionReason.MAPPING_REQUIRED);
    }

    private static ProductOrderDetail detail(String status, int remainQuantity) {
        return new ProductOrderDetail(
                "po-1", "order-1", 123L, null, "가죽 지갑", null, null, status,
                null, null, null, 2, remainQuantity, CHANGED_AT.minusMinutes(1), null,
                null, null, null, null, null, null, null, null, null);
    }

    private static ProductOrderChange change(String type, LocalDateTime changedAt) {
        return new ProductOrderChange("po-1", type, changedAt);
    }
}

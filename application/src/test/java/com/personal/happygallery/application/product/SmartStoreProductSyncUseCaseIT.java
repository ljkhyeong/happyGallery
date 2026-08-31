package com.personal.happygallery.application.product;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.adapter.out.persistence.order.SmartStoreProductOrderRepository;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangePage;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderChange;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.application.product.port.in.SmartStoreStockSyncBatchUseCase;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ChannelProduct;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.StockCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.SyncResult;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@UseCaseIT
class SmartStoreProductSyncUseCaseIT {

    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryService inventoryService;
    @Autowired ProductAdminUseCase productAdminUseCase;
    @Autowired ProductVariantStockService variantStockService;
    @Autowired SmartStoreInventoryUseCase smartStoreInventoryUseCase;
    @Autowired SmartStoreStockSyncBatchUseCase stockSyncBatchUseCase;
    @Autowired SmartStoreStockSyncPort stockSyncPort;
    @Autowired SmartStoreProductOrderRepository channelOrderRepository;
    @Autowired SmartStoreChannelOrderUseCase channelOrderUseCase;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean SmartStoreInventoryProvider provider;
    @MockitoBean SmartStoreOrderProvider orderProvider;

    @BeforeEach
    void setUp() {
        when(orderProvider.isEnabled()).thenReturn(true);
        when(orderProvider.fetchChanges(any(), any())).thenReturn(new ChangePage(List.of(), null));
        when(orderProvider.fetchDetails(any())).thenReturn(List.of());
        resetCursor(LocalDateTime.now(clock).minusMinutes(1), null);
    }

    @AfterEach
    void tearDown() {
        channelOrderRepository.deleteAllInBatch();
        cleanupSupport.clearProductData();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("수동 반영이 자동 재고 전송보다 늦게 끝나도 성공·부분 실패 뒤 최신 재고를 다시 보낸다")
    void applyProduct_requeuesCurrentStockAfterOverlappingAutomaticSync(boolean success) {
        var product = productRepository.save(readyStockProduct("스마트스토어 연동 상품", 35000L));
        Long productId = product.getId();
        inventoryRepository.save(inventory(product, 10));
        smartStoreInventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true, List.of()));
        AtomicInteger channelStock = new AtomicInteger(10);
        when(provider.isEnabled()).thenReturn(true);
        when(provider.getProduct(anyLong())).thenReturn(new ChannelProduct(33000L, "SALE", List.of()));
        String previewVersion = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
        when(provider.sync(any())).thenAnswer(invocation -> {
            StockCommand command = invocation.getArgument(0);
            channelStock.set(command.stockQuantity());
            return SyncResult.completed();
        });
        when(provider.applyProduct(any())).thenAnswer(invocation -> {
            ProductCommand command = invocation.getArgument(0);
            assertThat(command.stockQuantity()).isEqualTo(10);
            inventoryService.deduct(productId, 1);
            stockSyncBatchUseCase.syncPendingStocks();
            assertThat(channelStock.get()).isEqualTo(9);
            assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                    .isEqualTo(SmartStoreStockSyncStatus.SYNCED);
            channelStock.set(command.stockQuantity());
            return success ? SyncResult.completed() : SyncResult.failure("재고 변경 후 판매 상태 반영 실패");
        });

        if (success) {
            smartStoreInventoryUseCase.applyProduct(productId, previewVersion);
        } else {
            assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, previewVersion))
                    .isInstanceOf(HappyGalleryException.class);
        }

        assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                .isEqualTo(SmartStoreStockSyncStatus.PENDING);
        stockSyncBatchUseCase.syncPendingStocks();
        assertThat(channelStock.get()).isEqualTo(9);
        assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                .isEqualTo(SmartStoreStockSyncStatus.SYNCED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("재등록 전 전송이 늦게 적용돼도 성공·실패 응답 뒤 최신 재고를 다시 맞춘다")
    void oldStockWriteAfterReregistration_requeuesCurrentStock(boolean success) {
        Long productId = readyStockMapping();
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger channelStock = new AtomicInteger(5);
        when(provider.sync(any())).thenAnswer(invocation -> {
            StockCommand command = invocation.getArgument(0);
            boolean previousGeneration = calls.incrementAndGet() == 1;
            if (previousGeneration) {
                inventoryService.deduct(productId, 2);
                smartStoreInventoryUseCase.deleteMapping(productId);
                smartStoreInventoryUseCase.saveMapping(productId, new SaveMappingCommand(123L, true, List.of()));
                assertThat(stockSyncBatchUseCase.syncPendingStocks().successCount()).isEqualTo(1);
                assertThat(channelStock.get()).isEqualTo(3);
            }
            channelStock.set(command.stockQuantity());
            return previousGeneration && !success
                    ? SyncResult.failure("외부 수량 반영 후 응답 실패") : SyncResult.completed();
        });

        stockSyncBatchUseCase.syncPendingStocks();
        assertThat(channelStock.get()).isEqualTo(5);
        var pending = stockSyncPort.findByProductId(productId).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(SmartStoreStockSyncStatus.PENDING);
        assertThat(pending.getAttemptCount()).isZero();
        assertThat(stockSyncBatchUseCase.syncPendingStocks().successCount()).isEqualTo(1);
        assertThat(channelStock.get()).isEqualTo(3);
        assertThat(calls.get()).isEqualTo(3);
        assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                .isEqualTo(SmartStoreStockSyncStatus.SYNCED);
    }

    @Test
    @DisplayName("원상품·옵션 연결 변경과 재등록은 이전 미리보기를 거절하고 수량만 바뀌면 최신 재고로 반영한다")
    void applyProduct_requiresCurrentMappingPreviewButUsesLatestQuantity() {
        var registered = productAdminUseCase.register(new SaveProductCommand(
                "주문제작 연동 상품", ProductType.MADE_TO_ORDER, null, 35000L, 5,
                null, null, "가죽 제품", null, 7, List.of(), List.of()));
        Long productId = registered.product().getId();
        Long variantId = registered.options().variants().getFirst().id();
        long productVersion = registered.product().getVersion();
        when(provider.isEnabled()).thenReturn(true);
        when(provider.getProduct(anyLong())).thenReturn(new ChannelProduct(33000L, "SALE", List.of()));
        smartStoreInventoryUseCase.saveMapping(productId, new SaveMappingCommand(
                123L, true, List.of(new VariantMapping(variantId, 11L))));

        for (SaveMappingCommand changed : List.of(
                new SaveMappingCommand(456L, true, List.of(new VariantMapping(variantId, 11L))),
                new SaveMappingCommand(456L, true, List.of(new VariantMapping(variantId, 12L))))) {
            String previous = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
            smartStoreInventoryUseCase.saveMapping(productId, changed);
            assertThat(productRepository.findById(productId).orElseThrow().getVersion()).isEqualTo(productVersion);
            assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, previous))
                    .isInstanceOf(HappyGalleryException.class).hasMessageContaining("최신 차이");
        }

        String beforeRecreation = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
        smartStoreInventoryUseCase.deleteMapping(productId);
        assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, beforeRecreation))
                .isInstanceOf(HappyGalleryException.class).hasMessageContaining("연결");
        smartStoreInventoryUseCase.saveMapping(productId, new SaveMappingCommand(
                456L, true, List.of(new VariantMapping(variantId, 12L))));
        assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, beforeRecreation))
                .isInstanceOf(HappyGalleryException.class).hasMessageContaining("최신 차이");
        verify(provider, never()).applyProduct(any());

        String current = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
        variantStockService.deductAll(List.of(new VariantAdjustment(variantId, 1)));
        assertThat(smartStoreInventoryUseCase.previewProduct(productId).previewVersion()).isEqualTo(current);
        when(provider.applyProduct(any())).thenReturn(SyncResult.completed());
        smartStoreInventoryUseCase.applyProduct(productId, current);
        verify(provider).applyProduct(argThat(command -> command.originProductNo().equals(456L)
                && command.options().size() == 1
                && command.options().getFirst().optionId().equals(12L)
                && command.options().getFirst().stockQuantity() == 4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAILED", "MORE_PAGES", "PROCESSING", "BACKLOG", "DISABLED"})
    @DisplayName("주문 수집이 실패·미완료·진행 중·비활성이면 자동·수동 재고 전송을 보류하고 완료 후 재개한다")
    void blocksWritesUntilOrderFeedCompletes(String situation) {
        Long productId = readyStockMapping();
        String preview = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
        LocalDateTime now = LocalDateTime.now(clock);
        switch (situation) {
            case "FAILED" -> when(orderProvider.fetchChanges(any(), any()))
                    .thenThrow(new IllegalStateException("주문 조회 실패"));
            case "MORE_PAGES" -> when(orderProvider.fetchChanges(any(), any()))
                    .thenReturn(new ChangePage(List.of(), new ChangeCursor(now.minusMinutes(1), "next")));
            case "PROCESSING" -> resetCursor(now.minusMinutes(1), now);
            case "BACKLOG" -> resetCursor(now.minusDays(3), null);
            case "DISABLED" -> when(orderProvider.isEnabled()).thenReturn(false);
            default -> throw new IllegalArgumentException(situation);
        }

        assertThat(stockSyncBatchUseCase.syncPendingStocks().failureCount()).isEqualTo(1);
        assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, preview))
                .isInstanceOf(HappyGalleryException.class).hasMessageContaining("주문 수집");
        verify(provider, never()).sync(any());
        verify(provider, never()).applyProduct(any());
        var pending = stockSyncPort.findByProductId(productId).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(SmartStoreStockSyncStatus.PENDING);
        assertThat(pending.getAttemptCount()).isZero();

        when(orderProvider.isEnabled()).thenReturn(true);
        doReturn(new ChangePage(List.of(), null)).when(orderProvider).fetchChanges(any(), any());
        resetCursor(LocalDateTime.now(clock).minusMinutes(1), null);
        when(provider.sync(any())).thenReturn(SyncResult.completed());
        when(provider.applyProduct(any())).thenReturn(SyncResult.completed());

        assertThat(stockSyncBatchUseCase.syncPendingStocks().successCount()).isEqualTo(1);
        smartStoreInventoryUseCase.applyProduct(productId, preview);
        verify(provider).sync(argThat(command -> command.stockQuantity() == 5));
        verify(provider).applyProduct(argThat(command -> command.stockQuantity() == 5));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("재고 미반영 주문이 있는 원상품만 보류하고 관리자 재처리 후 실제 남은 재고를 전송한다")
    void unappliedOrder_blocksOnlyItsProductUntilRetry(boolean mapped) {
        Long productId = readyStockMapping();
        String preview = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
        var other = productRepository.save(readyStockProduct("다른 연동 상품", 35000L));
        inventoryRepository.save(inventory(other, 3));
        smartStoreInventoryUseCase.saveMapping(other.getId(), new SaveMappingCommand(456L, true, List.of()));
        LocalDateTime now = LocalDateTime.now(clock);
        var order = new SmartStoreProductOrder(
                "pending-stock", "order-pending", 123L, null, "재고 미반영 주문", null,
                "PAYED", null, null, 2, 2, "PAYED", now.minusMinutes(1), now);
        if (mapped) order.mapTo(productId, null);
        order.requireAttention(mapped ? SmartStoreOrderAttentionReason.STOCK_SHORTAGE
                : SmartStoreOrderAttentionReason.MAPPING_REQUIRED);
        channelOrderRepository.save(order);
        when(provider.sync(any())).thenReturn(SyncResult.completed());

        assertThat(stockSyncBatchUseCase.syncPendingStocks().successCount()).isEqualTo(1);
        verify(provider).sync(argThat(command -> command.originProductNo().equals(456L)));
        verify(provider, never()).sync(argThat(command -> command.originProductNo().equals(123L)));
        assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, preview))
                .isInstanceOf(HappyGalleryException.class).hasMessageContaining("재고 미반영");
        verify(provider, never()).applyProduct(any());
        var deferred = stockSyncPort.findByProductId(productId).orElseThrow();
        assertThat(deferred.getStatus()).isEqualTo(SmartStoreStockSyncStatus.PENDING);
        assertThat(deferred.getAttemptCount()).isZero();
        assertThat(deferred.getNextAttemptAt()).isAfter(now);

        channelOrderUseCase.retryInventory("pending-stock");
        assertThat(stockSyncBatchUseCase.syncPendingStocks().successCount()).isEqualTo(1);
        verify(provider).sync(argThat(command -> command.originProductNo().equals(123L)
                && command.stockQuantity() == 3));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    @DisplayName("상품 반영 직전 수집한 주문 수량을 차감하고 품절로 바뀌면 새 미리보기를 요구한다")
    void collectedOrder_isDeductedBeforeManualAndAutomaticWrites(int soldQuantity) {
        Long productId = readyStockMapping();
        String preview = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
        LocalDateTime now = LocalDateTime.now(clock);
        when(orderProvider.fetchChanges(any(), any())).thenReturn(new ChangePage(
                List.of(new ProductOrderChange("new-paid", "PAYED", now)), null));
        when(orderProvider.fetchDetails(List.of("new-paid"))).thenReturn(List.of(new ProductOrderDetail(
                "new-paid", "new-order", 123L, null, "수집한 주문", null, null, "PAYED",
                null, null, null, null, soldQuantity, soldQuantity, now.minusMinutes(1), null,
                null, null, null, null, null, null, null, null, null, List.of())));
        when(provider.applyProduct(any())).thenReturn(SyncResult.completed());
        when(provider.sync(any())).thenReturn(SyncResult.completed());

        if (soldQuantity == 5) {
            assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, preview))
                    .isInstanceOf(HappyGalleryException.class).hasMessageContaining("최신 차이");
            verify(provider, never()).applyProduct(any());
            String refreshed = smartStoreInventoryUseCase.previewProduct(productId).previewVersion();
            smartStoreInventoryUseCase.applyProduct(productId, refreshed);
        } else {
            smartStoreInventoryUseCase.applyProduct(productId, preview);
        }

        assertThat(channelOrderRepository.findById("new-paid").orElseThrow().getInventoryAppliedQuantity())
                .isEqualTo(soldQuantity);
        verify(provider).applyProduct(argThat(command -> command.stockQuantity() == 5 - soldQuantity));
        assertThat(stockSyncBatchUseCase.syncPendingStocks().successCount()).isEqualTo(1);
        verify(provider).sync(argThat(command -> command.stockQuantity() == 5 - soldQuantity));
    }

    private Long readyStockMapping() {
        var product = productRepository.save(readyStockProduct("전송 보류 확인 상품", 35000L));
        inventoryRepository.save(inventory(product, 5));
        smartStoreInventoryUseCase.saveMapping(product.getId(), new SaveMappingCommand(123L, true, List.of()));
        when(provider.isEnabled()).thenReturn(true);
        when(provider.getProduct(anyLong())).thenReturn(new ChannelProduct(33000L, "SALE", List.of()));
        return product.getId();
    }

    private void resetCursor(LocalDateTime from, LocalDateTime processingStartedAt) {
        jdbcTemplate.update("""
                UPDATE smartstore_order_sync_state
                   SET last_changed_from = ?, more_sequence = NULL, processing_started_at = ?
                 WHERE id = 1
                """, from, processingStartedAt);
    }
}

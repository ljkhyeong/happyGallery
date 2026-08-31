package com.personal.happygallery.application.product;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
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
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean SmartStoreInventoryProvider provider;

    @AfterEach
    void tearDown() {
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
}

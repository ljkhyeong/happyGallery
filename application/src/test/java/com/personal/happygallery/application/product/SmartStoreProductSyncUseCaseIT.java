package com.personal.happygallery.application.product;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.SaveMappingCommand;
import com.personal.happygallery.application.product.port.in.SmartStoreStockSyncBatchUseCase;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.StockCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.SyncResult;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@UseCaseIT
class SmartStoreProductSyncUseCaseIT {

    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryService inventoryService;
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
            smartStoreInventoryUseCase.applyProduct(productId, product.getVersion());
        } else {
            assertThatThrownBy(() -> smartStoreInventoryUseCase.applyProduct(productId, product.getVersion()))
                    .isInstanceOf(HappyGalleryException.class);
        }

        assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                .isEqualTo(SmartStoreStockSyncStatus.PENDING);
        stockSyncBatchUseCase.syncPendingStocks();
        assertThat(channelStock.get()).isEqualTo(9);
        assertThat(stockSyncPort.findByProductId(productId).orElseThrow().getStatus())
                .isEqualTo(SmartStoreStockSyncStatus.SYNCED);
    }
}

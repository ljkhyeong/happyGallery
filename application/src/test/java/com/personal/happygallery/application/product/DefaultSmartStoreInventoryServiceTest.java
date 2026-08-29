package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.SmartStoreStockSyncTransactionService.ProductSyncSnapshot;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantReaderPort;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class DefaultSmartStoreInventoryServiceTest {

    @Test
    @DisplayName("미리보기 뒤 상품 버전이 바뀌면 스마트스토어에 이전 값을 반영하지 않는다")
    void applyProduct_rejectsStalePreviewVersion() {
        SmartStoreInventoryProvider provider = mock(SmartStoreInventoryProvider.class);
        SmartStoreStockSyncTransactionService transactionService =
                mock(SmartStoreStockSyncTransactionService.class);
        when(provider.isEnabled()).thenReturn(true);
        when(transactionService.productSnapshot(1L)).thenReturn(new ProductSyncSnapshot(
                1L, 4L, 123L, 35000L, "SALE", 3, List.of()));
        var service = service(provider, transactionService);

        assertThatThrownBy(() -> service.applyProduct(1L, 3L))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("최신 차이");
        verify(provider, never()).applyProduct(any());
    }

    private static DefaultSmartStoreInventoryService service(
            SmartStoreInventoryProvider provider,
            SmartStoreStockSyncTransactionService transactionService) {
        return new DefaultSmartStoreInventoryService(
                mock(ProductReaderPort.class),
                mock(ProductVariantReaderPort.class),
                mock(SmartStoreStockMappingPort.class),
                mock(SmartStoreStockSyncPort.class),
                mock(SmartStoreStockSyncQueuePort.class),
                provider,
                transactionService,
                Clock.systemUTC());
    }
}

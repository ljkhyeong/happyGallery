package com.personal.happygallery.application.product;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.product.SmartStoreStockSyncTransactionService.ClaimedStock;
import com.personal.happygallery.application.product.port.in.SmartStoreStockSyncBatchUseCase;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.SyncResult;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultSmartStoreStockSyncBatchService implements SmartStoreStockSyncBatchUseCase {

    private static final int BATCH_SIZE = 100;

    private final SmartStoreInventoryProvider inventoryProvider;
    private final SmartStoreStockSyncPort syncPort;
    private final SmartStoreStockSyncTransactionService transactionService;
    private final Clock clock;

    public DefaultSmartStoreStockSyncBatchService(
            SmartStoreInventoryProvider inventoryProvider,
            SmartStoreStockSyncPort syncPort,
            SmartStoreStockSyncTransactionService transactionService,
            Clock clock) {
        this.inventoryProvider = inventoryProvider;
        this.syncPort = syncPort;
        this.transactionService = transactionService;
        this.clock = clock;
    }

    @Override
    public BatchResult syncPendingStocks() {
        if (!inventoryProvider.isEnabled()) {
            return BatchResult.successOnly(0);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return BatchExecutor.execute(
                syncPort.findDueProductIds(now, now.minusMinutes(5), BATCH_SIZE),
                productId -> productId,
                this::sync,
                "스마트스토어 재고 동기화");
    }

    private boolean sync(Long productId) {
        LocalDateTime claimedAt = LocalDateTime.now(clock);
        Optional<ClaimedStock> claimed = transactionService.claim(productId, claimedAt);
        if (claimed.isEmpty()) {
            return false;
        }
        ClaimedStock stock = claimed.get();
        SyncResult result = stock.configurationError() == null
                ? inventoryProvider.sync(stock.command())
                : SyncResult.failure(stock.configurationError());
        transactionService.finish(
                productId,
                stock.generation(),
                stock.version(),
                result.success(),
                result.reason(),
                LocalDateTime.now(clock));
        if (!result.success()) {
            throw new SmartStoreStockSyncException(result.reason());
        }
        return true;
    }

    private static final class SmartStoreStockSyncException extends RuntimeException {
        private SmartStoreStockSyncException(String message) {
            super(message);
        }
    }
}

package com.personal.happygallery.application.product;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.SmartStoreOrderSyncBatchUseCase;
import com.personal.happygallery.application.product.SmartStoreStockSyncTransactionService.ClaimedStock;
import com.personal.happygallery.application.product.port.in.SmartStoreStockSyncBatchUseCase;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.SyncResult;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultSmartStoreStockSyncBatchService implements SmartStoreStockSyncBatchUseCase {

    private static final int BATCH_SIZE = 100;
    private static final Duration RECONCILIATION_INTERVAL = Duration.ofHours(24);

    private final SmartStoreInventoryProvider inventoryProvider;
    private final SmartStoreStockSyncPort syncPort;
    private final SmartStoreStockSyncTransactionService transactionService;
    private final SmartStoreOrderSyncBatchUseCase orderSyncUseCase;
    private final Clock clock;

    public DefaultSmartStoreStockSyncBatchService(
            SmartStoreInventoryProvider inventoryProvider,
            SmartStoreStockSyncPort syncPort,
            SmartStoreStockSyncTransactionService transactionService,
            SmartStoreOrderSyncBatchUseCase orderSyncUseCase,
            Clock clock) {
        this.inventoryProvider = inventoryProvider;
        this.syncPort = syncPort;
        this.transactionService = transactionService;
        this.orderSyncUseCase = orderSyncUseCase;
        this.clock = clock;
    }

    @Override
    public BatchResult syncPendingStocks() {
        if (!inventoryProvider.isEnabled()) {
            return BatchResult.successOnly(0);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        BatchResult reconciliation = requestReconciliation(now);
        List<Long> productIds = syncPort.findDueProductIds(now, now.minusMinutes(5), BATCH_SIZE);
        if (productIds.isEmpty()) {
            return reconciliation;
        }
        if (!orderSyncUseCase.synchronizeBeforeStock()) {
            return reconciliation.merge(
                    BatchResult.of(0, Map.of("스마트스토어 주문 수집 미완료로 재고 전송 보류", 1)));
        }
        return reconciliation.merge(BatchExecutor.execute(
                productIds,
                productId -> productId,
                this::sync,
                "스마트스토어 재고 동기화"));
    }

    private BatchResult requestReconciliation(LocalDateTime now) {
        LocalDateTime syncedBefore = now.minus(RECONCILIATION_INTERVAL);
        List<Long> productIds = syncPort.findReconciliationProductIds(syncedBefore, BATCH_SIZE);
        BatchResult result = BatchExecutor.execute(
                productIds,
                productId -> productId,
                productId -> transactionService.requestReconciliation(productId, syncedBefore, now),
                "스마트스토어 재고 재검증 요청");
        return BatchResult.of(0, result.failureReasons());
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

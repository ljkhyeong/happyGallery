package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.SmartStoreOrderSyncStateService.ClaimedCursor;
import com.personal.happygallery.application.order.port.in.SmartStoreOrderSyncBatchUseCase;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangePage;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderChange;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ProductOrderDetail;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultSmartStoreOrderSyncBatchService implements SmartStoreOrderSyncBatchUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultSmartStoreOrderSyncBatchService.class);

    private final SmartStoreOrderProvider orderProvider;
    private final SmartStoreOrderSyncStateService stateService;
    private final SmartStoreOrderTransactionService transactionService;
    private final Clock clock;

    public DefaultSmartStoreOrderSyncBatchService(
            SmartStoreOrderProvider orderProvider,
            SmartStoreOrderSyncStateService stateService,
            SmartStoreOrderTransactionService transactionService,
            Clock clock) {
        this.orderProvider = orderProvider;
        this.stateService = stateService;
        this.transactionService = transactionService;
        this.clock = clock;
    }

    @Override
    public BatchResult syncChangedOrders() {
        return synchronize().batchResult();
    }

    @Override
    public boolean synchronizeBeforeStock() {
        return synchronize().caughtUp();
    }

    private SyncOutcome synchronize() {
        if (!orderProvider.isEnabled()) {
            stateService.skipDisabledPeriod();
            return new SyncOutcome(BatchResult.successOnly(0), false);
        }
        ClaimedCursor claimed = stateService.claim().orElse(null);
        if (claimed == null) {
            return new SyncOutcome(BatchResult.successOnly(0), false);
        }
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime changedTo = min(claimed.cursor().changedFrom().plusHours(24), now);
            if (!claimed.cursor().changedFrom().isBefore(changedTo)) {
                boolean completed = stateService.complete(claimed, new ChangeCursor(changedTo, null));
                return new SyncOutcome(BatchResult.successOnly(0),
                        completed && claimed.cursor().moreSequence() == null);
            }

            ChangePage page = orderProvider.fetchChanges(claimed.cursor(), changedTo);
            Map<String, ProductOrderChange> latestChanges = latestChanges(page.changes());
            List<ProductOrderDetail> details = orderProvider.fetchDetails(
                    List.copyOf(latestChanges.keySet()));
            Map<String, ProductOrderDetail> detailsById = new LinkedHashMap<>();
            details.forEach(detail -> detailsById.put(detail.productOrderId(), detail));
            if (!detailsById.keySet().containsAll(latestChanges.keySet())) {
                throw new IllegalStateException("스마트스토어 상품 주문 상세 일부를 받지 못했습니다.");
            }
            latestChanges.forEach((productOrderId, change) ->
                    transactionService.synchronize(detailsById.get(productOrderId), change));

            ChangeCursor next = page.nextCursor() == null
                    ? new ChangeCursor(changedTo, null)
                    : page.nextCursor();
            boolean completed = stateService.complete(claimed, next);
            return new SyncOutcome(BatchResult.successOnly(latestChanges.size()),
                    completed && page.nextCursor() == null && changedTo.equals(now));
        } catch (Exception exception) {
            stateService.release(claimed);
            log.warn("스마트스토어 주문 동기화 실패 [type={}]", exception.getClass().getSimpleName());
            return new SyncOutcome(BatchResult.of(0, Map.of("스마트스토어 주문을 가져오지 못함", 1)), false);
        }
    }

    private static Map<String, ProductOrderChange> latestChanges(List<ProductOrderChange> changes) {
        Map<String, ProductOrderChange> latest = new LinkedHashMap<>();
        changes.forEach(change -> latest.put(change.productOrderId(), change));
        return latest;
    }

    private static LocalDateTime min(LocalDateTime left, LocalDateTime right) {
        return left.isBefore(right) ? left : right;
    }

    private record SyncOutcome(BatchResult batchResult, boolean caughtUp) {}
}

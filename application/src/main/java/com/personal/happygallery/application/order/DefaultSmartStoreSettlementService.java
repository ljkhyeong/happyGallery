package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.SmartStoreSettlementUseCase;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import com.personal.happygallery.domain.order.SmartStoreSettlementStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSmartStoreSettlementService implements SmartStoreSettlementUseCase {

    private static final int MAX_DATES_PER_RUN = 31;

    private final SmartStoreSettlementProvider provider;
    private final SmartStoreSettlementTransactionService transactionService;
    private final SmartStoreSettlementPort settlementPort;
    private final SmartStoreSettlementSyncStateService syncStateService;
    private final Clock clock;

    public DefaultSmartStoreSettlementService(
            SmartStoreSettlementProvider provider,
            SmartStoreSettlementTransactionService transactionService,
            SmartStoreSettlementPort settlementPort,
            SmartStoreSettlementSyncStateService syncStateService,
            Clock clock) {
        this.provider = provider;
        this.transactionService = transactionService;
        this.settlementPort = settlementPort;
        this.syncStateService = syncStateService;
        this.clock = clock;
    }

    @Override
    public BatchResult synchronizeRecent() {
        if (!provider.isEnabled()) {
            return BatchResult.successOnly(0);
        }
        BatchResult result = BatchResult.successOnly(0);
        LocalDate today = LocalDate.now(clock);
        for (int index = 0; index < MAX_DATES_PER_RUN; index++) {
            var claimed = syncStateService.claim();
            if (claimed.isEmpty()) {
                break;
            }
            var date = claimed.get();
            try {
                result = result.merge(synchronize(date.payDate(), date.payDate()));
                syncStateService.complete(date);
            } catch (RuntimeException exception) {
                syncStateService.release(date);
                throw exception;
            }
            if (date.payDate().equals(today)) {
                break;
            }
        }
        return result;
    }

    @Override
    public BatchResult synchronize(LocalDate from, LocalDate to) {
        if (!provider.isEnabled()) {
            return BatchResult.successOnly(0);
        }
        if (to.isBefore(from) || from.plusDays(MAX_DATES_PER_RUN - 1L).isBefore(to)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "정산 재동기화 기간은 1일부터 31일까지 지정할 수 있습니다.");
        }
        int matched = 0;
        Map<String, Integer> issues = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (var item : provider.findByPayDate(date)) {
                SmartStoreSettlementStatus status = transactionService.reconcile(item);
                if (status == SmartStoreSettlementStatus.MATCHED
                        || status == SmartStoreSettlementStatus.NOT_APPLICABLE) {
                    matched++;
                } else {
                    issues.merge(status.name(), 1, Integer::sum);
                }
            }
        }
        return BatchResult.of(matched, issues);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmartStoreSettlementEntry> findIssues(int limit) {
        return settlementPort.findIssues(limit);
    }
}

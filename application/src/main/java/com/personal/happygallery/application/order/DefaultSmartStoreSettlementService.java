package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.order.port.in.SmartStoreSettlementUseCase;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementPort;
import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider;
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

    private final SmartStoreSettlementProvider provider;
    private final SmartStoreSettlementTransactionService transactionService;
    private final SmartStoreSettlementPort settlementPort;
    private final Clock clock;

    public DefaultSmartStoreSettlementService(
            SmartStoreSettlementProvider provider,
            SmartStoreSettlementTransactionService transactionService,
            SmartStoreSettlementPort settlementPort,
            Clock clock) {
        this.provider = provider;
        this.transactionService = transactionService;
        this.settlementPort = settlementPort;
        this.clock = clock;
    }

    @Override
    public BatchResult synchronizeRecent() {
        if (!provider.isEnabled()) {
            return BatchResult.successOnly(0);
        }
        LocalDate today = LocalDate.now(clock);
        int matched = 0;
        Map<String, Integer> issues = new LinkedHashMap<>();
        for (LocalDate date = today.minusDays(6); !date.isAfter(today); date = date.plusDays(1)) {
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

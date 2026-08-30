package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.payment.port.in.PaymentSettlementAdminUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentSettlementSyncUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentSettlementPort;
import com.personal.happygallery.application.payment.port.out.PaymentSettlementProvider;
import com.personal.happygallery.domain.payment.PaymentSettlement;
import com.personal.happygallery.domain.payment.PaymentSettlementStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPaymentSettlementService
        implements PaymentSettlementSyncUseCase, PaymentSettlementAdminUseCase {

    private final PaymentSettlementProvider provider;
    private final PaymentSettlementTransactionService transactionService;
    private final PaymentSettlementPort settlementPort;
    private final Clock clock;

    public DefaultPaymentSettlementService(
            PaymentSettlementProvider provider,
            PaymentSettlementTransactionService transactionService,
            PaymentSettlementPort settlementPort,
            Clock clock) {
        this.provider = provider;
        this.transactionService = transactionService;
        this.settlementPort = settlementPort;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public BatchResult synchronizeRecent() {
        LocalDate endDate = LocalDate.now(clock).minusDays(1);
        LocalDate startDate = endDate.minusDays(6);
        int matchedCount = 0;
        Map<String, Integer> issues = new LinkedHashMap<>();
        for (var item : provider.findSettlements(startDate, endDate)) {
            PaymentSettlementStatus status = transactionService.reconcile(item);
            if (status == PaymentSettlementStatus.MATCHED) {
                matchedCount++;
            } else {
                issues.merge(status.name(), 1, Integer::sum);
            }
        }
        return BatchResult.of(matchedCount, issues);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSettlement> findIssues(int limit) {
        return settlementPort.findIssues(limit);
    }
}

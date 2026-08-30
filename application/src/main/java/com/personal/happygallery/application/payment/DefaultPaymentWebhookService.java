package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentWebhookBatchUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentWebhookUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentWebhookReceiptPort;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPaymentWebhookService
        implements PaymentWebhookUseCase, PaymentWebhookBatchUseCase {

    static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";
    private static final int BATCH_LIMIT = 20;
    private static final int CLAIM_STALE_MINUTES = 1;

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentWebhookReceiptPort receiptPort;
    private final PaymentWebhookReceiptTransactionService receiptTransactionService;
    private final PaymentReconciliationAdminUseCase reconciliationUseCase;
    private final Clock clock;

    public DefaultPaymentWebhookService(
            PaymentAttemptReaderPort attemptReader,
            PaymentWebhookReceiptPort receiptPort,
            PaymentWebhookReceiptTransactionService receiptTransactionService,
            PaymentReconciliationAdminUseCase reconciliationUseCase,
            Clock clock) {
        this.attemptReader = attemptReader;
        this.receiptPort = receiptPort;
        this.receiptTransactionService = receiptTransactionService;
        this.reconciliationUseCase = reconciliationUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void receive(String transmissionId, String eventType, String orderId) {
        if (!PAYMENT_STATUS_CHANGED.equals(eventType)) {
            return;
        }
        attemptReader.findByOrderIdExternal(orderId).ifPresent(attempt ->
                receiptPort.insertIfAbsent(
                        transmissionId,
                        attempt.getId(),
                        eventType,
                        LocalDateTime.now(clock)));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public BatchResult processPendingReceipts() {
        LocalDateTime staleBefore = LocalDateTime.now(clock).minusMinutes(CLAIM_STALE_MINUTES);
        return BatchExecutor.execute(
                receiptPort.findPendingIds(staleBefore, BATCH_LIMIT),
                receiptId -> receiptId,
                receiptId -> processOne(receiptId, staleBefore),
                "결제 웹훅");
    }

    private boolean processOne(Long receiptId, LocalDateTime staleBefore) {
        return receiptTransactionService.claim(receiptId, staleBefore)
                .map(attemptId -> {
                    reconciliationUseCase.reconcile(attemptId);
                    receiptTransactionService.complete(receiptId);
                    return true;
                })
                .orElse(false);
    }
}

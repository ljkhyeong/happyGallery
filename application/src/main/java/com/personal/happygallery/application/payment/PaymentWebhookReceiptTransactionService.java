package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentWebhookReceiptPort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentWebhookReceipt;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentWebhookReceiptTransactionService {

    private final PaymentWebhookReceiptPort receiptPort;
    private final PaymentAttemptReaderPort attemptReader;
    private final Clock clock;

    PaymentWebhookReceiptTransactionService(
            PaymentWebhookReceiptPort receiptPort,
            PaymentAttemptReaderPort attemptReader,
            Clock clock) {
        this.receiptPort = receiptPort;
        this.attemptReader = attemptReader;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Optional<Long> claim(Long receiptId, LocalDateTime staleBefore) {
        PaymentWebhookReceipt receipt = receiptPort.findByIdForUpdate(receiptId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (receipt == null || !receipt.claim(now, staleBefore)) {
            return Optional.empty();
        }
        PaymentAttempt attempt = attemptReader.findById(receipt.getPaymentAttemptId()).orElse(null);
        if (attempt == null || attempt.getStatus() != PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
            receipt.markProcessed(now);
            receiptPort.save(receipt);
            return Optional.empty();
        }
        receiptPort.save(receipt);
        return Optional.of(attempt.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void complete(Long receiptId) {
        receiptPort.findByIdForUpdate(receiptId).ifPresent(receipt -> {
            receipt.markProcessed(LocalDateTime.now(clock));
            receiptPort.save(receipt);
        });
    }
}

package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentWebhookReceipt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentWebhookReceiptPort {

    int insertIfAbsent(
            String transmissionId,
            Long paymentAttemptId,
            String eventType,
            LocalDateTime receivedAt);

    List<Long> findPendingIds(LocalDateTime processingStaleBefore, int limit);

    Optional<PaymentWebhookReceipt> findByIdForUpdate(Long id);

    <S extends PaymentWebhookReceipt> S save(S receipt);
}

package com.personal.happygallery.adapter.out.persistence.payment;

import com.personal.happygallery.application.payment.port.out.PaymentWebhookReceiptPort;
import com.personal.happygallery.domain.payment.PaymentWebhookReceipt;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentWebhookReceiptRepository
        extends JpaRepository<PaymentWebhookReceipt, Long>, PaymentWebhookReceiptPort {

    @Override
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO payment_webhook_receipts (
                transmission_id, payment_attempt_id, event_type, received_at, version
            ) VALUES (
                :transmissionId, :paymentAttemptId, :eventType, :receivedAt, 0
            )
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("transmissionId") String transmissionId,
            @Param("paymentAttemptId") Long paymentAttemptId,
            @Param("eventType") String eventType,
            @Param("receivedAt") LocalDateTime receivedAt);

    @Override
    @Query(value = """
            SELECT id
            FROM payment_webhook_receipts
            WHERE processed_at IS NULL
              AND (processing_at IS NULL OR processing_at <= :processingStaleBefore)
            ORDER BY id
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findPendingIds(
            @Param("processingStaleBefore") LocalDateTime processingStaleBefore,
            @Param("limit") int limit);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select receipt from PaymentWebhookReceipt receipt where receipt.id = :id")
    Optional<PaymentWebhookReceipt> findByIdForUpdate(@Param("id") Long id);

    @Override
    <S extends PaymentWebhookReceipt> S save(S receipt);
}

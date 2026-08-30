package com.personal.happygallery.domain.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_webhook_receipts")
public class PaymentWebhookReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transmission_id", nullable = false, unique = true, length = 100)
    private String transmissionId;

    @Column(name = "payment_attempt_id", nullable = false)
    private Long paymentAttemptId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PaymentWebhookReceipt() {}

    public boolean claim(LocalDateTime now, LocalDateTime staleBefore) {
        if (processedAt != null || (processingAt != null && processingAt.isAfter(staleBefore))) {
            return false;
        }
        processingAt = now;
        return true;
    }

    public void markProcessed(LocalDateTime now) {
        processedAt = now;
        processingAt = null;
    }

    public Long getId() { return id; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
}

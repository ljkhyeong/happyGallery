package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_restock_alerts")
public class RestockAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "product_variant_id")
    private Long productVariantId;
    @Column(name = "option_label", nullable = false, length = 1000)
    private String optionLabel;
    @Column(name = "active_key", length = 100)
    private String activeKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RestockAlertStatus status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
    @Version
    private long version;

    protected RestockAlert() {}

    public RestockAlert(Long userId, Long productId, Long productVariantId, String optionLabel, LocalDateTime now) {
        this.userId = userId;
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.optionLabel = optionLabel;
        this.createdAt = now;
        this.status = RestockAlertStatus.WAITING;
        this.activeKey = activeKey(userId, productId, productVariantId);
    }

    public static String activeKey(Long userId, Long productId, Long variantId) {
        return userId + ":" + productId + ":" + (variantId == null ? 0 : variantId);
    }

    public boolean isActive() { return status == RestockAlertStatus.WAITING || status == RestockAlertStatus.QUEUED; }
    public void markQueued() { if (isActive()) status = RestockAlertStatus.QUEUED; }
    public void markNotified(LocalDateTime now) {
        if (!isActive()) return;
        status = RestockAlertStatus.NOTIFIED;
        activeKey = null;
        notifiedAt = now;
    }
    public void cancel(LocalDateTime now) {
        if (!isActive()) return;
        status = RestockAlertStatus.CANCELED;
        activeKey = null;
        canceledAt = now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public Long getProductVariantId() { return productVariantId; }
    public String getOptionLabel() { return optionLabel; }
    public RestockAlertStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
}

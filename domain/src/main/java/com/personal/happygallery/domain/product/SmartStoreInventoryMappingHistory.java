package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "smartstore_inventory_mapping_history")
public class SmartStoreInventoryMappingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmartStoreInventoryMappingAction action;

    @Column(name = "previous_origin_product_no")
    private Long previousOriginProductNo;

    @Column(name = "next_origin_product_no")
    private Long nextOriginProductNo;

    @Column(name = "previous_enabled")
    private Boolean previousEnabled;

    @Column(name = "next_enabled")
    private Boolean nextEnabled;

    @Column(name = "previous_option_mappings", columnDefinition = "TEXT")
    private String previousOptionMappings;

    @Column(name = "next_option_mappings", columnDefinition = "TEXT")
    private String nextOptionMappings;

    @Column(name = "previous_mapping_version")
    private Long previousMappingVersion;

    @Column(name = "next_mapping_version")
    private Long nextMappingVersion;

    @Column(name = "previous_origin_confirmed", nullable = false)
    private boolean previousOriginConfirmed;

    @Column(name = "changed_by_admin_id")
    private Long changedByAdminId;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected SmartStoreInventoryMappingHistory() {}

    public SmartStoreInventoryMappingHistory(
            Long productId,
            SmartStoreInventoryMappingAction action,
            Long previousOriginProductNo,
            Long nextOriginProductNo,
            Boolean previousEnabled,
            Boolean nextEnabled,
            String previousOptionMappings,
            String nextOptionMappings,
            Long previousMappingVersion,
            Long nextMappingVersion,
            boolean previousOriginConfirmed,
            Long changedByAdminId,
            String changedBy,
            LocalDateTime changedAt) {
        this.productId = productId;
        this.action = action;
        this.previousOriginProductNo = previousOriginProductNo;
        this.nextOriginProductNo = nextOriginProductNo;
        this.previousEnabled = previousEnabled;
        this.nextEnabled = nextEnabled;
        this.previousOptionMappings = previousOptionMappings;
        this.nextOptionMappings = nextOptionMappings;
        this.previousMappingVersion = previousMappingVersion;
        this.nextMappingVersion = nextMappingVersion;
        this.previousOriginConfirmed = previousOriginConfirmed;
        this.changedByAdminId = changedByAdminId;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public SmartStoreInventoryMappingAction getAction() { return action; }
    public Long getPreviousOriginProductNo() { return previousOriginProductNo; }
    public Long getNextOriginProductNo() { return nextOriginProductNo; }
    public Boolean getPreviousEnabled() { return previousEnabled; }
    public Boolean getNextEnabled() { return nextEnabled; }
    public String getPreviousOptionMappings() { return previousOptionMappings; }
    public String getNextOptionMappings() { return nextOptionMappings; }
    public Long getPreviousMappingVersion() { return previousMappingVersion; }
    public Long getNextMappingVersion() { return nextMappingVersion; }
    public boolean isPreviousOriginConfirmed() { return previousOriginConfirmed; }
    public Long getChangedByAdminId() { return changedByAdminId; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}

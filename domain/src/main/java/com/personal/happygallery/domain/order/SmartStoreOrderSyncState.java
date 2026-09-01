package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "smartstore_order_sync_state")
public class SmartStoreOrderSyncState {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "last_changed_from", nullable = false)
    private LocalDateTime lastChangedFrom;

    @Column(name = "more_sequence", length = 100)
    private String moreSequence;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "integration_enabled")
    private Boolean integrationEnabled;

    @Column(name = "pending_activation_from")
    private LocalDateTime pendingActivationFrom;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected SmartStoreOrderSyncState() {}

    public void recordEnabledStart(LocalDateTime startedAt) {
        if (integrationEnabled == null) {
            integrationEnabled = true;
            return;
        }
        if (Boolean.FALSE.equals(integrationEnabled)
                && (pendingActivationFrom == null || startedAt.isBefore(pendingActivationFrom))) {
            pendingActivationFrom = startedAt;
        }
    }

    public void disable() {
        integrationEnabled = false;
        pendingActivationFrom = null;
    }

    public boolean claimEnabled(LocalDateTime now, LocalDateTime staleBefore) {
        if (!claim(now, staleBefore)) {
            return false;
        }
        if (Boolean.FALSE.equals(integrationEnabled)) {
            lastChangedFrom = pendingActivationFrom;
            moreSequence = null;
            integrationEnabled = true;
            pendingActivationFrom = null;
        }
        return true;
    }

    public boolean claim(LocalDateTime now, LocalDateTime staleBefore) {
        if (processingStartedAt != null && processingStartedAt.isAfter(staleBefore)) {
            return false;
        }
        processingStartedAt = now;
        return true;
    }

    public void complete(LocalDateTime nextFrom, String nextSequence) {
        this.lastChangedFrom = nextFrom;
        this.moreSequence = nextSequence;
        this.processingStartedAt = null;
    }

    public void release() {
        this.processingStartedAt = null;
    }

    public Long getId() { return id; }
    public LocalDateTime getLastChangedFrom() { return lastChangedFrom; }
    public String getMoreSequence() { return moreSequence; }
    public LocalDateTime getProcessingStartedAt() { return processingStartedAt; }
    public Boolean getIntegrationEnabled() { return integrationEnabled; }
    public LocalDateTime getPendingActivationFrom() { return pendingActivationFrom; }
    public long getVersion() { return version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

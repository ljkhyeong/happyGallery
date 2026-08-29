package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "smartstore_settlement_sync_state")
public class SmartStoreSettlementSyncState {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "next_pay_date", nullable = false)
    private LocalDate nextPayDate;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    protected SmartStoreSettlementSyncState() {}

    public boolean claim(LocalDateTime now, LocalDateTime staleBefore) {
        if (processingStartedAt != null && processingStartedAt.isAfter(staleBefore)) {
            return false;
        }
        processingStartedAt = now;
        return true;
    }

    public LocalDate dateToProcess(LocalDate today) {
        return nextPayDate.isAfter(today) ? today : nextPayDate;
    }

    public void complete(LocalDate processedDate) {
        if (processedDate.equals(nextPayDate)) {
            nextPayDate = nextPayDate.plusDays(1);
        }
        processingStartedAt = null;
    }

    public void release() {
        processingStartedAt = null;
    }

    public LocalDateTime getProcessingStartedAt() { return processingStartedAt; }
}

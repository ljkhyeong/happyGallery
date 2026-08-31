package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "smartstore_stock_syncs")
public class SmartStoreStockSync {

    public static final int MAX_ATTEMPTS = 10;

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "request_version", nullable = false)
    private long requestVersion;

    @Column(nullable = false, length = 36)
    private String generation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmartStoreStockSyncStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected SmartStoreStockSync() {}

    public SmartStoreStockSync(Long productId, LocalDateTime now) {
        if (productId == null || now == null) {
            throw new IllegalArgumentException("스마트스토어 재고 동기화 대상과 요청 시각은 필수입니다.");
        }
        this.productId = productId;
        this.requestVersion = 1;
        this.generation = UUID.randomUUID().toString();
        this.status = SmartStoreStockSyncStatus.PENDING;
        this.nextAttemptAt = now;
    }

    public void request(LocalDateTime now) {
        requestVersion = Math.addExact(requestVersion, 1);
        status = SmartStoreStockSyncStatus.PENDING;
        attemptCount = 0;
        nextAttemptAt = now;
        processingStartedAt = null;
        lastError = null;
    }

    public long claim(LocalDateTime now, LocalDateTime staleBefore) {
        boolean staleProcessing = status == SmartStoreStockSyncStatus.PROCESSING
                && processingStartedAt != null
                && !processingStartedAt.isAfter(staleBefore);
        if (status != SmartStoreStockSyncStatus.PENDING && !staleProcessing) {
            throw new IllegalStateException("대기 중인 스마트스토어 재고만 선점할 수 있습니다.");
        }
        status = SmartStoreStockSyncStatus.PROCESSING;
        processingStartedAt = now;
        return requestVersion;
    }

    /** 미반영 채널 주문을 기다리는 동안 실패 횟수를 늘리지 않고 다른 상품에 전송 순서를 넘긴다. */
    public void postponeForUnappliedOrder(LocalDateTime now) {
        status = SmartStoreStockSyncStatus.PENDING;
        nextAttemptAt = now.plusMinutes(1);
        processingStartedAt = null;
    }

    public void complete(String claimedGeneration, long claimedVersion, LocalDateTime now) {
        if (!generation.equals(claimedGeneration)) {
            requestAfterPreviousGeneration(now);
            return;
        }
        if (requestVersion == claimedVersion) {
            status = SmartStoreStockSyncStatus.SYNCED;
            attemptCount = 0;
            lastError = null;
            syncedAt = now;
            processingStartedAt = null;
            return;
        }
        status = SmartStoreStockSyncStatus.PENDING;
        nextAttemptAt = now;
        processingStartedAt = null;
    }

    public void fail(String claimedGeneration, long claimedVersion, String reason, LocalDateTime now) {
        if (!generation.equals(claimedGeneration)) {
            requestAfterPreviousGeneration(now);
            return;
        }
        if (requestVersion != claimedVersion) {
            status = SmartStoreStockSyncStatus.PENDING;
            nextAttemptAt = now;
            processingStartedAt = null;
            return;
        }
        attemptCount++;
        lastError = trim(reason);
        if (attemptCount >= MAX_ATTEMPTS) {
            status = SmartStoreStockSyncStatus.FAILED;
            processingStartedAt = null;
            return;
        }
        status = SmartStoreStockSyncStatus.PENDING;
        nextAttemptAt = now.plusMinutes(Math.min(30, 1L << Math.min(attemptCount - 1, 5)));
        processingStartedAt = null;
    }

    /** 이미 전송했거나 전송 중이면 다시 요청하고, 대기·최종 실패의 재시도 정책은 유지한다. */
    private void requestAfterPreviousGeneration(LocalDateTime now) {
        if (status == SmartStoreStockSyncStatus.PROCESSING || status == SmartStoreStockSyncStatus.SYNCED) {
            request(now);
        }
    }

    private static String trim(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= 500 ? reason : reason.substring(0, 500);
    }

    public Long getProductId() { return productId; }
    public long getRequestVersion() { return requestVersion; }
    public String getGeneration() { return generation; }
    public SmartStoreStockSyncStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public LocalDateTime getProcessingStartedAt() { return processingStartedAt; }
    public String getLastError() { return lastError; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
    public long getRowVersion() { return rowVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

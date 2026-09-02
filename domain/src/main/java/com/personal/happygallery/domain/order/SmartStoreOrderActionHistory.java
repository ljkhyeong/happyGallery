package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "smartstore_order_action_history")
public class SmartStoreOrderActionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_order_id", nullable = false, length = 30)
    private String productOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SmartStoreOrderAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmartStoreOrderActionStatus status;

    @Column(name = "request_summary", columnDefinition = "TEXT")
    private String requestSummary;

    @Column(name = "result_code", length = 100)
    private String resultCode;

    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    @Column(name = "changed_by_admin_id")
    private Long changedByAdminId;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected SmartStoreOrderActionHistory() {}

    public SmartStoreOrderActionHistory(
            String productOrderId,
            SmartStoreOrderAction action,
            String requestSummary,
            Long changedByAdminId,
            String changedBy,
            LocalDateTime requestedAt) {
        this.productOrderId = requireText(productOrderId, "상품 주문 번호");
        this.action = Objects.requireNonNull(action, "처리 종류는 필수입니다.");
        this.status = SmartStoreOrderActionStatus.REQUESTED;
        this.requestSummary = optionalText(requestSummary);
        this.changedByAdminId = changedByAdminId;
        this.changedBy = requireText(changedBy, "처리자");
        this.requestedAt = Objects.requireNonNull(requestedAt, "요청 시각은 필수입니다.");
    }

    public void succeed(LocalDateTime completedAt) {
        complete(SmartStoreOrderActionStatus.SUCCEEDED, null, null, completedAt);
    }

    public void reject(String resultCode, String resultMessage, LocalDateTime completedAt) {
        complete(SmartStoreOrderActionStatus.REJECTED, resultCode, resultMessage, completedAt);
    }

    public void markResultUnknown(String resultMessage, LocalDateTime completedAt) {
        complete(SmartStoreOrderActionStatus.RESULT_UNKNOWN, "RESULT_UNKNOWN", resultMessage, completedAt);
    }

    private void complete(
            SmartStoreOrderActionStatus nextStatus,
            String resultCode,
            String resultMessage,
            LocalDateTime completedAt) {
        if (status != SmartStoreOrderActionStatus.REQUESTED) {
            return;
        }
        this.status = nextStatus;
        this.resultCode = optionalText(resultCode);
        this.resultMessage = truncate(optionalText(resultMessage), 1000);
        this.completedAt = Objects.requireNonNull(completedAt, "완료 시각은 필수입니다.");
    }

    private static String requireText(String value, String field) {
        String normalized = optionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + "은 필수입니다.");
        }
        return normalized;
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public Long getId() { return id; }
    public String getProductOrderId() { return productOrderId; }
    public SmartStoreOrderAction getAction() { return action; }
    public SmartStoreOrderActionStatus getStatus() { return status; }
    public String getRequestSummary() { return requestSummary; }
    public String getResultCode() { return resultCode; }
    public String getResultMessage() { return resultMessage; }
    public Long getChangedByAdminId() { return changedByAdminId; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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
@Table(name = "order_claims")
public class OrderClaim {

    public static final int MAX_REASON_LENGTH = 1000;
    public static final int MAX_ADMIN_NOTE_LENGTH = 1000;
    public static final int MAX_DELIVERY_VALUE_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 30)
    private OrderClaimType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_resolution", nullable = false, length = 30)
    private OrderClaimResolution requestedResolution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderClaimStatus status;

    @Column(name = "customer_reason", nullable = false, length = MAX_REASON_LENGTH)
    private String customerReason;

    @Column(name = "admin_note", length = MAX_ADMIN_NOTE_LENGTH)
    private String adminNote;

    @Column(name = "resolved_by_admin_id")
    private Long resolvedByAdminId;

    @Column(name = "completed_by_admin_id")
    private Long completedByAdminId;

    @Column(name = "replacement_carrier", length = MAX_DELIVERY_VALUE_LENGTH)
    private String replacementCarrier;

    @Column(name = "replacement_tracking_number", length = MAX_DELIVERY_VALUE_LENGTH)
    private String replacementTrackingNumber;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected OrderClaim() {}

    private OrderClaim(Long orderId,
                       OrderClaimType type,
                       OrderClaimResolution requestedResolution,
                       String customerReason,
                       LocalDateTime requestedAt) {
        if (orderId == null || type == null || requestedResolution == null || requestedAt == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "클레임 필수값이 누락되었습니다.");
        }
        this.orderId = orderId;
        this.type = type;
        this.requestedResolution = requestedResolution;
        this.customerReason = requireText(customerReason, MAX_REASON_LENGTH, "클레임 사유");
        this.requestedAt = requestedAt;
        this.status = OrderClaimStatus.REQUESTED;
    }

    public static OrderClaim request(Long orderId,
                                     OrderClaimType type,
                                     OrderClaimResolution requestedResolution,
                                     String customerReason,
                                     LocalDateTime requestedAt) {
        return new OrderClaim(orderId, type, requestedResolution, customerReason, requestedAt);
    }

    public void approveRefund(Long adminId, String adminNote, LocalDateTime resolvedAt) {
        requireRequested();
        if (requestedResolution != OrderClaimResolution.REFUND) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "환불을 요청한 클레임만 환불 승인할 수 있습니다.");
        }
        this.adminNote = optionalText(adminNote, MAX_ADMIN_NOTE_LENGTH, "관리자 메모");
        this.resolvedByAdminId = requireAdminId(adminId);
        this.resolvedAt = resolvedAt;
        this.status = OrderClaimStatus.REFUND_REQUESTED;
    }

    public void approveExchange(Long adminId, String adminNote, LocalDateTime resolvedAt) {
        requireRequested();
        if (requestedResolution != OrderClaimResolution.EXCHANGE) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "교환을 요청한 클레임만 교환 승인할 수 있습니다.");
        }
        this.adminNote = optionalText(adminNote, MAX_ADMIN_NOTE_LENGTH, "관리자 메모");
        this.resolvedByAdminId = requireAdminId(adminId);
        this.resolvedAt = resolvedAt;
        this.status = OrderClaimStatus.EXCHANGE_APPROVED;
    }

    public void reject(Long adminId, String adminNote, LocalDateTime resolvedAt) {
        requireRequested();
        this.adminNote = requireText(adminNote, MAX_ADMIN_NOTE_LENGTH, "거절 사유");
        this.resolvedByAdminId = requireAdminId(adminId);
        this.completedByAdminId = this.resolvedByAdminId;
        this.resolvedAt = resolvedAt;
        this.completedAt = resolvedAt;
        this.status = OrderClaimStatus.REJECTED;
    }

    public void completeRefund(LocalDateTime completedAt) {
        if (status == OrderClaimStatus.COMPLETED) {
            return;
        }
        if (status != OrderClaimStatus.REFUND_REQUESTED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "환불 요청 상태의 클레임만 완료할 수 있습니다.");
        }
        this.completedAt = completedAt;
        this.status = OrderClaimStatus.COMPLETED;
    }

    public void completeExchange(Long adminId, String carrier, String trackingNumber, String adminNote,
                                 LocalDateTime completedAt) {
        if (status != OrderClaimStatus.EXCHANGE_APPROVED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "교환 승인 상태의 클레임만 완료할 수 있습니다.");
        }
        this.replacementCarrier = requireText(carrier, MAX_DELIVERY_VALUE_LENGTH, "택배사");
        this.replacementTrackingNumber =
                requireText(trackingNumber, MAX_DELIVERY_VALUE_LENGTH, "운송장 번호");
        if (adminNote != null && !adminNote.isBlank()) {
            this.adminNote = optionalText(adminNote, MAX_ADMIN_NOTE_LENGTH, "관리자 메모");
        }
        this.completedByAdminId = requireAdminId(adminId);
        this.completedAt = completedAt;
        this.status = OrderClaimStatus.COMPLETED;
    }

    private void requireRequested() {
        if (status != OrderClaimStatus.REQUESTED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "접수 상태의 클레임만 처리할 수 있습니다.");
        }
    }

    private static String requireText(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, fieldName + "은 필수입니다.");
        }
        return validatedText(value, maxLength, fieldName);
    }

    private static String optionalText(String value, int maxLength, String fieldName) {
        return value != null && !value.isBlank() ? validatedText(value, maxLength, fieldName) : null;
    }

    private static String validatedText(String value, int maxLength, String fieldName) {
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, fieldName + "은 " + maxLength + "자 이하여야 합니다.");
        }
        return trimmed;
    }

    private static Long requireAdminId(Long adminId) {
        if (adminId == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "처리 관리자 정보가 필요합니다.");
        }
        return adminId;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public OrderClaimType getType() { return type; }
    public OrderClaimResolution getRequestedResolution() { return requestedResolution; }
    public OrderClaimStatus getStatus() { return status; }
    public String getCustomerReason() { return customerReason; }
    public String getAdminNote() { return adminNote; }
    public Long getResolvedByAdminId() { return resolvedByAdminId; }
    public Long getCompletedByAdminId() { return completedByAdminId; }
    public String getReplacementCarrier() { return replacementCarrier; }
    public String getReplacementTrackingNumber() { return replacementTrackingNumber; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

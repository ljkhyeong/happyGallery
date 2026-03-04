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

@Entity
@Table(name = "order_approvals")
public class OrderApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "decided_by_admin_id")
    private Long decidedByAdminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderApprovalDecision decision;

    @Column(length = 500)
    private String reason;

    @Column(name = "decided_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime decidedAt;

    protected OrderApprovalHistory() {}

    public OrderApprovalHistory(Long orderId, OrderApprovalDecision decision) {
        this(orderId, decision, null, null);
    }

    public OrderApprovalHistory(Long orderId, OrderApprovalDecision decision, Long decidedByAdminId, String reason) {
        this.orderId = orderId;
        this.decision = decision;
        this.decidedByAdminId = decidedByAdminId;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getDecidedByAdminId() { return decidedByAdminId; }
    public OrderApprovalDecision getDecision() { return decision; }
    public String getReason() { return reason; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
}

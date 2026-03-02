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

/**
 * 상품 주문 — orders 테이블.
 *
 * <p>결제 완료 시 {@link OrderStatus#PAID_APPROVAL_PENDING}으로 생성되며,
 * 관리자 승인({@link #approve()}) 또는 거절({@link #reject()}),
 * 혹은 24시간 초과 자동환불({@link #markAutoRefunded()})로 전이된다.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_id")
    private Long guestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "approval_deadline_at")
    private LocalDateTime approvalDeadlineAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Order() {}

    /**
     * 결제 완료 주문 생성. 초기 상태는 {@link OrderStatus#PAID_APPROVAL_PENDING}.
     *
     * @param guestId            비회원 ID (회원이면 null)
     * @param totalAmount        결제 금액 (원)
     * @param paidAt             결제 완료 시각
     * @param approvalDeadlineAt 승인 마감 시각 (= paidAt + 24h)
     */
    public Order(Long guestId, long totalAmount, LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        this.guestId = guestId;
        this.totalAmount = totalAmount;
        this.paidAt = paidAt;
        this.approvalDeadlineAt = approvalDeadlineAt;
        this.status = OrderStatus.PAID_APPROVAL_PENDING;
    }

    /**
     * 관리자 승인 처리.
     * 이미 환불된 주문({@link OrderStatus#REJECTED_REFUNDED}, {@link OrderStatus#AUTO_REFUNDED_TIMEOUT})에
     * 대한 호출은 {@link com.personal.happygallery.common.error.AlreadyRefundedException}을 던진다.
     */
    public void approve() {
        this.status.requireApprovable();
        this.status = OrderStatus.APPROVED_FULFILLMENT_PENDING;
    }

    /**
     * 관리자 거절 처리. 환불·재고 복구는 서비스 레이어에서 선행한다.
     * 이미 환불된 주문에 대한 호출은 {@link com.personal.happygallery.common.error.AlreadyRefundedException}을 던진다.
     */
    public void reject() {
        this.status.requireApprovable();
        this.status = OrderStatus.REJECTED_REFUNDED;
    }

    /**
     * 24시간 초과 자동환불 처리.
     * 이미 환불된 주문에 대한 호출은 {@link com.personal.happygallery.common.error.AlreadyRefundedException}을 던진다.
     */
    public void markAutoRefunded() {
        this.status.requireApprovable();
        this.status = OrderStatus.AUTO_REFUNDED_TIMEOUT;
    }

    public Long getId() { return id; }
    public Long getGuestId() { return guestId; }
    public OrderStatus getStatus() { return status; }
    public long getTotalAmount() { return totalAmount; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getApprovalDeadlineAt() { return approvalDeadlineAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

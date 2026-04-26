package com.personal.happygallery.domain.order;

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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "guest_id")
    private Long guestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "access_token", length = 64)
    private String accessToken;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "approval_deadline_at")
    private LocalDateTime approvalDeadlineAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Order() {}

    private Order(Long userId, Long guestId, String accessToken, long totalAmount,
                  LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        this.userId = userId;
        this.guestId = guestId;
        this.accessToken = accessToken;
        this.totalAmount = totalAmount;
        this.paidAt = paidAt;
        this.approvalDeadlineAt = approvalDeadlineAt;
        this.status = OrderStatus.PAID_APPROVAL_PENDING;
    }

    /** 비회원 주문 생성. 초기 상태는 {@link OrderStatus#PAID_APPROVAL_PENDING}. */
    public static Order forGuest(Long guestId, String accessToken, long totalAmount,
                                 LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        return new Order(null, guestId, accessToken, totalAmount, paidAt, approvalDeadlineAt);
    }

    /** 회원 주문 생성. guest 대신 user_id를 설정한다. */
    public static Order forMember(Long userId, long totalAmount,
                                  LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        return new Order(userId, null, null, totalAmount, paidAt, approvalDeadlineAt);
    }

    /**
     * 관리자 승인 처리.
     * 이미 환불된 주문({@link OrderStatus#REJECTED}, {@link OrderStatus#AUTO_REFUND_TIMEOUT})에
     * 대한 호출은 {@link com.personal.happygallery.domain.error.AlreadyRefundedException}을 던진다.
     * 승인 대기 상태({@link OrderStatus#PAID_APPROVAL_PENDING})가 아니면 400을 던진다.
     */
    public void approve() {
        this.status.requireApprovalPending();
        this.status = OrderStatus.APPROVED_FULFILLMENT_PENDING;
    }

    /**
     * 관리자 거절 처리. 환불·재고 복구는 서비스 레이어에서 선행한다.
     * 이미 환불된 주문에 대한 호출은 {@link com.personal.happygallery.domain.error.AlreadyRefundedException}을 던진다.
     * 승인 대기 상태({@link OrderStatus#PAID_APPROVAL_PENDING})가 아니면 400을 던진다.
     * 제작 중인 주문({@link OrderStatus#IN_PRODUCTION}, {@link OrderStatus#DELAY_REQUESTED})은
     * {@link com.personal.happygallery.domain.error.ProductionRefundNotAllowedException}을 던진다.
     */
    public void reject() {
        this.status.requireCancellable();
        this.status.requireApprovalPending();
        this.status = OrderStatus.REJECTED;
    }

    /**
     * 예약 제작 승인. MADE_TO_ORDER 상품 주문에서 호출한다.
     * 이미 환불된 주문에 대한 호출은 {@link com.personal.happygallery.domain.error.AlreadyRefundedException}을 던진다.
     */
    public void approveAsProduction() {
        this.status.requireApprovalPending();
        this.status = OrderStatus.IN_PRODUCTION;
    }

    /**
     * 고객 동의 하에 배송 지연 상태로 전환한다.
     * {@link OrderStatus#IN_PRODUCTION} 상태가 아니면 {@code 400 INVALID_INPUT}을 던진다.
     */
    public void requestDelay() {
        this.status.requireInProduction();
        this.status = OrderStatus.DELAY_REQUESTED;
    }

    /**
     * 지연 요청 상태에서 제작을 재개한다.
     * {@link OrderStatus#DELAY_REQUESTED} 상태가 아니면 400을 던진다.
     */
    public void resumeProduction() {
        this.status.requireDelayRequested();
        this.status = OrderStatus.IN_PRODUCTION;
    }

    /**
     * 제작 완료 처리. {@link OrderStatus#IN_PRODUCTION} 또는 {@link OrderStatus#DELAY_REQUESTED}
     * 상태에서만 호출 가능하며, {@link OrderStatus#APPROVED_FULFILLMENT_PENDING}으로 전이한다.
     */
    public void completeProduction() {
        this.status.requireProductionCompletable();
        this.status = OrderStatus.APPROVED_FULFILLMENT_PENDING;
    }

    /**
     * 24시간 초과 자동환불 처리.
     * 승인 대기 상태({@link OrderStatus#PAID_APPROVAL_PENDING})가 아니면 예외를 던진다.
     * 이미 환불된 주문은 {@link com.personal.happygallery.domain.error.AlreadyRefundedException}(409).
     */
    public void markAutoRefunded() {
        this.status.requireApprovalPending();
        this.status = OrderStatus.AUTO_REFUND_TIMEOUT;
    }

    /**
     * 배치에서 자동환불 대상인지 판단한다.
     * 승인 대기 상태이고, 승인 마감 시각이 {@code now} 이전이면 {@code true}.
     */
    public boolean canAutoRefund(LocalDateTime now) {
        return this.status == OrderStatus.PAID_APPROVAL_PENDING
                && this.approvalDeadlineAt != null
                && this.approvalDeadlineAt.isBefore(now);
    }

    /**
     * 배송 준비 시작. {@link OrderStatus#APPROVED_FULFILLMENT_PENDING} → {@link OrderStatus#SHIPPING_PREPARING}.
     */
    public void markShippingPreparing() {
        this.status.requireShippingPreparable();
        this.status = OrderStatus.SHIPPING_PREPARING;
    }

    /**
     * 배송 출발. {@link OrderStatus#SHIPPING_PREPARING} → {@link OrderStatus#SHIPPED}.
     */
    public void markShipped() {
        this.status.requireShippingPreparing();
        this.status = OrderStatus.SHIPPED;
    }

    /**
     * 배송 완료. {@link OrderStatus#SHIPPED} → {@link OrderStatus#DELIVERED}.
     */
    public void markDelivered() {
        this.status.requireShipped();
        this.status = OrderStatus.DELIVERED;
    }

    /**
     * 픽업 준비 완료. {@link OrderStatus#APPROVED_FULFILLMENT_PENDING} 상태가 아니면 400을 던진다.
     */
    public void markPickupReady() {
        this.status.requireFulfillmentPending();
        this.status = OrderStatus.PICKUP_READY;
    }

    /**
     * 픽업 완료. {@link OrderStatus#PICKUP_READY} 상태가 아니면 400을 던진다.
     */
    public void confirmPickup() {
        this.status.requirePickupReady();
        this.status = OrderStatus.PICKED_UP;
    }

    /**
     * 픽업 마감 초과 자동환불. {@link OrderStatus#PICKUP_READY} 상태가 아니면 400을 던진다.
     */
    public void markPickupExpired() {
        this.status.requirePickupReady();
        this.status = OrderStatus.PICKUP_EXPIRED;
    }

    public void claimToUser(Long userId) {
        this.userId = userId;
        this.guestId = null;
    }

    /** 결제 confirm 성공 후 PG 원결제 참조값을 저장한다. */
    public void recordPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getGuestId() { return guestId; }
    public String getAccessToken() { return accessToken; }
    public String getPaymentKey() { return paymentKey; }
    public OrderStatus getStatus() { return status; }
    public long getTotalAmount() { return totalAmount; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getApprovalDeadlineAt() { return approvalDeadlineAt; }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

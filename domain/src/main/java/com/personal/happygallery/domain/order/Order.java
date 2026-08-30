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

    @Column(name = "product_amount", nullable = false)
    private long productAmount;

    @Column(name = "shipping_fee", nullable = false)
    private long shippingFee;

    @Column(name = "coupon_discount_amount", nullable = false)
    private long couponDiscountAmount;

    @Column(name = "reward_used_amount", nullable = false)
    private long rewardUsedAmount;

    @Column(name = "pg_paid_amount", nullable = false)
    private long pgPaidAmount;

    @Column(name = "reward_earn_base", nullable = false)
    private long rewardEarnBase;

    @Column(name = "issued_coupon_id")
    private Long issuedCouponId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "approval_deadline_at")
    private LocalDateTime approvalDeadlineAt;

    @Column(name = "made_to_order_consent_version", length = 30)
    private String madeToOrderConsentVersion;

    @Column(name = "made_to_order_consent_disclosure", length = 1000)
    private String madeToOrderConsentDisclosure;

    @Column(name = "made_to_order_consent_at")
    private LocalDateTime madeToOrderConsentAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Order() {}

    private Order(Long userId, Long guestId, String accessToken, OrderPricingSnapshot pricing,
                  LocalDateTime paidAt, LocalDateTime approvalDeadlineAt,
                  MadeToOrderConsent madeToOrderConsent) {
        requireExactlyOneOwner(userId, guestId);
        this.userId = userId;
        this.guestId = guestId;
        this.accessToken = accessToken;
        this.totalAmount = pricing.totalAmount();
        this.productAmount = pricing.productAmount();
        this.shippingFee = pricing.shippingFee();
        this.couponDiscountAmount = pricing.couponDiscountAmount();
        this.rewardUsedAmount = pricing.rewardUsedAmount();
        this.pgPaidAmount = pricing.pgPaidAmount();
        this.rewardEarnBase = pricing.rewardEarnBase();
        this.issuedCouponId = pricing.issuedCouponId();
        this.paidAt = paidAt;
        this.approvalDeadlineAt = approvalDeadlineAt;
        this.status = OrderStatus.PAID_APPROVAL_PENDING;
        if (madeToOrderConsent != null) {
            this.madeToOrderConsentVersion = madeToOrderConsent.version();
            this.madeToOrderConsentDisclosure = madeToOrderConsent.disclosure();
            this.madeToOrderConsentAt = madeToOrderConsent.agreedAt();
        }
    }

    private static void requireExactlyOneOwner(Long userId, Long guestId) {
        if ((userId == null) == (guestId == null)) {
            throw new IllegalArgumentException("주문은 회원 또는 비회원 소유자 중 하나만 가져야 합니다.");
        }
    }

    /** 비회원 주문 생성. 초기 상태는 {@link OrderStatus#PAID_APPROVAL_PENDING}. */
    public static Order forGuest(Long guestId, String accessToken, long totalAmount,
                                 LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        return forGuest(guestId, accessToken, totalAmount, 0L, paidAt, approvalDeadlineAt);
    }

    public static Order forGuest(Long guestId, String accessToken, long totalAmount, long shippingFee,
                                 LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        return forGuest(
                guestId, accessToken, totalAmount, shippingFee, paidAt, approvalDeadlineAt, null);
    }

    public static Order forGuest(Long guestId, String accessToken, long totalAmount, long shippingFee,
                                 LocalDateTime paidAt, LocalDateTime approvalDeadlineAt,
                                 MadeToOrderConsent madeToOrderConsent) {
        return new Order(
                null, guestId, accessToken, legacyPricing(totalAmount, shippingFee),
                paidAt, approvalDeadlineAt, madeToOrderConsent);
    }

    /** 회원 주문 생성. guest 대신 user_id를 설정한다. */
    public static Order forMember(Long userId, long totalAmount,
                                  LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        return forMember(userId, totalAmount, 0L, paidAt, approvalDeadlineAt);
    }

    public static Order forMember(Long userId, long totalAmount, long shippingFee,
                                  LocalDateTime paidAt, LocalDateTime approvalDeadlineAt) {
        return forMember(userId, totalAmount, shippingFee, paidAt, approvalDeadlineAt, null);
    }

    public static Order forMember(Long userId, long totalAmount, long shippingFee,
                                  LocalDateTime paidAt, LocalDateTime approvalDeadlineAt,
                                  MadeToOrderConsent madeToOrderConsent) {
        return new Order(
                userId, null, null, legacyPricing(totalAmount, shippingFee),
                paidAt, approvalDeadlineAt, madeToOrderConsent);
    }

    public static Order forMember(Long userId,
                                  OrderPricingSnapshot pricing,
                                  LocalDateTime paidAt,
                                  LocalDateTime approvalDeadlineAt,
                                  MadeToOrderConsent madeToOrderConsent) {
        return new Order(
                userId, null, null, pricing,
                paidAt, approvalDeadlineAt, madeToOrderConsent);
    }

    private static OrderPricingSnapshot legacyPricing(long totalAmount, long shippingFee) {
        if (shippingFee < 0L || shippingFee > totalAmount) {
            throw new IllegalArgumentException("배송비는 0원 이상이며 총 결제 금액을 넘을 수 없습니다.");
        }
        return OrderPricingSnapshot.fullPrice(totalAmount - shippingFee, shippingFee);
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
     * 제작 중인 주문({@link OrderStatus#IN_PRODUCTION}, {@link OrderStatus#DELAY_ACCEPTED})은
     * {@link com.personal.happygallery.domain.error.ProductionRefundNotAllowedException}을 던진다.
     */
    public void reject() {
        this.status.requireCancellable();
        this.status.requireApprovalPending();
        this.status = OrderStatus.REJECTED;
    }

    /** 고객이 관리자 승인 전에 주문을 직접 취소한다. */
    public void cancelByCustomer() {
        this.status.requireCustomerCancellationAllowed();
        this.status = OrderStatus.CUSTOMER_CANCELED;
    }

    /**
     * 예약 제작 승인. MADE_TO_ORDER 상품 주문에서 호출한다.
     * 이미 환불된 주문에 대한 호출은 {@link com.personal.happygallery.domain.error.AlreadyRefundedException}을 던진다.
     */
    public void approveAsProduction() {
        this.status.requireApprovalPending();
        if (madeToOrderConsentAt == null) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문제작 동의 기록이 없어 제작을 시작할 수 없습니다.");
        }
        this.status = OrderStatus.IN_PRODUCTION;
    }

    /** 승인 전 기성품의 재고 부족으로 이행 지연을 제안하고 고객 응답을 기다린다. */
    public void proposeReadyStockDelay() {
        this.status.requireReadyStockDelayProposable();
        this.status = OrderStatus.DELAY_CONSENT_PENDING;
    }

    /** 제작 중인 주문제작 또는 혼합 주문의 일정 지연을 제안하고 고객 응답을 기다린다. */
    public void proposeProductionDelay() {
        this.status.requireProductionDelayProposable();
        this.status = OrderStatus.DELAY_CONSENT_PENDING;
    }

    /** 고객이 주문 이행 지연 제안을 수락하거나 거절한다. */
    public void respondToDelay(OrderDelayDecision decision) {
        this.status.requireDelayConsentPending();
        if (decision == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지연 제안 응답은 필수입니다.");
        }
        this.status = switch (decision) {
            case ACCEPT -> OrderStatus.DELAY_ACCEPTED;
            case REJECT -> OrderStatus.DELAY_REJECTED_CANCELED;
        };
    }

    /**
     * 고객이 주문 이행 지연을 거절해 주문을 취소한다.
     * {@link OrderStatus#DELAY_CONSENT_PENDING} 상태에서만 허용한다.
     */
    public void cancelForDelayRejection() {
        this.status.requireDelayRejectionCancelable();
        this.status = OrderStatus.DELAY_REJECTED_CANCELED;
    }

    /**
     * 지연 수락 상태에서 주문 이행을 재개한다.
     * 주문제작 상품은 제작 중으로, 기성품은 배송·픽업 이행 대기로 돌아간다.
     * {@link OrderStatus#DELAY_ACCEPTED} 상태가 아니면 400을 던진다.
     */
    public void resumeAfterDelay() {
        this.status.requireDelayAccepted();
        this.status = madeToOrderConsentAt == null
                ? OrderStatus.APPROVED_FULFILLMENT_PENDING
                : OrderStatus.IN_PRODUCTION;
    }

    /**
     * 제작 완료 처리. {@link OrderStatus#IN_PRODUCTION} 또는 {@link OrderStatus#DELAY_ACCEPTED}
     * 상태에서만 호출 가능하며, {@link OrderStatus#APPROVED_FULFILLMENT_PENDING}으로 전이한다.
     */
    public void completeProduction() {
        this.status.requireProductionCompletable();
        if (madeToOrderConsentAt == null) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "주문제작 상품만 제작 완료 처리할 수 있습니다.");
        }
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

    /** 픽업 마감 초과를 환불 없이 미수령 종료 상태로 전이한다. */
    public void markPickupForfeited() {
        this.status.requirePickupReady();
        this.status = OrderStatus.PICKUP_FORFEITED;
    }

    /** 관리자가 미수령 종료 주문을 예외 환불 상태로 전이한다. */
    public void markMissedPickupRefunded() {
        this.status.requireMissedPickupRefundable();
        this.status = OrderStatus.PICKUP_EXPIRED;
    }

    public void claimToUser(Long userId) {
        requireExactlyOneOwner(userId, null);
        this.userId = userId;
        this.guestId = null;
        this.accessToken = null;
    }

    /** 휴대폰 소유 확인 후 비회원 주문의 관리 토큰을 교체한다. */
    public void replaceGuestAccessToken(String accessToken) {
        if (guestId == null) {
            throw new IllegalStateException("회원 주문에는 비회원 접근 토큰을 발급할 수 없습니다.");
        }
        this.accessToken = accessToken;
    }

    /** 결제 confirm 성공 후 원결제 paymentKey를 저장한다. */
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
    public long getProductAmount() { return productAmount; }
    public long getShippingFee() { return shippingFee; }
    public long getCouponDiscountAmount() { return couponDiscountAmount; }
    public long getRewardUsedAmount() { return rewardUsedAmount; }
    public long getPgPaidAmount() { return pgPaidAmount; }
    public long getRewardEarnBase() { return rewardEarnBase; }
    public Long getIssuedCouponId() { return issuedCouponId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getApprovalDeadlineAt() { return approvalDeadlineAt; }
    public String getMadeToOrderConsentVersion() { return madeToOrderConsentVersion; }
    public String getMadeToOrderConsentDisclosure() { return madeToOrderConsentDisclosure; }
    public LocalDateTime getMadeToOrderConsentAt() { return madeToOrderConsentAt; }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

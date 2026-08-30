package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.RefundStatus;
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
import java.util.Objects;
import java.util.UUID;

/** 환불 요청 — 원결제 paymentKey와 환불 거래 transactionKey를 분리해 재시도 가능성을 보존한다. */
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_claim_id")
    private Long orderClaimId;

    @Column(name = "pass_purchase_id")
    private Long passPurchaseId;

    @Column(name = "payment_attempt_id")
    private Long paymentAttemptId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "customer_refund_amount", nullable = false)
    private long customerRefundAmount;

    @Column(name = "reward_restore_amount", nullable = false)
    private long rewardRestoreAmount;

    @Column(name = "reward_revoke_amount", nullable = false)
    private long rewardRevokeAmount;

    @Column(name = "restore_coupon", nullable = false)
    private boolean restoreCoupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "processing_token", length = 64)
    private String processingToken;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_recovery_at")
    private LocalDateTime lastRecoveryAt;

    @Column(name = "payment_key", length = 255)
    private String paymentKey;

    @Column(name = "refund_transaction_key", unique = true, length = 255)
    private String refundTransactionKey;

    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Refund() {}

    private Refund(Long bookingId, Long orderId, Long orderClaimId,
                   Long passPurchaseId, Long paymentAttemptId,
                   long amount, long customerRefundAmount,
                   long rewardRestoreAmount, long rewardRevokeAmount,
                   boolean restoreCoupon, String paymentKey) {
        validateAmounts(
                orderId, orderClaimId, amount, customerRefundAmount,
                rewardRestoreAmount, rewardRevokeAmount, restoreCoupon);
        this.bookingId = bookingId;
        this.orderId = orderId;
        this.orderClaimId = orderClaimId;
        this.passPurchaseId = passPurchaseId;
        this.paymentAttemptId = paymentAttemptId;
        this.amount = amount;
        this.customerRefundAmount = customerRefundAmount;
        this.rewardRestoreAmount = rewardRestoreAmount;
        this.rewardRevokeAmount = rewardRevokeAmount;
        this.restoreCoupon = restoreCoupon;
        this.paymentKey = paymentKey;
        this.idempotencyKey = UUID.randomUUID().toString();
        this.status = RefundStatus.REQUESTED;
    }

    private static void validateAmounts(
            Long orderId, Long orderClaimId,
            long amount, long customerRefundAmount,
            long rewardRestoreAmount, long rewardRevokeAmount,
            boolean restoreCoupon) {
        long composedCustomerRefundAmount;
        try {
            composedCustomerRefundAmount = Math.addExact(amount, rewardRestoreAmount);
        } catch (ArithmeticException exception) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "환불 금액과 혜택 복원 정보가 올바르지 않습니다.");
        }
        boolean invalidAmount = amount < 0L
                || customerRefundAmount < 0L
                || rewardRestoreAmount < 0L
                || rewardRevokeAmount < 0L
                || customerRefundAmount != composedCustomerRefundAmount;
        boolean noRefundEffect = customerRefundAmount == 0L
                && rewardRevokeAmount == 0L
                && !restoreCoupon;
        boolean invalidOrderBenefit = orderId == null
                && (rewardRestoreAmount > 0L || rewardRevokeAmount > 0L || restoreCoupon);
        boolean invalidClaimCoupon = orderClaimId != null && restoreCoupon;
        if (invalidAmount || noRefundEffect || invalidOrderBenefit || invalidClaimCoupon) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "환불 금액과 혜택 복원 정보가 올바르지 않습니다.");
        }
    }

    /** 예약금 환불 요청 생성 (booking 취소 시). */
    public static Refund forBooking(Booking booking, long amount) {
        Objects.requireNonNull(booking, "booking must not be null");
        Long bookingId = Objects.requireNonNull(booking.getId(), "bookingId must not be null");
        return monetaryRefund(
                bookingId, null, null, null, null, amount, booking.getPaymentKey());
    }

    /** 주문 환불 요청 생성 (주문 거절/자동환불 시). bookingId는 null. */
    public static Refund forOrder(Long orderId, long amount, String paymentKey) {
        return forOrder(orderId, amount, amount, 0L, 0L, false, paymentKey);
    }

    /** 주문의 PG 취소액과 고객 반환 혜택을 환불 요청 시점 값으로 고정한다. */
    public static Refund forOrder(
            Long orderId,
            long pgRefundAmount,
            long customerRefundAmount,
            long rewardRestoreAmount,
            long rewardRevokeAmount,
            boolean restoreCoupon,
            String paymentKey) {
        return new Refund(
                null, Objects.requireNonNull(orderId, "orderId must not be null"),
                null, null, null,
                pgRefundAmount, customerRefundAmount,
                rewardRestoreAmount, rewardRevokeAmount, restoreCoupon, paymentKey);
    }

    /** 배송·픽업 완료 후 주문 클레임 환불 요청 생성. */
    public static Refund forOrderClaim(Long orderId, Long orderClaimId, long amount, String paymentKey) {
        return forOrderClaim(
                orderId, orderClaimId, amount, amount, 0L, 0L, paymentKey);
    }

    /** 클레임의 PG 취소액과 적립금 복원·회수액을 승인 시점 값으로 고정한다. */
    public static Refund forOrderClaim(
            Long orderId,
            Long orderClaimId,
            long pgRefundAmount,
            long customerRefundAmount,
            long rewardRestoreAmount,
            long rewardRevokeAmount,
            String paymentKey) {
        return new Refund(null,
                Objects.requireNonNull(orderId, "orderId must not be null"),
                Objects.requireNonNull(orderClaimId, "orderClaimId must not be null"),
                null, null,
                pgRefundAmount, customerRefundAmount,
                rewardRestoreAmount, rewardRevokeAmount, false, paymentKey);
    }

    /** 8회권 환불 요청 생성. bookingId/orderId는 null. */
    public static Refund forPass(Long passPurchaseId, long amount, String paymentKey) {
        return monetaryRefund(null, null, null,
                Objects.requireNonNull(passPurchaseId, "passPurchaseId must not be null"),
                null, amount, paymentKey);
    }

    /** PG 승인 후 도메인 생성 실패를 보상하는 환불 요청. */
    public static Refund forPaymentAttempt(Long paymentAttemptId, long amount, String paymentKey) {
        return monetaryRefund(null, null, null, null,
                Objects.requireNonNull(paymentAttemptId, "paymentAttemptId must not be null"),
                amount, paymentKey);
    }

    private static Refund monetaryRefund(
            Long bookingId, Long orderId, Long orderClaimId,
            Long passPurchaseId, Long paymentAttemptId,
            long amount, String paymentKey) {
        if (amount <= 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "환불 금액은 1원 이상이어야 합니다.");
        }
        return new Refund(
                bookingId, orderId, orderClaimId, passPurchaseId, paymentAttemptId,
                amount, amount, 0L, 0L, false, paymentKey);
    }

    /** 실행 가능한 환불을 선점한다. 선점할 수 없으면 null을 반환한다. */
    public String startProcessing(LocalDateTime now, LocalDateTime staleBefore) {
        if (!isClaimable(now, staleBefore)) {
            return null;
        }
        this.status = RefundStatus.PROCESSING;
        this.processingAt = now;
        this.processingToken = UUID.randomUUID().toString();
        this.attemptCount++;
        this.nextAttemptAt = null;
        return this.processingToken;
    }

    /** 자동 복구 후보가 반복해서 선두를 독점하지 않도록 마지막 복구 선점 시각을 기록한다. */
    public void recordRecoveryAttempt(LocalDateTime now) {
        this.lastRecoveryAt = now;
    }

    private boolean isClaimable(LocalDateTime now, LocalDateTime staleBefore) {
        if (status == RefundStatus.PROCESSING) {
            return processingAt != null && processingAt.isBefore(staleBefore);
        }
        if (status != RefundStatus.REQUESTED
                && status != RefundStatus.RETRYABLE
                && status != RefundStatus.RECONCILIATION_REQUIRED) {
            return false;
        }
        return nextAttemptAt == null || !nextAttemptAt.isAfter(now);
    }

    /** PG 환불 성공 처리. 현재 선점 토큰과 일치하지 않는 오래된 결과는 무시한다. */
    public boolean markSucceeded(String token, String refundTransactionKey, LocalDateTime succeededAt) {
        if (!ownsProcessing(token)) {
            return false;
        }
        if (refundTransactionKey == null || refundTransactionKey.isBlank()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "PG 환불 결과의 transactionKey가 누락되었습니다.");
        }
        Objects.requireNonNull(succeededAt, "succeededAt must not be null");
        this.status = RefundStatus.SUCCEEDED;
        this.refundTransactionKey = refundTransactionKey;
        this.succeededAt = succeededAt;
        this.failReason = null;
        clearProcessing();
        return true;
    }

    /** PG 취소액이 없는 주문 환불을 외부 호출 없이 성공 처리한다. */
    public boolean markLocallySucceeded(String token, LocalDateTime succeededAt) {
        if (!ownsProcessing(token)) {
            return false;
        }
        if (amount != 0L) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "PG 취소액이 있는 환불은 로컬 성공 처리할 수 없습니다.");
        }
        this.status = RefundStatus.SUCCEEDED;
        this.refundTransactionKey = null;
        this.succeededAt = Objects.requireNonNull(succeededAt, "succeededAt must not be null");
        this.failReason = null;
        clearProcessing();
        return true;
    }

    /** PG가 명시적으로 거절한 환불을 최종 실패로 처리한다. */
    public boolean markFailed(String token, String reason) {
        if (!ownsProcessing(token)) {
            return false;
        }
        this.status = RefundStatus.FAILED;
        this.failReason = reason;
        clearProcessing();
        return true;
    }

    /** PG 호출을 실행하지 못했거나 명시적인 일시 실패가 발생한 경우 재시도를 예약한다. */
    public boolean markRetryable(String token, String reason, LocalDateTime nextAttemptAt) {
        if (!ownsProcessing(token)) {
            return false;
        }
        this.status = RefundStatus.RETRYABLE;
        this.failReason = reason;
        this.nextAttemptAt = nextAttemptAt;
        clearProcessingToken();
        return true;
    }

    /** 요청 처리 여부를 알 수 없는 경우 같은 멱등키로 결과 확인을 예약한다. */
    public boolean markReconciliationRequired(String token, String reason, LocalDateTime nextAttemptAt) {
        if (!ownsProcessing(token)) {
            return false;
        }
        this.status = RefundStatus.RECONCILIATION_REQUIRED;
        this.failReason = reason;
        this.nextAttemptAt = nextAttemptAt;
        clearProcessingToken();
        return true;
    }

    /** 운영자가 조치 필요 환불을 즉시 처리하도록 예약한다. 결과 불명 상태는 조회 단계를 유지한다. */
    public void requestRetry(LocalDateTime now) {
        if (status != RefundStatus.FAILED
                && status != RefundStatus.RETRYABLE
                && status != RefundStatus.RECONCILIATION_REQUIRED) {
            throw new IllegalStateException("재시도할 수 없는 환불 상태입니다. (현재: " + status + ")");
        }
        if (status != RefundStatus.RECONCILIATION_REQUIRED) {
            this.status = RefundStatus.RETRYABLE;
        }
        this.nextAttemptAt = now;
        clearProcessingToken();
    }

    private boolean ownsProcessing(String token) {
        return status == RefundStatus.PROCESSING
                && token != null
                && token.equals(processingToken);
    }

    private void clearProcessing() {
        this.nextAttemptAt = null;
        clearProcessingToken();
    }

    private void clearProcessingToken() {
        this.processingAt = null;
        this.processingToken = null;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public Long getOrderId() { return orderId; }
    public Long getOrderClaimId() { return orderClaimId; }
    public Long getPassPurchaseId() { return passPurchaseId; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public long getAmount() { return amount; }
    public long getCustomerRefundAmount() { return customerRefundAmount; }
    public long getRewardRestoreAmount() { return rewardRestoreAmount; }
    public long getRewardRevokeAmount() { return rewardRevokeAmount; }
    public boolean isRestoreCoupon() { return restoreCoupon; }
    public boolean requiresPgCancellation() { return amount > 0L; }
    public RefundStatus getStatus() { return status; }
    public LocalDateTime getProcessingAt() { return processingAt; }
    public String getProcessingToken() { return processingToken; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public LocalDateTime getLastRecoveryAt() { return lastRecoveryAt; }
    public String getPaymentKey() { return paymentKey; }
    public String getRefundTransactionKey() { return refundTransactionKey; }
    public LocalDateTime getSucceededAt() { return succeededAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

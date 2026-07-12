package com.personal.happygallery.domain.booking;

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

    @Column(name = "pass_purchase_id")
    private Long passPurchaseId;

    @Column(name = "payment_attempt_id")
    private Long paymentAttemptId;

    @Column(nullable = false)
    private long amount;

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

    @Column(name = "payment_key", length = 255)
    private String paymentKey;

    @Column(name = "refund_transaction_key", length = 255)
    private String refundTransactionKey;

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

    private Refund(Long bookingId, Long orderId, Long passPurchaseId, Long paymentAttemptId,
                   long amount, String paymentKey) {
        this.bookingId = bookingId;
        this.orderId = orderId;
        this.passPurchaseId = passPurchaseId;
        this.paymentAttemptId = paymentAttemptId;
        this.amount = amount;
        this.paymentKey = paymentKey;
        this.idempotencyKey = UUID.randomUUID().toString();
        this.status = RefundStatus.REQUESTED;
    }

    /** 예약금 환불 요청 생성 (booking 취소 시). */
    public static Refund forBooking(Booking booking, long amount) {
        Objects.requireNonNull(booking, "booking must not be null");
        Long bookingId = Objects.requireNonNull(booking.getId(), "bookingId must not be null");
        return new Refund(bookingId, null, null, null, amount, booking.getPaymentKey());
    }

    /** 주문 환불 요청 생성 (주문 거절/자동환불 시). bookingId는 null. */
    public static Refund forOrder(Long orderId, long amount, String paymentKey) {
        return new Refund(
                null,
                Objects.requireNonNull(orderId, "orderId must not be null"),
                null,
                null,
                amount,
                paymentKey);
    }

    /** 8회권 환불 요청 생성. bookingId/orderId는 null. */
    public static Refund forPass(Long passPurchaseId, long amount, String paymentKey) {
        return new Refund(null, null, Objects.requireNonNull(passPurchaseId, "passPurchaseId must not be null"), null,
                amount, paymentKey);
    }

    /** PG 승인 후 도메인 생성 실패를 보상하는 환불 요청. */
    public static Refund forPaymentAttempt(Long paymentAttemptId, long amount, String paymentKey) {
        return new Refund(null, null, null,
                Objects.requireNonNull(paymentAttemptId, "paymentAttemptId must not be null"),
                amount, paymentKey);
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
    public boolean markSucceeded(String token, String refundTransactionKey) {
        if (!ownsProcessing(token)) {
            return false;
        }
        this.status = RefundStatus.SUCCEEDED;
        this.refundTransactionKey = refundTransactionKey;
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

    /** 운영자가 조치 필요 환불을 즉시 재시도할 수 있도록 예약한다. */
    public void requestRetry(LocalDateTime now) {
        if (status != RefundStatus.FAILED
                && status != RefundStatus.RETRYABLE
                && status != RefundStatus.RECONCILIATION_REQUIRED) {
            throw new IllegalStateException("재시도할 수 없는 환불 상태입니다. (현재: " + status + ")");
        }
        this.status = RefundStatus.RETRYABLE;
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
    public Long getPassPurchaseId() { return passPurchaseId; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public long getAmount() { return amount; }
    public RefundStatus getStatus() { return status; }
    public LocalDateTime getProcessingAt() { return processingAt; }
    public String getProcessingToken() { return processingToken; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public String getPaymentKey() { return paymentKey; }
    public String getRefundTransactionKey() { return refundTransactionKey; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

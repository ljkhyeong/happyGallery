package com.personal.happygallery.domain.payment;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * PG 결제 확정 시도 — payment_attempt 테이블.
 *
 * <p>서버가 prepare 단계에서 orderIdExternal(UUID)과 amount를 확정해
 * {@link PaymentAttemptStatus#PENDING}으로 생성한다. confirm 단계에서는 짧은 트랜잭션으로
 * PROCESSING을 선점하고, 트랜잭션 밖 PG 승인 후 APPROVED를 저장한 다음 도메인 생성과
 * CONFIRMED 전이를 한 트랜잭션으로 완료한다.
 *
 * <p>서버가 orderId와 amount를 둘 다 쥐는 것이 핵심이다. 클라이언트가 금액을 속여도
 * prepare 시점의 amount와 confirm 시점에 들어온 amount가 다르면 {@link #requireConfirmable(long)}에서
 * {@code 400 INVALID_INPUT}을 던진다.
 */
@Entity
@Table(name = "payment_attempt")
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id_external", nullable = false, unique = true, length = 64)
    private String orderIdExternal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentContext context;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentAttemptStatus status;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "pg_ref", length = 200)
    private String pgRef;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Lob
    @Column(name = "payload_enc", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String payloadEnc;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PaymentAttempt() {}

    private PaymentAttempt(String orderIdExternal, PaymentContext context, long amount, String payloadEnc) {
        this.orderIdExternal = orderIdExternal;
        this.context = context;
        this.amount = amount;
        this.status = PaymentAttemptStatus.PENDING;
        this.payloadEnc = payloadEnc;
    }

    /**
     * prepare 단계 엔트리. status는 PENDING으로 시작. createdAt은 DB default로 채워진다.
     */
    public static PaymentAttempt start(String orderIdExternal, PaymentContext context,
                                       long amount, String payloadEnc) {
        return new PaymentAttempt(orderIdExternal, context, amount, payloadEnc);
    }

    /**
     * confirm 호출 직전 검증. PENDING 또는 RETRYABLE 상태여야 하고, 클라이언트가 전달한
     * 금액이 prepare 시점에 저장된 amount와 일치해야 한다. 불일치 시 {@code 400 INVALID_INPUT}.
     */
    public void requireConfirmable(long expectedAmount) {
        this.status.requireConfirmable();
        requireAmount(expectedAmount);
    }

    /** 처리 중인 동일 요청을 판별할 때 상태와 무관하게 금액/paymentKey를 비교한다. */
    public void requireMatchingRequest(long expectedAmount, String requestedPaymentKey) {
        requireAmount(expectedAmount);
        requireSamePaymentKey(requestedPaymentKey);
    }

    private void requireAmount(long expectedAmount) {
        if (this.amount != expectedAmount) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "결제 금액이 일치하지 않습니다.");
        }
    }

    /** confirm 실행권을 선점한다. PENDING/RETRYABLE → PROCESSING. */
    public void startProcessing(long expectedAmount, String paymentKey, LocalDateTime processingAt) {
        requireConfirmable(expectedAmount);
        requireSamePaymentKey(paymentKey);
        this.status = PaymentAttemptStatus.PROCESSING;
        this.paymentKey = paymentKey;
        this.processingAt = processingAt;
        this.failReason = null;
    }

    /** 제한 시간을 넘긴 PROCESSING 요청을 동일 paymentKey로 다시 선점한다. */
    public void restartProcessing(String paymentKey, LocalDateTime processingAt) {
        requireStatus(PaymentAttemptStatus.PROCESSING);
        requireSamePaymentKey(paymentKey);
        this.processingAt = processingAt;
    }

    /** PG 승인 또는 amount=0 내부 승인이 끝나 도메인 생성을 수행할 수 있다. */
    public void markApproved(String pgRef, LocalDateTime approvedAt) {
        requireStatus(PaymentAttemptStatus.PROCESSING);
        if (amount > 0 && (pgRef == null || pgRef.isBlank())) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "PG 승인 결과의 paymentKey가 누락되었습니다.");
        }
        if (amount == 0 && pgRef != null && !pgRef.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "0원 결제에는 PG 승인 키를 저장할 수 없습니다.");
        }
        this.status = PaymentAttemptStatus.APPROVED;
        this.pgRef = pgRef;
        this.confirmedAt = approvedAt;
    }

    /** 도메인 생성까지 완료한다. APPROVED → CONFIRMED. */
    public void markConfirmed() {
        requireStatus(PaymentAttemptStatus.APPROVED);
        this.status = PaymentAttemptStatus.CONFIRMED;
        this.failReason = null;
    }

    /** 네트워크 타임아웃처럼 같은 멱등키로 다시 확인할 수 있는 PG 실패다. */
    public void markRetryable(String reason) {
        requireStatus(PaymentAttemptStatus.PROCESSING);
        this.status = PaymentAttemptStatus.RETRYABLE;
        this.failReason = reason;
    }

    /** 최종 PG 실패 또는 amount=0 도메인 생성 실패다. */
    public void markFailed(String reason) {
        requireStatus(PaymentAttemptStatus.PROCESSING, PaymentAttemptStatus.APPROVED);
        this.status = PaymentAttemptStatus.FAILED;
        this.failReason = reason;
    }

    /** PG 승인 후 도메인 생성 실패로 보상 환불을 요청한다. */
    public void markCompensationRequested(String reason) {
        requireStatus(PaymentAttemptStatus.APPROVED);
        this.status = PaymentAttemptStatus.COMPENSATION_REQUESTED;
        this.failReason = reason;
    }

    public void markCompensationFailed(String reason) {
        requireStatus(
                PaymentAttemptStatus.COMPENSATION_REQUESTED,
                PaymentAttemptStatus.COMPENSATION_FAILED);
        this.status = PaymentAttemptStatus.COMPENSATION_FAILED;
        this.failReason = reason;
    }

    public void markCompensated() {
        requireStatus(
                PaymentAttemptStatus.COMPENSATION_REQUESTED,
                PaymentAttemptStatus.COMPENSATION_FAILED);
        this.status = PaymentAttemptStatus.COMPENSATED;
    }

    /** 사용자 포기/타임아웃 시 배치/어드민이 호출. */
    public void markCanceled() {
        this.status = PaymentAttemptStatus.CANCELED;
    }

    public Long getId() { return id; }
    public String getOrderIdExternal() { return orderIdExternal; }
    public PaymentContext getContext() { return context; }
    public long getAmount() { return amount; }
    public PaymentAttemptStatus getStatus() { return status; }
    public String getPaymentKey() { return paymentKey; }
    public String getPgRef() { return pgRef; }
    public String getFailReason() { return failReason; }
    public String getPayloadEnc() { return payloadEnc; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessingAt() { return processingAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public long getVersion() { return version; }

    private void requireSamePaymentKey(String requestedPaymentKey) {
        if (amount == 0) {
            if (requestedPaymentKey != null && !requestedPaymentKey.isBlank()) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "0원 결제에는 paymentKey를 사용할 수 없습니다.");
            }
            return;
        }
        if (requestedPaymentKey == null || requestedPaymentKey.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "paymentKey가 누락되었습니다.");
        }
        if (paymentKey != null && !paymentKey.equals(requestedPaymentKey)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "처음 요청한 paymentKey와 일치하지 않습니다.");
        }
    }

    private void requireStatus(PaymentAttemptStatus... allowedStatuses) {
        for (PaymentAttemptStatus allowedStatus : allowedStatuses) {
            if (status == allowedStatus) {
                return;
            }
        }
        throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
                "결제 상태를 변경할 수 없습니다. (현재: " + status + ")");
    }
}

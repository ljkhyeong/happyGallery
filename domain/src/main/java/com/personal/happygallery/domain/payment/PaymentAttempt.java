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
import java.util.UUID;

/**
 * PG 결제 확정 시도 — payment_attempt 테이블.
 *
 * <p>서버가 prepare 단계에서 orderIdExternal(UUID)과 amount를 확정해
 * {@link PaymentAttemptStatus#PENDING}으로 생성한다. confirm 단계에서는 짧은 트랜잭션으로
 * PROCESSING과 실행권 토큰을 선점하고, 트랜잭션 밖 PG 승인 후 현재 토큰과 일치하는 결과만
 * APPROVED로 저장한 다음 도메인 생성과 CONFIRMED 전이를 한 트랜잭션으로 완료한다.
 *
 * <p>서버가 orderId와 amount를 둘 다 쥐는 것이 핵심이다. 클라이언트가 금액을 속여도
 * prepare 시점의 amount와 confirm 시점에 들어온 amount가 다르면 {@link #startProcessing(long, String, LocalDateTime)}에서
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

    @Column(name = "processing_token", length = 64)
    private String processingToken;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "confirmed_payment_key", length = 200)
    private String confirmedPaymentKey;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Lob
    @Column(name = "payload_enc", columnDefinition = "MEDIUMTEXT")
    private String payloadEnc;

    @Column(name = "fulfilled_domain_id")
    private Long fulfilledDomainId;

    @Lob
    @Column(name = "fulfilled_access_token_enc", columnDefinition = "MEDIUMTEXT")
    private String fulfilledAccessTokenEnc;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirm_recovery_attempted_at")
    private LocalDateTime confirmRecoveryAttemptedAt;

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
        PaymentAmountPolicy.requireValid(amount);
        return new PaymentAttempt(orderIdExternal, context, amount, payloadEnc);
    }

    /** 기존 결제 시도와 동일한 confirm 요청인지 상태와 무관하게 금액/paymentKey를 비교한다. */
    public void requireMatchingConfirmRequest(long requestedAmount, String requestedPaymentKey) {
        requireRequestedAmount(requestedAmount);
        requireSamePaymentKey(requestedPaymentKey);
    }

    private void requireRequestedAmount(long requestedAmount) {
        if (this.amount != requestedAmount) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "결제 금액이 일치하지 않습니다.");
        }
    }

    /** confirm 실행권을 선점한다. PENDING/RETRYABLE → PROCESSING. */
    public String startProcessing(long requestedAmount, String paymentKey, LocalDateTime processingAt) {
        status.requireConfirmable();
        requireRequestedAmount(requestedAmount);
        requireSamePaymentKey(paymentKey);
        this.status = PaymentAttemptStatus.PROCESSING;
        this.paymentKey = paymentKey;
        this.processingAt = processingAt;
        this.processingToken = UUID.randomUUID().toString();
        this.failReason = null;
        return this.processingToken;
    }

    /** 제한 시간을 넘긴 PROCESSING 요청을 동일 금액/paymentKey로 다시 선점한다. */
    public String restartProcessing(long requestedAmount, String paymentKey, LocalDateTime processingAt) {
        requireStatus(PaymentAttemptStatus.PROCESSING);
        requireMatchingConfirmRequest(requestedAmount, paymentKey);
        this.processingAt = processingAt;
        this.processingToken = UUID.randomUUID().toString();
        this.failReason = null;
        return this.processingToken;
    }

    /** PG 승인 또는 amount=0 내부 승인이 끝나 도메인 생성을 수행할 수 있다. */
    public boolean markApproved(String expectedProcessingToken,
                                String confirmedPaymentKey,
                                LocalDateTime approvedAt) {
        if (!ownsProcessing(expectedProcessingToken)) {
            return false;
        }
        applyApproval(confirmedPaymentKey, approvedAt);
        return true;
    }

    /**
     * 재선점 뒤 늦게 도착한 PG 성공을 로컬 상태와 화해한다.
     * 실패 결과와 달리 외부 승인은 이미 성립한 사실이므로, 새 실행이 처리 중이거나 실패로 끝났어도
     * 도메인 생성 전 APPROVED로 단조 전이한다.
     */
    public boolean reconcileLatePgApproval(String confirmedPaymentKey, LocalDateTime approvedAt) {
        if (status != PaymentAttemptStatus.PROCESSING
                && status != PaymentAttemptStatus.RETRYABLE
                && status != PaymentAttemptStatus.FAILED
                && status != PaymentAttemptStatus.RECONCILIATION_REQUIRED) {
            return false;
        }
        applyApproval(confirmedPaymentKey, approvedAt);
        return true;
    }

    private void applyApproval(String confirmedPaymentKey, LocalDateTime approvedAt) {
        boolean hasConfirmedPaymentKey = confirmedPaymentKey != null && !confirmedPaymentKey.isBlank();
        if (amount > 0 && !hasConfirmedPaymentKey) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "PG 승인 결과의 paymentKey가 누락되었습니다.");
        }
        if (amount == 0 && hasConfirmedPaymentKey) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "0원 결제에는 PG 승인 키를 저장할 수 없습니다.");
        }
        this.status = PaymentAttemptStatus.APPROVED;
        this.confirmedPaymentKey = confirmedPaymentKey;
        this.confirmedAt = approvedAt;
        this.processingToken = null;
    }

    /** 도메인 생성 결과를 보존하고 APPROVED → CONFIRMED로 전이한다. */
    public void markConfirmed(Long domainId, String accessTokenEnc) {
        requireStatus(PaymentAttemptStatus.APPROVED);
        if (domainId == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "완료된 결제의 도메인 ID가 누락되었습니다.");
        }
        this.status = PaymentAttemptStatus.CONFIRMED;
        this.fulfilledDomainId = domainId;
        this.fulfilledAccessTokenEnc = accessTokenEnc;
        this.failReason = null;
    }

    /** 네트워크 타임아웃처럼 같은 멱등키로 다시 확인할 수 있는 PG 실패다. */
    public boolean markRetryable(String expectedProcessingToken, String reason) {
        if (!ownsProcessing(expectedProcessingToken)) {
            return false;
        }
        this.status = PaymentAttemptStatus.RETRYABLE;
        this.failReason = reason;
        this.processingToken = null;
        return true;
    }

    /** 현재 선점자가 받은 최종 PG 실패다. */
    public boolean markProcessingFailed(String expectedProcessingToken, String reason) {
        if (!ownsProcessing(expectedProcessingToken)) {
            return false;
        }
        this.status = PaymentAttemptStatus.FAILED;
        this.failReason = reason;
        this.processingToken = null;
        return true;
    }

    /** amount=0 내부 승인 뒤 도메인 생성이 실패했다. */
    public void markFailed(String reason) {
        requireStatus(PaymentAttemptStatus.APPROVED);
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

    /** prepare 유효시간이 지난 미시작 결제를 취소하고 개인정보 payload를 제거한다. */
    public boolean expirePendingBefore(LocalDateTime cutoff) {
        if (status != PaymentAttemptStatus.PENDING
                || createdAt == null
                || createdAt.isAfter(cutoff)) {
            return false;
        }
        this.status = PaymentAttemptStatus.CANCELED;
        this.processingToken = null;
        this.payloadEnc = null;
        this.failReason = "결제 준비 유효시간이 만료되었습니다.";
        return true;
    }

    /**
     * confirm 도중 프로세스 또는 DB 장애로 중단된 시도인지 판정한다.
     *
     * <p>PG 결과 저장 전인 PROCESSING은 선점 시각을, PG 승인 저장 후인 APPROVED는 승인 시각을 기준으로 한다.
     * 시각이 없는 과거 데이터는 생성 시각까지 제한 시간을 넘긴 경우에만 복구한다.
     */
    public boolean isConfirmRecoveryCandidate(LocalDateTime activityStaleBefore,
                                              LocalDateTime createdAtStaleBeforeUtc) {
        if (confirmRecoveryAttemptedAt != null
                && !isAtOrBefore(confirmRecoveryAttemptedAt, activityStaleBefore)) {
            return false;
        }
        return switch (status) {
            case PROCESSING, RETRYABLE -> processingAt != null
                    ? isAtOrBefore(processingAt, activityStaleBefore)
                    : isAtOrBefore(createdAt, createdAtStaleBeforeUtc);
            case APPROVED -> confirmedAt != null
                    ? isAtOrBefore(confirmedAt, activityStaleBefore)
                    : isAtOrBefore(createdAt, createdAtStaleBeforeUtc);
            default -> false;
        };
    }

    /** 자동 복구 시도 간 backoff와 후보 순환을 위한 마지막 시각을 기록한다. */
    public void markConfirmRecoveryAttempted(LocalDateTime attemptedAt) {
        requireStatus(
                PaymentAttemptStatus.PROCESSING,
                PaymentAttemptStatus.RETRYABLE,
                PaymentAttemptStatus.APPROVED);
        this.confirmRecoveryAttemptedAt = attemptedAt;
    }

    /** Toss 멱등 응답 안전 구간을 지난 미확정 PG 호출을 수동 대사 대상으로 격리한다. */
    public void markConfirmReconciliationRequired(String reason) {
        requireStatus(PaymentAttemptStatus.PROCESSING, PaymentAttemptStatus.RETRYABLE);
        this.status = PaymentAttemptStatus.RECONCILIATION_REQUIRED;
        this.processingToken = null;
        this.failReason = reason;
    }

    /** PG 조회로 승인되지 않았음이 확정된 대사 대상을 실패로 종결한다. */
    public void markReconciledNotApproved(String reason) {
        requireStatus(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
        this.status = PaymentAttemptStatus.FAILED;
        this.processingToken = null;
        this.payloadEnc = null;
        this.failReason = reason;
    }

    public boolean requiresConfirmReconciliation(LocalDateTime automaticRetrySafeSince) {
        return amount > 0L
                && (status == PaymentAttemptStatus.PROCESSING || status == PaymentAttemptStatus.RETRYABLE)
                && (createdAt == null || createdAt.isBefore(automaticRetrySafeSince));
    }

    /** 보존 기간이 지난 최종 결제 시도의 개인정보 암호문만 제거한다. */
    public boolean clearSensitiveDataBefore(LocalDateTime cutoff) {
        if (!status.isSensitiveDataCleanupAllowed()
                || createdAt == null
                || createdAt.isAfter(cutoff)
                || (payloadEnc == null && fulfilledAccessTokenEnc == null)) {
            return false;
        }
        payloadEnc = null;
        fulfilledAccessTokenEnc = null;
        return true;
    }

    public Long getId() { return id; }
    public String getOrderIdExternal() { return orderIdExternal; }
    public PaymentContext getContext() { return context; }
    public long getAmount() { return amount; }
    public PaymentAttemptStatus getStatus() { return status; }
    public String getPaymentKey() { return paymentKey; }
    public String getConfirmedPaymentKey() { return confirmedPaymentKey; }
    public String getFailReason() { return failReason; }
    public String getPayloadEnc() { return payloadEnc; }
    public Long getFulfilledDomainId() { return fulfilledDomainId; }
    public String getFulfilledAccessTokenEnc() { return fulfilledAccessTokenEnc; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessingAt() { return processingAt; }
    public String getProcessingToken() { return processingToken; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getConfirmRecoveryAttemptedAt() { return confirmRecoveryAttemptedAt; }
    public long getVersion() { return version; }

    private void requireSamePaymentKey(String requestedPaymentKey) {
        boolean hasRequestedPaymentKey = requestedPaymentKey != null && !requestedPaymentKey.isBlank();
        if (amount == 0) {
            if (hasRequestedPaymentKey) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "0원 결제에는 paymentKey를 사용할 수 없습니다.");
            }
            return;
        }
        if (!hasRequestedPaymentKey) {
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

    private boolean ownsProcessing(String expectedProcessingToken) {
        return status == PaymentAttemptStatus.PROCESSING
                && processingToken != null
                && processingToken.equals(expectedProcessingToken);
    }

    private boolean isAtOrBefore(LocalDateTime occurredAt, LocalDateTime boundary) {
        return occurredAt != null && !occurredAt.isAfter(boundary);
    }
}

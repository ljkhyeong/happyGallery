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
 * {@link PaymentAttemptStatus#PENDING}으로 생성하고, 프론트가 PG 결제창을 통과한 뒤
 * confirm 단계에서 paymentKey를 붙여 {@link PaymentAttemptStatus#CONFIRMED}로 전이한다.
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
    @Column(nullable = false, length = 20)
    private PaymentAttemptStatus status;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "pg_ref", length = 200)
    private String pgRef;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PaymentAttempt() {}

    private PaymentAttempt(String orderIdExternal, PaymentContext context, long amount, String payloadJson) {
        this.orderIdExternal = orderIdExternal;
        this.context = context;
        this.amount = amount;
        this.status = PaymentAttemptStatus.PENDING;
        this.payloadJson = payloadJson;
    }

    /**
     * prepare 단계 엔트리. status는 PENDING으로 시작. createdAt은 DB default로 채워진다.
     */
    public static PaymentAttempt start(String orderIdExternal, PaymentContext context,
                                       long amount, String payloadJson) {
        return new PaymentAttempt(orderIdExternal, context, amount, payloadJson);
    }

    /**
     * confirm 호출 직전 검증. PENDING 상태여야 하고, 클라이언트가 전달한 금액이
     * prepare 시점에 저장된 amount와 일치해야 한다. 불일치 시 {@code 400 INVALID_INPUT}.
     */
    public void requireConfirmable(long expectedAmount) {
        this.status.requireConfirmable();
        if (this.amount != expectedAmount) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "결제 금액이 일치하지 않습니다.");
        }
    }

    /** PG confirm 성공 시 호출. PENDING → CONFIRMED. */
    public void markConfirmed(String paymentKey, String pgRef, LocalDateTime now) {
        this.status.requireConfirmable();
        this.status = PaymentAttemptStatus.CONFIRMED;
        this.paymentKey = paymentKey;
        this.pgRef = pgRef;
        this.confirmedAt = now;
    }

    /** PG confirm 실패 시 호출. 재시도 금지 처리. */
    public void markFailed() {
        this.status = PaymentAttemptStatus.FAILED;
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
    public String getPayloadJson() { return payloadJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public long getVersion() { return version; }
}

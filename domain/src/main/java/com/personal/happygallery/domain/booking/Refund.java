package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.payment.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 환불 요청 — 원결제 참조와 환불 결과 참조를 분리해 재시도 가능성을 보존한다. */
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RefundStatus status;

    @Column(name = "pg_ref", length = 255)
    private String originalPgRef;

    @Column(name = "refund_pg_ref", length = 255)
    private String refundPgRef;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Refund() {}

    private Refund(Booking booking, Long orderId, long amount, String originalPgRef) {
        this.booking = booking;
        this.orderId = orderId;
        this.amount = amount;
        this.originalPgRef = originalPgRef;
        this.status = RefundStatus.REQUESTED;
    }

    /** 예약금 환불 요청 생성 (booking 취소 시). */
    public static Refund forBooking(Booking booking, long amount) {
        return new Refund(booking, null, amount, booking.getPaymentKey());
    }

    /** 주문 환불 요청 생성 (주문 거절/자동환불 시). booking은 null. */
    public static Refund forOrder(Long orderId, long amount, String originalPgRef) {
        return new Refund(null, orderId, amount, originalPgRef);
    }

    /** PG 환불 성공 처리 */
    public void markSucceeded(String refundPgRef) {
        this.status = RefundStatus.SUCCEEDED;
        this.refundPgRef = refundPgRef;
    }

    /** PG 환불 실패 처리 — 레코드는 삭제하지 않고 FAILED 로 유지 (운영자 재시도 대상) */
    public void markFailed(String reason) {
        this.status = RefundStatus.FAILED;
        this.failReason = reason;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public Long getOrderId() { return orderId; }
    public long getAmount() { return amount; }
    public RefundStatus getStatus() { return status; }
    public String getOriginalPgRef() { return originalPgRef; }
    public String getRefundPgRef() { return refundPgRef; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

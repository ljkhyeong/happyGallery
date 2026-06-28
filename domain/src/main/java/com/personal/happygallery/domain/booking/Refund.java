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
import java.time.LocalDateTime;
import java.util.Objects;

/** 환불 요청 — 원결제 참조와 환불 결과 참조를 분리해 재시도 가능성을 보존한다. */
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

    private Refund(Long bookingId, Long orderId, Long passPurchaseId, long amount, String originalPgRef) {
        this.bookingId = bookingId;
        this.orderId = orderId;
        this.passPurchaseId = passPurchaseId;
        this.amount = amount;
        this.originalPgRef = originalPgRef;
        this.status = RefundStatus.REQUESTED;
    }

    /** 예약금 환불 요청 생성 (booking 취소 시). */
    public static Refund forBooking(Booking booking, long amount) {
        Objects.requireNonNull(booking, "booking must not be null");
        Long bookingId = Objects.requireNonNull(booking.getId(), "bookingId must not be null");
        return new Refund(bookingId, null, null, amount, booking.getPaymentKey());
    }

    /** 주문 환불 요청 생성 (주문 거절/자동환불 시). bookingId는 null. */
    public static Refund forOrder(Long orderId, long amount, String originalPgRef) {
        return new Refund(
                null,
                Objects.requireNonNull(orderId, "orderId must not be null"),
                null,
                amount,
                originalPgRef);
    }

    /** 8회권 환불 요청 생성. bookingId/orderId는 null. */
    public static Refund forPass(Long passPurchaseId, long amount, String originalPgRef) {
        return new Refund(null, null, Objects.requireNonNull(passPurchaseId, "passPurchaseId must not be null"),
                amount, originalPgRef);
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
    public Long getBookingId() { return bookingId; }
    public Long getOrderId() { return orderId; }
    public Long getPassPurchaseId() { return passPurchaseId; }
    public long getAmount() { return amount; }
    public RefundStatus getStatus() { return status; }
    public String getOriginalPgRef() { return originalPgRef; }
    public String getRefundPgRef() { return refundPgRef; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

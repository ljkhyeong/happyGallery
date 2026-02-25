package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.order.RefundStatus;
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

/** 환불 요청 — refunds 테이블 (V2 기존 테이블, PG 연동 전 REQUESTED 상태로만 기록) */
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
    private String pgRef;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Refund() {}

    /** 예약금 환불 요청 생성 (booking 취소 시). order_id, pg_ref는 null. */
    public Refund(Booking booking, long amount) {
        this.booking = booking;
        this.amount = amount;
        this.status = RefundStatus.REQUESTED;
    }

    /** PG 환불 성공 처리 */
    public void markSucceeded(String pgRef) {
        this.status = RefundStatus.SUCCEEDED;
        this.pgRef = pgRef;
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
    public String getPgRef() { return pgRef; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

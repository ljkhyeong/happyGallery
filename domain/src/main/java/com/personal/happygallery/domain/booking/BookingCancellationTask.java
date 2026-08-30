package com.personal.happygallery.domain.booking;

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

/** 공방 사정 예약 취소 후 운영자가 직접 마무리해야 하는 후속 작업. */
@Entity
@Table(name = "booking_cancellation_tasks")
public class BookingCancellationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private BookingCancellationTaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BookingCancellationTaskStatus status;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "completed_by_admin_id")
    private Long completedByAdminId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected BookingCancellationTask() {}

    private BookingCancellationTask(
            Booking booking,
            BookingCancellationTaskType type,
            String reason
    ) {
        this.booking = booking;
        this.type = type;
        this.status = BookingCancellationTaskStatus.PENDING;
        this.reason = reason;
    }

    public static BookingCancellationTask pending(
            Booking booking,
            BookingCancellationTaskType type,
            String reason
    ) {
        return new BookingCancellationTask(booking, type, reason);
    }

    /**
     * 관리자가 작업을 완료한다.
     *
     * @return 이번 호출에서 상태가 변경되었으면 {@code true}, 이미 완료된 작업이면 {@code false}
     */
    public boolean complete(Long adminId, LocalDateTime now) {
        boolean changed = switch (status) {
            case PENDING -> true;
            case COMPLETED -> false;
        };
        if (!changed) {
            return false;
        }
        status = BookingCancellationTaskStatus.COMPLETED;
        completedByAdminId = adminId;
        completedAt = now;
        return true;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public BookingCancellationTaskType getType() { return type; }
    public BookingCancellationTaskStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public Long getCompletedByAdminId() { return completedByAdminId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

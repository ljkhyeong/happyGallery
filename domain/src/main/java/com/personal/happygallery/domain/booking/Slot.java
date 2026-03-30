package com.personal.happygallery.domain.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 체험 예약 슬롯 — slots 테이블 */
@Entity
@Table(name = "slots")
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private BookingClass bookingClass;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private int capacity = SlotCapacity.MAX;

    @Column(name = "booked_count", nullable = false)
    private int bookedCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected Slot() {}

    public Slot(BookingClass bookingClass, LocalDateTime startAt, LocalDateTime endAt) {
        this.bookingClass = bookingClass;
        this.startAt = startAt;
        this.endAt = endAt;
        this.capacity = SlotCapacity.MAX;
        this.bookedCount = 0;
        this.isActive = true;
    }

    /** 운영자 또는 버퍼 정책에 의해 슬롯을 비활성화한다. */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 예약을 1건 추가한다. 정원 초과 시 {@link com.personal.happygallery.domain.error.CapacityExceededException}을 던진다.
     * 호출 전 반드시 비관적 락(SELECT FOR UPDATE)으로 row를 잠가야 한다.
     */
    public void incrementBookedCount() {
        SlotCapacity.checkAvailable(this.bookedCount);
        this.bookedCount++;
    }

    /**
     * 예약을 1건 반납한다(변경/취소 시).
     * booked_count가 0이면 IllegalStateException을 던진다.
     * 호출 전 반드시 비관적 락(SELECT FOR UPDATE)으로 row를 잠가야 한다.
     */
    public void decrementBookedCount() {
        if (this.bookedCount <= 0) {
            throw new IllegalStateException("booked_count는 0 이하로 감소할 수 없습니다.");
        }
        this.bookedCount--;
    }

    public Long getId() { return id; }
    public BookingClass getBookingClass() { return bookingClass; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public int getCapacity() { return capacity; }
    public int getBookedCount() { return bookedCount; }
    public boolean isActive() { return isActive; }
}

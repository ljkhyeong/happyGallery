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

    @Column(name = "admin_active", nullable = false)
    private boolean adminActive = true;

    @Column(name = "buffer_block_count", nullable = false)
    private int bufferBlockCount = 0;

    protected Slot() {}

    public Slot(BookingClass bookingClass, LocalDateTime startAt) {
        this(bookingClass, startAt, startAt.plusMinutes(bookingClass.getDurationMin()));
    }

    public Slot(BookingClass bookingClass, LocalDateTime startAt, LocalDateTime endAt) {
        this.bookingClass = bookingClass;
        this.startAt = startAt;
        this.endAt = endAt;
        this.capacity = SlotCapacity.MAX;
        this.bookedCount = 0;
        this.adminActive = true;
        this.bufferBlockCount = 0;
    }

    /** 운영자가 슬롯을 비활성화한다. 버퍼 차단 해제와 무관하게 유지된다. */
    public void deactivate() {
        this.adminActive = false;
    }

    /** 다른 슬롯의 예약으로 인해 이 슬롯을 막는 버퍼가 하나 추가된다. */
    public void incrementBufferBlockCount() {
        this.bufferBlockCount++;
    }

    /** 원인 슬롯의 마지막 예약이 사라져 버퍼 차단 하나를 해제한다. */
    public void decrementBufferBlockCount() {
        if (this.bufferBlockCount <= 0) {
            throw new IllegalStateException("buffer_block_count는 0 이하로 감소할 수 없습니다.");
        }
        this.bufferBlockCount--;
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
    public boolean isAdminActive() { return adminActive; }
    public boolean isBufferBlocked() { return bufferBlockCount > 0; }
    public boolean isActive() { return adminActive && bufferBlockCount == 0; }
    public boolean hasBookings() { return bookedCount > 0; }
    public boolean isReservableAt(LocalDateTime now) { return isActive() && startAt.isAfter(now); }
}

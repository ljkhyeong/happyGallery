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

/** 예약 변경 이력 — booking_history 테이블 (append-only) */
@Entity
@Table(name = "booking_history")
public class BookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BookingHistoryAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_slot_id")
    private Slot fromSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_slot_id")
    private Slot toSlot;

    /** CUSTOMER | ADMIN */
    @Column(nullable = false, length = 10)
    private String actor;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BookingHistory() {}

    public BookingHistory(Booking booking, BookingHistoryAction action,
                          Slot fromSlot, Slot toSlot, String actor, String reason) {
        this.booking = booking;
        this.action = action;
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.actor = actor;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public BookingHistoryAction getAction() { return action; }
    public Slot getFromSlot() { return fromSlot; }
    public Slot getToSlot() { return toSlot; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

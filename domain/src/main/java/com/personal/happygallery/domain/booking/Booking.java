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
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/** 체험 예약 — bookings 테이블 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private BookingClass bookingClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BookingStatus status;

    @Column(name = "deposit_amount", nullable = false)
    private long depositAmount;

    @Column(name = "deposit_paid_at")
    private LocalDateTime depositPaidAt;

    @Column(name = "balance_amount", nullable = false)
    private long balanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_status", nullable = false, length = 10)
    private BalanceStatus balanceStatus;

    @Column(name = "arrears_flag", nullable = false)
    private boolean arrearsFlag = false;

    @Version
    @Column(nullable = false)
    private long version;

    /** 비회원 예약 조회용 토큰 (V3에서 추가) */
    @Column(name = "access_token", length = 64)
    private String accessToken;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Booking() {}

    /**
     * 게스트 예약 생성.
     *
     * @param guest         예약자 (비회원)
     * @param slot          예약 슬롯
     * @param depositAmount 예약금 (원)
     * @param balanceAmount 잔금 (원) = 클래스 가격 - 예약금
     * @param accessToken   비회원 조회용 토큰 (UUID 기반 32자)
     */
    public Booking(Guest guest, Slot slot, long depositAmount, long balanceAmount, String accessToken) {
        this.guest = guest;
        this.bookingClass = slot.getBookingClass();
        this.slot = slot;
        this.status = BookingStatus.BOOKED;
        this.depositAmount = depositAmount;
        this.balanceAmount = balanceAmount;
        this.balanceStatus = BalanceStatus.UNPAID;
        this.arrearsFlag = false;
        this.accessToken = accessToken;
    }

    /**
     * 예약 슬롯을 변경한다. 상태는 BOOKED를 유지한다.
     * 호출 후 저장 시 {@code @Version}으로 낙관적 락 충돌을 감지한다.
     */
    public void reschedule(Slot newSlot) {
        this.slot = newSlot;
    }

    public Long getId() { return id; }
    public Guest getGuest() { return guest; }
    public BookingClass getBookingClass() { return bookingClass; }
    public Slot getSlot() { return slot; }
    public BookingStatus getStatus() { return status; }
    public long getDepositAmount() { return depositAmount; }
    public LocalDateTime getDepositPaidAt() { return depositPaidAt; }
    public long getBalanceAmount() { return balanceAmount; }
    public BalanceStatus getBalanceStatus() { return balanceStatus; }
    public boolean isArrearsFlag() { return arrearsFlag; }
    public long getVersion() { return version; }
    public String getAccessToken() { return accessToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

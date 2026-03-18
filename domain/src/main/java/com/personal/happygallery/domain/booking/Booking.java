package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.pass.PassPurchase;
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

    @Column(name = "user_id")
    private Long userId;

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

    /** 예약금 결제 수단 (V4에서 추가). BANK_TRANSFER는 생성 시점에서 차단됨. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 15)
    private DepositPaymentMethod paymentMethod;

    /** 비회원 예약 조회용 토큰 (V3에서 추가) */
    @Column(name = "access_token", length = 64)
    private String accessToken;

    /** 8회권 결제 연결 (V5에서 추가). null이면 예약금 결제. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pass_purchase_id")
    private PassPurchase passPurchase;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Booking() {}

    private Booking(Guest guest, Long userId, Slot slot, long depositAmount, long balanceAmount,
                    DepositPaymentMethod paymentMethod, PassPurchase passPurchase, String accessToken) {
        this.guest = guest;
        this.userId = userId;
        this.bookingClass = slot.getBookingClass();
        this.slot = slot;
        this.status = BookingStatus.BOOKED;
        this.depositAmount = depositAmount;
        this.balanceAmount = balanceAmount;
        this.balanceStatus = BalanceStatus.UNPAID;
        this.arrearsFlag = false;
        this.paymentMethod = paymentMethod;
        this.passPurchase = passPurchase;
        this.accessToken = accessToken;
    }

    /** 게스트 예약금 예약 생성. */
    public static Booking forGuestDeposit(Guest guest, Slot slot, long depositAmount, long balanceAmount,
                                          DepositPaymentMethod paymentMethod, String accessToken) {
        return new Booking(guest, null, slot, depositAmount, balanceAmount, paymentMethod, null, accessToken);
    }

    /** 게스트 8회권 예약 생성. depositAmount/balanceAmount=0, paymentMethod=null. */
    public static Booking forGuestPass(Guest guest, Slot slot, PassPurchase passPurchase, String accessToken) {
        return new Booking(guest, null, slot, 0, 0, null, passPurchase, accessToken);
    }

    /** 회원 예약금 예약 생성. */
    public static Booking forMemberDeposit(Long userId, Slot slot, long depositAmount, long balanceAmount,
                                           DepositPaymentMethod paymentMethod) {
        return new Booking(null, userId, slot, depositAmount, balanceAmount, paymentMethod, null, null);
    }

    /** 회원 8회권 예약 생성. depositAmount/balanceAmount=0, paymentMethod=null. */
    public static Booking forMemberPass(Long userId, Slot slot, PassPurchase passPurchase) {
        return new Booking(null, userId, slot, 0, 0, null, passPurchase, null);
    }

    /**
     * 예약 슬롯을 변경한다. 상태는 BOOKED를 유지한다.
     * 호출 후 저장 시 {@code @Version}으로 낙관적 락 충돌을 감지한다.
     */
    public void reschedule(Slot newSlot) {
        this.slot = newSlot;
    }

    /**
     * 예약을 취소한다. 상태를 CANCELED로 변경한다.
     * 환불 가능 여부는 호출자가 {@link com.personal.happygallery.common.time.TimeBoundary#isRefundable}로 판단한다.
     */
    public void cancel() {
        this.status = BookingStatus.CANCELED;
    }

    /** 결석 처리. 크레딧은 예약 시 이미 소모되었으므로 상태만 변경. */
    public void markNoShow() {
        this.status = BookingStatus.NO_SHOW;
    }

    public void claimToUser(Long userId) {
        this.userId = userId;
        this.guest = null;
    }

    /** 8회권으로 결제된 예약인지 여부. */
    public boolean isPassBooking() {
        return passPurchase != null;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
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
    public DepositPaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getAccessToken() { return accessToken; }
    public PassPurchase getPassPurchase() { return passPurchase; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

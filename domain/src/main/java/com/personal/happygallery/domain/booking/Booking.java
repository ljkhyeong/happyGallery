package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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

    @Column(name = "balance_paid_at")
    private LocalDateTime balancePaidAt;

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

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    /** 8회권 결제 연결 (V5에서 추가). null이면 예약금 결제. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pass_purchase_id")
    private PassPurchase passPurchase;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Booking() {}

    private Booking(Guest guest, Long userId, Slot slot, long depositAmount, long balanceAmount,
                    DepositPaymentMethod paymentMethod, PassPurchase passPurchase, String accessToken) {
        requireExactlyOneOwner(guest, userId);
        this.guest = guest;
        this.userId = userId;
        this.bookingClass = slot.getBookingClass();
        this.slot = slot;
        this.status = BookingStatus.BOOKED;
        this.depositAmount = depositAmount;
        this.balanceAmount = balanceAmount;
        this.balanceStatus = balanceAmount == 0 ? BalanceStatus.PAID : BalanceStatus.UNPAID;
        this.balancePaidAt = null;
        this.arrearsFlag = false;
        this.paymentMethod = paymentMethod;
        this.passPurchase = passPurchase;
        this.accessToken = accessToken;
    }

    private static void requireExactlyOneOwner(Guest guest, Long userId) {
        if ((guest == null) == (userId == null)) {
            throw new IllegalArgumentException("예약은 회원 또는 비회원 소유자 중 하나만 가져야 합니다.");
        }
    }

    /** 게스트 예약금 예약 생성. */
    public static Booking forGuestDeposit(Guest guest, Slot slot, long depositAmount, long balanceAmount,
                                          DepositPaymentMethod paymentMethod, String accessToken) {
        return new Booking(guest, null, slot, depositAmount, balanceAmount, paymentMethod, null, accessToken);
    }

    /** 회원 예약금 예약 생성. */
    public static Booking forMemberDeposit(Long userId, Slot slot, long depositAmount, long balanceAmount,
                                           DepositPaymentMethod paymentMethod) {
        return new Booking(null, userId, slot, depositAmount, balanceAmount, paymentMethod, null, null);
    }

    /** 회원 8회권 예약 생성. depositAmount/balanceAmount=0, paymentMethod=null. */
    public static Booking forMemberPass(Long userId, Slot slot, PassPurchase passPurchase) {
        passPurchase.requireApplicableToClass(
                slot.getBookingClass().getCategory(), slot.getBookingClass().isPassEligible());
        return new Booking(null, userId, slot, 0, 0, null, passPurchase, null);
    }

    /**
     * 같은 클래스의 슬롯으로 예약을 변경한다. 상태는 BOOKED를 유지한다.
     * 호출 후 저장 시 {@code @Version}으로 낙관적 락 충돌을 감지한다.
     */
    public void reschedule(Slot newSlot) {
        status.requireBooked();
        if (!isSameBookingClass(newSlot)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "같은 클래스의 슬롯으로만 예약을 변경할 수 있습니다.");
        }
        if (isPassBooking()) {
            passPurchase.requireApplicableToClass(
                    newSlot.getBookingClass().getCategory(), newSlot.getBookingClass().isPassEligible());
        }
        this.slot = newSlot;
    }

    private boolean isSameBookingClass(Slot newSlot) {
        BookingClass newBookingClass = newSlot.getBookingClass();
        return bookingClass == newBookingClass
                || (bookingClass.getId() != null && bookingClass.getId().equals(newBookingClass.getId()));
    }

    /**
     * 예약을 취소한다. 상태를 CANCELED로 변경한다.
     * 환불 가능 여부는 호출자가 {@link com.personal.happygallery.domain.time.TimeBoundary#isRefundable}로 판단한다.
     */
    public void cancel() {
        status.requireBooked();
        if (!isCustomerCancellationAllowed()) {
            throw new HappyGalleryException(
                    ErrorCode.CHANGE_NOT_ALLOWED,
                    "잔금 결제가 완료된 예약은 고객이 취소할 수 없습니다. 관리자 정산이 필요합니다.");
        }
        this.status = BookingStatus.CANCELED;
    }

    /** 공방 사정으로 예약을 취소한다. 고객 취소 제한과 관계없이 관리자 정산 흐름에서만 호출한다. */
    public void cancelByAdmin() {
        status.requireBooked();
        this.status = BookingStatus.CANCELED;
    }

    public boolean isCustomerCancellationAllowed() {
        return status == BookingStatus.BOOKED
                && (balanceAmount == 0 || balanceStatus != BalanceStatus.PAID);
    }

    /** 수업 종료 후 결석 처리. 크레딧은 예약 시 이미 소모되었으므로 상태만 변경. */
    public void markNoShow(LocalDateTime now) {
        status.requireBooked();
        if (now.isBefore(slot.getEndAt())) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "수업 종료 전에는 노쇼 처리할 수 없습니다.");
        }
        this.status = BookingStatus.NO_SHOW;
    }

    /** 현장 잔금 결제를 기록한다. 이미 결제된 예약은 최초 결제 시각을 유지한다. */
    public void markBalancePaid(LocalDateTime paidAt) {
        requireSettlementEditable();
        if (balanceStatus == BalanceStatus.PAID) {
            return;
        }
        this.balanceStatus = BalanceStatus.PAID;
        this.balancePaidAt = paidAt;
        this.arrearsFlag = false;
    }

    /** 미수 여부를 갱신한다. 결제 완료된 잔금은 미수로 되돌릴 수 없다. */
    public void updateArrears(boolean arrears) {
        requireSettlementEditable();
        if (arrears && balanceStatus == BalanceStatus.PAID) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "결제 완료된 잔금은 미수로 표시할 수 없습니다.");
        }
        this.arrearsFlag = arrears;
    }

    /** 수업 종료 후 예약을 완료한다. 미결제 잔금은 먼저 미수로 명시해야 한다. */
    public void complete(LocalDateTime now) {
        status.requireBooked();
        if (now.isBefore(slot.getEndAt())) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "수업 종료 전에는 예약을 완료할 수 없습니다.");
        }
        if (balanceStatus == BalanceStatus.UNPAID && !arrearsFlag) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "미결제 잔금은 미수로 표시한 뒤 완료해 주세요.");
        }
        this.status = BookingStatus.COMPLETED;
    }

    private void requireSettlementEditable() {
        if (status != BookingStatus.BOOKED && status != BookingStatus.COMPLETED) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "예약 중이거나 완료된 예약만 잔금을 정산할 수 있습니다. 현재: " + status);
        }
    }

    public void claimToUser(Long userId) {
        requireExactlyOneOwner(null, userId);
        this.userId = userId;
        this.guest = null;
        this.accessToken = null;
    }

    /** 휴대폰 소유 확인 후 비회원 예약의 관리 토큰을 교체한다. */
    public void replaceGuestAccessToken(String accessToken) {
        if (guest == null) {
            throw new IllegalStateException("회원 예약에는 비회원 접근 토큰을 발급할 수 없습니다.");
        }
        this.accessToken = accessToken;
    }

    /** 결제 confirm 성공 후 원결제 식별자와 예약금 결제 시각을 저장한다. */
    public void recordPaymentConfirmation(String paymentKey, LocalDateTime paidAt) {
        this.paymentKey = paymentKey;
        if (!isPassBooking()) {
            this.depositPaidAt = paidAt;
        }
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
    public LocalDateTime getBalancePaidAt() { return balancePaidAt; }
    public boolean isArrearsFlag() { return arrearsFlag; }
    public long getVersion() { return version; }
    public DepositPaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getAccessToken() { return accessToken; }
    public String getPaymentKey() { return paymentKey; }
    public PassPurchase getPassPurchase() { return passPurchase; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

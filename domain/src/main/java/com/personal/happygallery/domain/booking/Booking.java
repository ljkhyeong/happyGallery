package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.user.User;
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
import java.math.BigInteger;
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

    @Column(name = "owner_phone_hmac", length = 64)
    private String ownerPhoneHmac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private BookingClass bookingClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingSource source;

    @Column(name = "deposit_amount", nullable = false)
    private long depositAmount;

    @Column(name = "deposit_paid_at")
    private LocalDateTime depositPaidAt;

    @Column(name = "balance_amount", nullable = false)
    private long balanceAmount;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

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

    /**
     * 예약금 결제 수단.
     * 고객 WEB 결제에서는 계좌이체를 차단하고, 관리자 오프라인 입금 기록에는 허용한다.
     */
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

    private Booking(Guest guest, Long userId, String ownerPhoneHmac, Slot slot, int participantCount,
                    long depositAmount, long balanceAmount,
                    DepositPaymentMethod paymentMethod, PassPurchase passPurchase, String accessToken,
                    BookingSource source) {
        requireExactlyOneOwner(guest, userId);
        requireOwnerPhoneHmac(ownerPhoneHmac);
        SlotCapacity.requireValidParticipantCount(participantCount);
        if (passPurchase != null && participantCount != 1) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "8회권 예약은 1명만 예약할 수 있습니다.");
        }
        this.guest = guest;
        this.userId = userId;
        this.ownerPhoneHmac = ownerPhoneHmac;
        this.bookingClass = slot.getBookingClass();
        this.slot = slot;
        this.status = BookingStatus.BOOKED;
        this.source = source;
        this.participantCount = participantCount;
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

    private static void requireOwnerPhoneHmac(String ownerPhoneHmac) {
        if (ownerPhoneHmac == null || ownerPhoneHmac.isBlank()) {
            throw new IllegalArgumentException("예약자 휴대폰 식별자는 필수입니다.");
        }
    }

    /** 게스트 예약금 예약 생성. */
    public static Booking forGuestDeposit(Guest guest, Slot slot, long depositAmount, long balanceAmount,
                                          DepositPaymentMethod paymentMethod, String accessToken) {
        return forGuestDeposit(
                guest, slot, 1, depositAmount, balanceAmount, paymentMethod, accessToken);
    }

    public static Booking forGuestDeposit(Guest guest, Slot slot, int participantCount,
                                          long depositAmount, long balanceAmount,
                                          DepositPaymentMethod paymentMethod, String accessToken) {
        return new Booking(
                guest, null, guest.getPhoneHmac(), slot, participantCount,
                depositAmount, balanceAmount, paymentMethod, null, accessToken, BookingSource.WEB);
    }

    /** 회원 예약금 예약 생성. */
    public static Booking forMemberDeposit(User member,
                                           Slot slot, long depositAmount, long balanceAmount,
                                           DepositPaymentMethod paymentMethod) {
        return forMemberDeposit(
                member, slot, 1, depositAmount, balanceAmount, paymentMethod);
    }

    public static Booking forMemberDeposit(User member, Slot slot, int participantCount,
                                           long depositAmount, long balanceAmount,
                                           DepositPaymentMethod paymentMethod) {
        return new Booking(
                null, member.getId(), member.getPhoneHmac(), slot, participantCount,
                depositAmount, balanceAmount, paymentMethod, null, null, BookingSource.WEB);
    }

    /** 회원 8회권 예약 생성. depositAmount/balanceAmount=0, paymentMethod=null. */
    public static Booking forMemberPass(
            User member, Slot slot, PassPurchase passPurchase) {
        return forMemberPass(member, slot, passPurchase, 1);
    }

    public static Booking forMemberPass(
            User member, Slot slot, PassPurchase passPurchase, int participantCount) {
        passPurchase.requireApplicableToClass(
                slot.getBookingClass().getCategory(), slot.getBookingClass().isPassEligible());
        return new Booking(
                null, member.getId(), member.getPhoneHmac(), slot, participantCount,
                0, 0, null, passPurchase, null, BookingSource.WEB);
    }

    /** 전화·메신저·방문으로 접수한 비회원 예약을 운영자가 등록한다. */
    public static Booking forAdminGuest(
            Guest guest,
            Slot slot,
            int participantCount,
            long depositAmount,
            long balanceAmount,
            BookingSource source,
            LocalDateTime depositPaidAt
    ) {
        source.requireOperatorManaged();
        Booking booking = new Booking(
                guest,
                null,
                guest.getPhoneHmac(),
                slot,
                participantCount,
                depositAmount,
                balanceAmount,
                depositPaidAt == null ? null : DepositPaymentMethod.BANK_TRANSFER,
                null,
                null,
                source);
        booking.depositPaidAt = depositPaidAt;
        return booking;
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
        if (!isCustomerCancellationAllowedAfterBooked()) {
            throw new HappyGalleryException(
                    ErrorCode.CHANGE_NOT_ALLOWED,
                    "잔금 결제가 완료된 예약은 고객이 취소할 수 없습니다. 관리자 정산이 필요합니다.");
        }
        this.status = BookingStatus.CANCELED;
        this.ownerPhoneHmac = null;
    }

    /** 공방 사정으로 예약을 취소한다. 고객 취소 제한과 관계없이 관리자 정산 흐름에서만 호출한다. */
    public void cancelByAdmin() {
        status.requireBooked();
        this.status = BookingStatus.CANCELED;
        this.ownerPhoneHmac = null;
    }

    /** 예약을 유지하면서 취소한 인원만큼 예약금·잔금과 슬롯 점유를 줄인다. */
    public ParticipantReduction reduceParticipants(int newParticipantCount) {
        status.requireBooked();
        if (!isCustomerCancellationAllowedAfterBooked()) {
            throw new HappyGalleryException(
                    ErrorCode.CHANGE_NOT_ALLOWED,
                    "잔금 결제가 완료된 예약은 인원을 줄일 수 없습니다. 관리자 정산이 필요합니다.");
        }
        if (isPassBooking()) {
            throw new HappyGalleryException(
                    ErrorCode.CHANGE_NOT_ALLOWED, "8회권 예약은 인원을 줄일 수 없습니다.");
        }
        if (newParticipantCount < 1 || newParticipantCount >= participantCount) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "변경 인원은 1명 이상이고 현재 예약 인원보다 적어야 합니다.");
        }

        int previousParticipantCount = participantCount;
        long previousDepositAmount = depositAmount;
        long previousBalanceAmount = balanceAmount;
        depositAmount = prorated(previousDepositAmount, newParticipantCount, previousParticipantCount);
        balanceAmount = prorated(previousBalanceAmount, newParticipantCount, previousParticipantCount);
        participantCount = newParticipantCount;
        return new ParticipantReduction(
                previousParticipantCount - newParticipantCount,
                previousDepositAmount - depositAmount,
                previousBalanceAmount - balanceAmount);
    }

    private static long prorated(long amount, int numerator, int denominator) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(numerator))
                .divide(BigInteger.valueOf(denominator))
                .longValueExact();
    }

    public boolean isCustomerCancellationAllowed() {
        return status == BookingStatus.BOOKED && isCustomerCancellationAllowedAfterBooked();
    }

    public record ParticipantReduction(
            int canceledParticipantCount,
            long refundAmount,
            long reducedBalanceAmount
    ) {}

    private boolean isCustomerCancellationAllowedAfterBooked() {
        return balanceAmount == 0 || balanceStatus != BalanceStatus.PAID;
    }

    /** 수업 종료 후 결석 처리. 크레딧은 예약 시 이미 소모되었으므로 상태만 변경. */
    public void markNoShow(LocalDateTime now) {
        status.requireBooked();
        if (now.isBefore(slot.getEndAt())) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "수업 종료 전에는 노쇼 처리할 수 없습니다.");
        }
        this.status = BookingStatus.NO_SHOW;
        this.ownerPhoneHmac = null;
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
        this.ownerPhoneHmac = null;
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

    /** PG 결제키 없이 현장에서 받은 예약금은 운영자가 직접 반환해야 한다. */
    public boolean requiresManualDepositCompensation() {
        return source != BookingSource.WEB
                && depositAmount > 0
                && depositPaidAt != null
                && paymentKey == null;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Guest getGuest() { return guest; }
    public String getOwnerPhoneHmac() { return ownerPhoneHmac; }
    public BookingClass getBookingClass() { return bookingClass; }
    public Slot getSlot() { return slot; }
    public BookingStatus getStatus() { return status; }
    public BookingSource getSource() { return source; }
    public long getDepositAmount() { return depositAmount; }
    public LocalDateTime getDepositPaidAt() { return depositPaidAt; }
    public long getBalanceAmount() { return balanceAmount; }
    public int getParticipantCount() { return participantCount; }
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

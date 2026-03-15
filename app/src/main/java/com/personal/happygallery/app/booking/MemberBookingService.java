package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.DuplicateBookingException;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.PaymentMethodNotAllowedException;
import com.personal.happygallery.common.error.SlotNotAvailableException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.pass.PassLedger;
import com.personal.happygallery.domain.pass.PassLedgerType;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 예약 생성 서비스.
 * GuestBookingService와 동일한 슬롯/8회권 로직을 따르되, 휴대폰 인증 대신 세션 userId를 사용한다.
 */
@Service
@Transactional
public class MemberBookingService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final SlotManagementService slotManagementService;
    private final PassPurchaseRepository passPurchaseRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    public MemberBookingService(SlotRepository slotRepository,
                                BookingRepository bookingRepository,
                                SlotManagementService slotManagementService,
                                PassPurchaseRepository passPurchaseRepository,
                                PassLedgerRepository passLedgerRepository,
                                BookingSupport bookingSupport,
                                Clock clock) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.slotManagementService = slotManagementService;
        this.passPurchaseRepository = passPurchaseRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    /**
     * 회원 예약을 생성한다. {@code passId}가 있으면 8회권 결제, 없으면 예약금 결제.
     *
     * @param userId        인증된 회원 ID
     * @param slotId        예약 슬롯 ID
     * @param depositAmount 예약금 (passId가 null일 때)
     * @param paymentMethod 결제 수단 (passId가 null일 때)
     * @param passId        8회권 ID (null이면 예약금 결제)
     */
    public Booking createMemberBooking(Long userId, Long slotId, long depositAmount,
                                        DepositPaymentMethod paymentMethod, Long passId) {

        // 1. 슬롯 활성 여부 확인
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));
        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }

        // 2. 중복 예약 확인
        if (bookingRepository.existsBySlotIdAndUserId(slotId, userId)) {
            throw new DuplicateBookingException();
        }

        // 3. confirmBooking — 비관적 락 + 정원 증가 + 버퍼 비활성화
        slotManagementService.confirmBooking(slotId);

        Booking booking;

        if (passId != null) {
            // 4a. 8회권 결제 경로
            PassPurchase pass = passPurchaseRepository.findById(passId)
                    .orElseThrow(() -> new NotFoundException("8회권"));

            // 8회권 소유자 확인
            if (!Objects.equals(pass.getUserId(), userId)) {
                throw new NotFoundException("8회권");
            }

            pass.requireUsable(LocalDateTime.now(clock));

            passLedgerRepository.save(new PassLedger(pass, PassLedgerType.USE, 1));
            pass.useCredit();
            passPurchaseRepository.save(pass);

            booking = new Booking(userId, slot, pass);
        } else {
            // 4b. 예약금 결제 경로 — 계좌이체 차단
            if (paymentMethod == DepositPaymentMethod.BANK_TRANSFER) {
                throw new PaymentMethodNotAllowedException();
            }
            long balanceAmount = slot.getBookingClass().getPrice() - depositAmount;
            booking = new Booking(userId, slot, depositAmount, balanceAmount, paymentMethod);
        }

        booking = bookingRepository.save(booking);

        // 5. 초기 이력 저장 (BOOKED)
        bookingSupport.recordHistory(booking, BookingHistoryAction.BOOKED, null, slot, "CUSTOMER", null);

        // 6. 예약 완료 알림
        bookingSupport.notifyBookingUser(booking, NotificationEventType.BOOKING_CONFIRMED);

        return booking;
    }
}

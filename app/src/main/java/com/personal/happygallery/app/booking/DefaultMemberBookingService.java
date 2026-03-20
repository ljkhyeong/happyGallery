package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.in.MemberBookingUseCase;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.common.error.DuplicateBookingException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.pass.PassPurchase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 예약 생성 서비스.
 * DefaultGuestBookingService와 동일한 슬롯/8회권 로직을 따르되, 휴대폰 인증 대신 세션 userId를 사용한다.
 */
@Service
@Transactional
public class DefaultMemberBookingService implements MemberBookingUseCase {

    private final BookingReaderPort bookingReaderPort;
    private final BookingSlotSupport creationSupport;

    public DefaultMemberBookingService(BookingReaderPort bookingReaderPort,
                                BookingSlotSupport creationSupport) {
        this.bookingReaderPort = bookingReaderPort;
        this.creationSupport = creationSupport;
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
        Slot slot = creationSupport.loadActiveSlot(slotId);

        // 2. 중복 예약 확인
        if (bookingReaderPort.existsBySlotIdAndUserId(slotId, userId)) {
            throw new DuplicateBookingException();
        }

        // 3. 비관적 락 + 정원 증가 + 버퍼 비활성화
        creationSupport.lockSlotCapacity(slotId);

        Booking booking;
        if (passId != null) {
            PassPurchase pass = creationSupport.deductPassCredit(passId, userId);
            booking = Booking.forMemberPass(userId, slot, pass);
        } else {
            creationSupport.requireValidDeposit(paymentMethod);
            long balanceAmount = slot.getBookingClass().getPrice() - depositAmount;
            booking = Booking.forMemberDeposit(userId, slot, depositAmount, balanceAmount, paymentMethod);
        }

        return creationSupport.saveAndComplete(booking, slot);
    }
}

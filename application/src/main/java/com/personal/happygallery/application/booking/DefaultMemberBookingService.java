package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.MemberBookingUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.DuplicateBookingException;
import com.personal.happygallery.domain.error.PhoneVerificationRequiredException;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.user.User;
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
    private final SlotCapacitySupport slotCapacitySupport;
    private final BookingCreationSupport creationSupport;
    private final MemberAccountGuard memberAccountGuard;

    public DefaultMemberBookingService(BookingReaderPort bookingReaderPort,
                                       SlotCapacitySupport slotCapacitySupport,
                                       BookingCreationSupport creationSupport,
                                       MemberAccountGuard memberAccountGuard) {
        this.bookingReaderPort = bookingReaderPort;
        this.slotCapacitySupport = slotCapacitySupport;
        this.creationSupport = creationSupport;
        this.memberAccountGuard = memberAccountGuard;
    }

    /** 결제 prepare 단계에서 확정한 예약금과 잔금으로 회원 예약을 생성한다. */
    @Override
    public Booking createMemberDepositBooking(Long userId, Long slotId,
                                               DepositPaymentMethod paymentMethod,
                                               long depositAmount, long balanceAmount) {
        return createMemberDepositBooking(
                userId, slotId, paymentMethod, depositAmount, balanceAmount, 1);
    }

    @Override
    public Booking createMemberDepositBooking(Long userId, Long slotId,
                                               DepositPaymentMethod paymentMethod,
                                               long depositAmount, long balanceAmount,
                                               int participantCount) {
        User user = requireBookableMember(userId);
        Slot slot = reserveSlot(user.getPhoneHmac(), slotId, participantCount);
        creationSupport.requireValidDeposit(paymentMethod);
        Booking booking = Booking.forMemberDeposit(
                user, slot, participantCount, depositAmount, balanceAmount, paymentMethod);
        return creationSupport.saveAndComplete(booking, slot);
    }

    /** 회원이 소유한 8회권 크레딧으로 예약을 생성한다. */
    @Override
    public Booking createMemberPassBooking(Long userId, Long slotId, Long passId) {
        return createMemberPassBooking(userId, slotId, passId, 1);
    }

    @Override
    public Booking createMemberPassBooking(
            Long userId, Long slotId, Long passId, int participantCount) {
        User user = requireBookableMember(userId);
        PassPurchase pass = creationSupport.requireOwnedPassForUpdate(passId, userId);
        Slot slot = reserveSlot(user.getPhoneHmac(), slotId, participantCount);
        Booking booking = creationSupport.save(
                Booking.forMemberPass(user, slot, pass, participantCount));
        creationSupport.deductPassCredit(pass, booking.getId());
        return creationSupport.complete(booking, slot);
    }

    private User requireBookableMember(Long userId) {
        User user = memberAccountGuard.requireActiveForUpdate(userId);
        if (user.getPhoneHmac() == null) {
            throw new PhoneVerificationRequiredException();
        }
        return user;
    }

    private Slot reserveSlot(String ownerPhoneHmac, Long slotId, int participantCount) {
        slotCapacitySupport.requireAvailableSlot(slotId);
        if (bookingReaderPort.existsBookedBySlotIdAndOwnerPhoneHmac(
                slotId, ownerPhoneHmac)) {
            throw new DuplicateBookingException();
        }
        return slotCapacitySupport.reserveCapacity(slotId, participantCount);
    }
}

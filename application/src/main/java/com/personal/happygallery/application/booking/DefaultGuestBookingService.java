package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.customer.VerifiedGuestResolver;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.payment.PaymentContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultGuestBookingService implements GuestBookingUseCase {

    private final VerifiedGuestResolver verifiedGuestResolver;
    private final BookingCreationSupport creationSupport;
    private final GuestTokenService guestTokenService;

    public DefaultGuestBookingService(VerifiedGuestResolver verifiedGuestResolver,
                                      BookingCreationSupport creationSupport,
                                      GuestTokenService guestTokenService) {
        this.verifiedGuestResolver = verifiedGuestResolver;
        this.creationSupport = creationSupport;
        this.guestTokenService = guestTokenService;
    }

    /** 결제 확정 후 게스트 예약을 생성한다. 비회원은 예약금 결제만 허용한다. */
    @Override
    public GuestBookingResult createPaymentGuestBooking(CreatePaymentGuestBookingCommand command) {
        Guest guest = verifiedGuestResolver.resolveWithPaymentProof(
                PaymentContext.BOOKING,
                command.paymentOrderId(), command.phone(), command.verificationProof(), command.name());
        return createBooking(
                guest, command.slotId(), command.paymentMethod(),
                command.depositAmount(), command.balanceAmount(), command.participantCount());
    }

    private GuestBookingResult createBooking(
            Guest guest,
            Long slotId,
            DepositPaymentMethod paymentMethod,
            long depositAmount,
            long balanceAmount,
            int participantCount) {
        Slot slot = creationSupport.reserveSlot(
                guest.getPhoneHmac(), slotId, participantCount);

        GuestTokenService.IssuedToken issued = guestTokenService.issue();
        String rawToken = issued.rawToken();
        String accessToken = issued.tokenHash();

        creationSupport.requireValidDeposit(paymentMethod);
        Booking booking = Booking.forGuestDeposit(
                guest,
                slot,
                participantCount,
                depositAmount,
                balanceAmount,
                paymentMethod,
                accessToken);

        booking = creationSupport.saveAndComplete(booking, slot);
        return new GuestBookingResult(booking, rawToken);
    }
}

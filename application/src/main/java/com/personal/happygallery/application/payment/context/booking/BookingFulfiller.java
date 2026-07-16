package com.personal.happygallery.application.payment.context.booking;

import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase.CreateGuestBookingCommand;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase.GuestBookingResult;
import com.personal.happygallery.application.booking.port.in.MemberBookingUseCase;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingFulfiller implements PaymentFulfiller {

    private final GuestBookingUseCase guestBookingUseCase;
    private final MemberBookingUseCase memberBookingUseCase;

    public BookingFulfiller(GuestBookingUseCase guestBookingUseCase,
                            MemberBookingUseCase memberBookingUseCase) {
        this.guestBookingUseCase = guestBookingUseCase;
        this.memberBookingUseCase = memberBookingUseCase;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.BOOKING;
    }

    @Override
    public void validateBeforePg(PaymentAttempt attempt, PaymentPayload payload) {
        if (!(payload instanceof BookingPayload bp)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 결제 payload가 아닙니다.");
        }
        if (bp.passId() != null && bp.userId() == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 사용 예약은 회원 인증이 필요합니다.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentPayload payload, String paymentKey) {
        BookingPayload bp = (BookingPayload) payload;

        if (bp.userId() != null) {
            Booking booking = memberBookingUseCase.createMemberBooking(
                    bp.userId(), bp.slotId(), bp.paymentMethod(), bp.passId());
            booking.recordPaymentKey(paymentKey);
            return new FulfillResult(booking.getId(), null);
        }

        GuestBookingResult result = guestBookingUseCase.createGuestBooking(
                new CreateGuestBookingCommand(bp.phone(), bp.verificationCode(), bp.name(),
                        bp.slotId(), bp.paymentMethod()));
        result.booking().recordPaymentKey(paymentKey);
        return new FulfillResult(result.booking().getId(), result.rawAccessToken());
    }
}

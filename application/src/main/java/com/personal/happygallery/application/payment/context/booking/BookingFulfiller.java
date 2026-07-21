package com.personal.happygallery.application.payment.context.booking;

import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase.CreatePaymentGuestBookingCommand;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase.GuestBookingResult;
import com.personal.happygallery.application.booking.port.in.MemberBookingUseCase;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PreparedBookingPayload;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BookingFulfiller implements PaymentFulfiller {

    private final GuestBookingUseCase guestBookingUseCase;
    private final MemberBookingUseCase memberBookingUseCase;
    private final Clock clock;

    public BookingFulfiller(GuestBookingUseCase guestBookingUseCase,
                            MemberBookingUseCase memberBookingUseCase,
                            Clock clock) {
        this.guestBookingUseCase = guestBookingUseCase;
        this.memberBookingUseCase = memberBookingUseCase;
        this.clock = clock;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.BOOKING;
    }

    @Override
    public void validateStoredPayload(PaymentAttempt attempt, PaymentPayload payload) {
        if (!(payload instanceof PreparedBookingPayload bp)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "예약 금액 정보가 없습니다. 결제를 다시 준비해 주세요.");
        }
        if (bp.passId() != null) {
            if (bp.userId() == null || attempt.getAmount() != 0L
                    || bp.depositAmount() != 0L || bp.balanceAmount() != 0L) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 예약 금액 정보가 올바르지 않습니다.");
            }
            return;
        }
        if (bp.depositAmount() != attempt.getAmount() || bp.depositAmount() < 0L || bp.balanceAmount() < 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "저장된 예약 금액이 결제 금액과 일치하지 않습니다.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentAttempt attempt, PaymentPayload payload) {
        PreparedBookingPayload bp = (PreparedBookingPayload) payload;

        if (bp.userId() != null) {
            Booking booking = bp.passId() != null
                    ? memberBookingUseCase.createMemberPassBooking(bp.userId(), bp.slotId(), bp.passId())
                    : memberBookingUseCase.createMemberDepositBooking(
                            bp.userId(), bp.slotId(), bp.paymentMethod(),
                            bp.depositAmount(), bp.balanceAmount());
            booking.recordPaymentConfirmation(attempt.getConfirmedPaymentKey(), LocalDateTime.now(clock));
            return new FulfillResult(booking.getId(), null);
        }

        GuestBookingResult result = guestBookingUseCase.createPaymentGuestBooking(
                new CreatePaymentGuestBookingCommand(
                        attempt.getOrderIdExternal(), bp.phone(), bp.guestVerificationProof(), bp.name(),
                        bp.slotId(), bp.paymentMethod(), bp.depositAmount(), bp.balanceAmount()));
        result.booking().recordPaymentConfirmation(
                attempt.getConfirmedPaymentKey(), LocalDateTime.now(clock));
        return new FulfillResult(result.booking().getId(), result.rawAccessToken());
    }
}

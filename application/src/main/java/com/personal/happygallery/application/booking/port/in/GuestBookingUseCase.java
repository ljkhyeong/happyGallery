package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;

/**
 * 게스트(비회원) 예약 유스케이스.
 *
 * <p>결제 확정 후 게스트 예약 생성을 담당한다. 휴대폰 인증 코드 발급은
 * customer 패키지의 {@code PhoneVerificationIssueUseCase}가 맡는다.
 */
public interface GuestBookingUseCase {

    record GuestBookingResult(Booking booking, String rawAccessToken) {}

    record CreatePaymentGuestBookingCommand(
            String paymentOrderId,
            String phone,
            String verificationProof,
            String name,
            Long slotId,
            DepositPaymentMethod paymentMethod,
            long depositAmount,
            long balanceAmount,
            int participantCount) {}

    GuestBookingResult createPaymentGuestBooking(CreatePaymentGuestBookingCommand command);
}

package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;

/**
 * 게스트(비회원) 예약 유스케이스.
 *
 * <p>휴대폰 인증 코드 발송 및 게스트 예약 생성을 담당한다.
 */
public interface GuestBookingUseCase {

    PhoneVerification sendVerificationCode(String phone, PhoneVerificationPurpose purpose);

    record GuestBookingResult(Booking booking, String rawAccessToken) {}

    record CreateGuestBookingCommand(String phone, String code, String name,
                                     Long slotId,
                                     DepositPaymentMethod paymentMethod,
                                     long depositAmount,
                                     long balanceAmount) {}

    record CreatePaymentGuestBookingCommand(
            String paymentOrderId,
            String phone,
            String verificationProof,
            String name,
            Long slotId,
            DepositPaymentMethod paymentMethod,
            long depositAmount,
            long balanceAmount,
            int participantCount) {

        public CreatePaymentGuestBookingCommand(
                String paymentOrderId,
                String phone,
                String verificationProof,
                String name,
                Long slotId,
                DepositPaymentMethod paymentMethod,
                long depositAmount,
                long balanceAmount) {
            this(paymentOrderId, phone, verificationProof, name, slotId,
                    paymentMethod, depositAmount, balanceAmount, 1);
        }
    }

    GuestBookingResult createGuestBooking(CreateGuestBookingCommand command);

    GuestBookingResult createPaymentGuestBooking(CreatePaymentGuestBookingCommand command);
}

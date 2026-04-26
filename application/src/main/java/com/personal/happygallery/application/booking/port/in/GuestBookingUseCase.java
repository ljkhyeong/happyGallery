package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.PhoneVerification;

/**
 * 게스트(비회원) 예약 유스케이스.
 *
 * <p>휴대폰 인증 코드 발송 및 게스트 예약 생성을 담당한다.
 */
public interface GuestBookingUseCase {

    PhoneVerification sendVerificationCode(String phone);

    record GuestBookingResult(Booking booking, String rawAccessToken) {}

    record CreateGuestBookingCommand(String phone, String code, String name,
                                     Long slotId,
                                     DepositPaymentMethod paymentMethod) {}

    GuestBookingResult createGuestBooking(CreateGuestBookingCommand command);
}

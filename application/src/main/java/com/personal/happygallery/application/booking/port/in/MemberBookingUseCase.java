package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;

/**
 * 회원 예약 생성 유스케이스.
 */
public interface MemberBookingUseCase {

    Booking createMemberDepositBooking(Long userId, Long slotId,
                                       DepositPaymentMethod paymentMethod,
                                       long depositAmount, long balanceAmount);

    Booking createMemberPassBooking(Long userId, Long slotId, Long passId);
}

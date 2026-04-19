package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;

/**
 * 예약 변경(reschedule) 유스케이스.
 *
 * <p>비회원(access-token) / 회원(userId) 두 경로를 지원한다.
 */
public interface BookingRescheduleUseCase {

    Booking rescheduleBooking(Long bookingId, String accessToken, Long newSlotId);

    Booking rescheduleMemberBooking(Long bookingId, Long userId, Long newSlotId);
}

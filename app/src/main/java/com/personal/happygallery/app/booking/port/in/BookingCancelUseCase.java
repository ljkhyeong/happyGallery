package com.personal.happygallery.app.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;

/**
 * 예약 취소 유스케이스.
 *
 * <p>비회원(access-token) / 회원(userId) 두 경로를 지원한다.
 */
public interface BookingCancelUseCase {

    CancelResult cancelBooking(Long bookingId, String accessToken);

    CancelResult cancelMemberBooking(Long bookingId, Long userId);

    record CancelResult(Booking booking, boolean refundable) {}
}

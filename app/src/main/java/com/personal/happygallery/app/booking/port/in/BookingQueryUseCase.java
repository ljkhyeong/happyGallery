package com.personal.happygallery.app.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import java.util.List;

/**
 * 예약 조회 유스케이스.
 *
 * <p>비회원(access-token) / 회원(userId) 두 경로를 지원한다.
 */
public interface BookingQueryUseCase {

    Booking getBookingByToken(Long bookingId, String accessToken);

    List<Booking> listMyBookings(Long userId);

    Booking findMyBooking(Long id, Long userId);
}

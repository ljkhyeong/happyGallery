package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import java.util.List;

/**
 * 예약 조회 유스케이스.
 *
 * <p>비회원(access-token) / 회원(userId) 두 경로를 지원한다.
 */
public interface BookingQueryUseCase {

    record BookingDetail(Booking booking, Refund refund) {}

    BookingDetail getBookingByToken(Long bookingId, String accessToken);

    List<Booking> listMyBookings(Long userId);

    CursorPage<Booking> listMyBookings(Long userId, String cursor, int size);

    BookingDetail findMyBooking(Long id, Long userId);
}

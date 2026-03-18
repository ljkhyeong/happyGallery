package com.personal.happygallery.app.booking.port.in;

import com.personal.happygallery.app.web.admin.dto.AdminBookingResponse;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 예약 조회 유스케이스.
 */
public interface AdminBookingQueryUseCase {

    List<AdminBookingResponse> listBookings(LocalDate date, BookingStatus status);
}

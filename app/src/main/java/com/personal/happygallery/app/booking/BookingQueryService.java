package com.personal.happygallery.app.booking;

import com.personal.happygallery.domain.booking.Booking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingSupport bookingSupport;

    public BookingQueryService(BookingSupport bookingSupport) {
        this.bookingSupport = bookingSupport;
    }

    /**
     * access_token으로 비회원 예약을 조회한다.
     * bookingId + accessToken 두 조건이 모두 일치해야 한다.
     */
    public Booking getBookingByToken(Long bookingId, String accessToken) {
        return bookingSupport.findByToken(bookingId, accessToken);
    }
}

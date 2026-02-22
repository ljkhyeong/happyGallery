package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.infra.booking.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingRepository bookingRepository;

    public BookingQueryService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * access_token으로 비회원 예약을 조회한다.
     * bookingId + accessToken 두 조건이 모두 일치해야 한다.
     */
    public Booking getBookingByToken(Long bookingId, String accessToken) {
        return bookingRepository.findByIdAndAccessToken(bookingId, accessToken)
                .orElseThrow(() -> new NotFoundException("예약"));
    }
}

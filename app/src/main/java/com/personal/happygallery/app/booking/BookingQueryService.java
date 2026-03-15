package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.infra.booking.BookingRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingSupport bookingSupport;
    private final BookingRepository bookingRepository;

    public BookingQueryService(BookingSupport bookingSupport,
                               BookingRepository bookingRepository) {
        this.bookingSupport = bookingSupport;
        this.bookingRepository = bookingRepository;
    }

    /**
     * access_token으로 비회원 예약을 조회한다.
     * bookingId + accessToken 두 조건이 모두 일치해야 한다.
     */
    public Booking getBookingByToken(Long bookingId, String accessToken) {
        return bookingSupport.findByToken(bookingId, accessToken);
    }

    /** 회원 — 자기 예약 목록 조회 */
    public List<Booking> listMyBookings(Long userId) {
        return bookingRepository.findByUserIdWithDetails(userId);
    }

    /** 회원 — 자기 예약 상세 조회 */
    public Booking findMyBooking(Long id, Long userId) {
        return bookingRepository.findByIdAndUserIdWithDetails(id, userId)
                .orElseThrow(() -> new NotFoundException("예약"));
    }
}

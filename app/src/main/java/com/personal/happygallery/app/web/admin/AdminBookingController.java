package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.pass.PassNoShowService;
import com.personal.happygallery.app.web.admin.dto.AdminBookingResponse;
import com.personal.happygallery.app.web.admin.dto.BookingNoShowResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.infra.booking.BookingRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/bookings", "/admin/bookings"})
public class AdminBookingController {

    private final PassNoShowService passNoShowService;
    private final BookingRepository bookingRepository;

    public AdminBookingController(PassNoShowService passNoShowService,
                                  BookingRepository bookingRepository) {
        this.passNoShowService = passNoShowService;
        this.bookingRepository = bookingRepository;
    }

    /** GET /admin/bookings?date=2026-03-08&status=BOOKED — 날짜별 예약 조회 (상태 필터 선택) */
    @GetMapping
    public List<AdminBookingResponse> listBookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) BookingStatus status) {

        return bookingRepository.findAllInRange(date.atStartOfDay(), date.atTime(LocalTime.MAX))
                .stream()
                .filter(b -> status == null || b.getStatus() == status)
                .map(AdminBookingResponse::from)
                .toList();
    }

    /** 결석 처리 — 8회권 크레딧 소멸 유지, 상태 NO_SHOW 전이 */
    @PostMapping("/{bookingId}/no-show")
    public BookingNoShowResponse markNoShow(@PathVariable Long bookingId) {
        Booking booking = passNoShowService.markNoShow(bookingId);
        return BookingNoShowResponse.from(booking);
    }
}

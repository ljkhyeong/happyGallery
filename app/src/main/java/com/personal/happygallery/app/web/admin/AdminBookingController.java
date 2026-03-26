package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.app.pass.port.in.PassNoShowUseCase;
import com.personal.happygallery.app.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.app.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.app.web.OffsetPage;
import com.personal.happygallery.app.web.admin.dto.AdminBookingResponse;
import com.personal.happygallery.app.web.admin.dto.BookingNoShowResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;
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

    private final AdminBookingQueryUseCase adminBookingQueryUseCase;
    private final AdminBookingSearchUseCase adminBookingSearchUseCase;
    private final PassNoShowUseCase passNoShowUseCase;

    public AdminBookingController(AdminBookingQueryUseCase adminBookingQueryUseCase,
                                  AdminBookingSearchUseCase adminBookingSearchUseCase,
                                  PassNoShowUseCase passNoShowUseCase) {
        this.adminBookingQueryUseCase = adminBookingQueryUseCase;
        this.adminBookingSearchUseCase = adminBookingSearchUseCase;
        this.passNoShowUseCase = passNoShowUseCase;
    }

    /** GET /admin/bookings?date=2026-03-08&status=BOOKED — 날짜별 예약 조회 (상태 필터 선택) */
    @GetMapping
    public List<AdminBookingResponse> listBookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) BookingStatus status) {
        return adminBookingQueryUseCase.listBookings(date, status);
    }

    /** GET /admin/bookings/search — 상태·날짜·키워드 기반 예약 검색 (OFFSET + 지연 조인) */
    @GetMapping("/search")
    public OffsetPage<AdminBookingSearchRow> searchBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminBookingSearchUseCase.search(status, dateFrom, dateTo, keyword, page, size);
    }

    /** 결석 처리 — 8회권 크레딧 소멸 유지, 상태 NO_SHOW 전이 */
    @PostMapping("/{bookingId}/no-show")
    public BookingNoShowResponse markNoShow(@PathVariable Long bookingId) {
        Booking booking = passNoShowUseCase.markNoShow(bookingId);
        return BookingNoShowResponse.from(booking);
    }
}

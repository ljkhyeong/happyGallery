package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.pass.PassNoShowService;
import com.personal.happygallery.domain.booking.Booking;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final PassNoShowService passNoShowService;

    public AdminBookingController(PassNoShowService passNoShowService) {
        this.passNoShowService = passNoShowService;
    }

    /** 결석 처리 — 8회권 크레딧 소멸 유지, 상태 NO_SHOW 전이 */
    @PostMapping("/{bookingId}/no-show")
    public Map<String, Object> markNoShow(@PathVariable Long bookingId) {
        Booking booking = passNoShowService.markNoShow(bookingId);
        return Map.of(
                "bookingId", booking.getId(),
                "status", booking.getStatus().name()
        );
    }
}

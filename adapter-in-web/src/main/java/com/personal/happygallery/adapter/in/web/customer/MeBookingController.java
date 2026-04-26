package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.adapter.in.web.booking.dto.CancelResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.MemberRescheduleRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.MyBookingDetail;
import com.personal.happygallery.adapter.in.web.customer.dto.MyBookingSummary;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import com.personal.happygallery.domain.booking.Booking;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 예약 조회/변경/취소 API.
 *
 * <p>예약 생성은 {@code POST /api/v1/payments/prepare} → {@code /confirm} 경로로 일원화됨.
 */
@RestController
@RequestMapping("/api/v1/me/bookings")
public class MeBookingController {

    private final BookingQueryUseCase bookingQueryUseCase;
    private final BookingRescheduleUseCase bookingRescheduleUseCase;
    private final BookingCancelUseCase bookingCancelUseCase;

    public MeBookingController(BookingQueryUseCase bookingQueryUseCase,
                                BookingRescheduleUseCase bookingRescheduleUseCase,
                                BookingCancelUseCase bookingCancelUseCase) {
        this.bookingQueryUseCase = bookingQueryUseCase;
        this.bookingRescheduleUseCase = bookingRescheduleUseCase;
        this.bookingCancelUseCase = bookingCancelUseCase;
    }

    @GetMapping
    public List<MyBookingSummary> myBookings(@CustomerUserId Long userId) {
        return bookingQueryUseCase.listMyBookings(userId).stream()
                .map(MyBookingSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MyBookingDetail myBooking(@PathVariable Long id, @CustomerUserId Long userId) {
        Booking booking = bookingQueryUseCase.findMyBooking(id, userId);
        return MyBookingDetail.from(booking);
    }

    @PatchMapping("/{id}/reschedule")
    public MyBookingSummary rescheduleBooking(@PathVariable Long id,
                                              @RequestBody @Valid MemberRescheduleRequest req,
                                              @CustomerUserId Long userId) {
        Booking booking = bookingRescheduleUseCase.rescheduleMemberBooking(id, userId, req.newSlotId());
        return MyBookingSummary.from(booking);
    }

    @DeleteMapping("/{id}")
    public CancelResponse cancelBooking(@PathVariable Long id, @CustomerUserId Long userId) {
        BookingCancelUseCase.CancelResult result = bookingCancelUseCase.cancelMemberBooking(id, userId);
        return CancelResponse.from(result.booking(), result.refundable());
    }
}

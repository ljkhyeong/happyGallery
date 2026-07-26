package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.adapter.in.web.booking.dto.CancelResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.MemberRescheduleRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.MyBookingDetail;
import com.personal.happygallery.adapter.in.web.customer.dto.MyBookingSummary;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.domain.booking.Booking;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final Clock clock;

    public MeBookingController(BookingQueryUseCase bookingQueryUseCase,
                                BookingRescheduleUseCase bookingRescheduleUseCase,
                                BookingCancelUseCase bookingCancelUseCase,
                                Clock clock) {
        this.bookingQueryUseCase = bookingQueryUseCase;
        this.bookingRescheduleUseCase = bookingRescheduleUseCase;
        this.bookingCancelUseCase = bookingCancelUseCase;
        this.clock = clock;
    }

    @GetMapping
    @Operation(operationId = "listMyBookings")
    public List<MyBookingSummary> myBookings(@AuthenticationPrincipal CustomerPrincipal customer) {
        return MyBookingSummary.fromAll(bookingQueryUseCase.listMyBookings(customer.userId()));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getMyBooking")
    public MyBookingDetail myBooking(@PathVariable Long id,
                                     @AuthenticationPrincipal CustomerPrincipal customer) {
        BookingQueryUseCase.BookingDetail detail = bookingQueryUseCase.findMyBooking(id, customer.userId());
        return MyBookingDetail.from(detail.booking(), detail.refund(), clock);
    }

    @PatchMapping("/{id}/reschedule")
    @Operation(operationId = "rescheduleMyBooking")
    public MyBookingSummary rescheduleBooking(@PathVariable Long id,
                                              @RequestBody @Valid MemberRescheduleRequest req,
                                              @AuthenticationPrincipal CustomerPrincipal customer) {
        Booking booking = bookingRescheduleUseCase.rescheduleMemberBooking(
                id, customer.userId(), req.newSlotId());
        return MyBookingSummary.from(booking);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "cancelMyBooking")
    public CancelResponse cancelBooking(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomerPrincipal customer) {
        BookingCancelUseCase.CancelResult result = bookingCancelUseCase.cancelMemberBooking(
                id, customer.userId());
        return CancelResponse.from(result);
    }
}

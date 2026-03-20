package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.app.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.app.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.app.booking.port.in.MemberBookingUseCase;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.booking.dto.CancelResponse;
import com.personal.happygallery.app.web.customer.dto.CreateMemberBookingRequest;
import com.personal.happygallery.app.web.customer.dto.MemberRescheduleRequest;
import com.personal.happygallery.app.web.customer.dto.MyBookingDetail;
import com.personal.happygallery.app.web.customer.dto.MyBookingSummary;
import com.personal.happygallery.domain.booking.Booking;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/bookings")
public class MeBookingController {

    private final BookingQueryUseCase bookingQueryUseCase;
    private final MemberBookingUseCase memberBookingUseCase;
    private final BookingRescheduleUseCase bookingRescheduleUseCase;
    private final BookingCancelUseCase bookingCancelUseCase;

    public MeBookingController(BookingQueryUseCase bookingQueryUseCase,
                                MemberBookingUseCase memberBookingUseCase,
                                BookingRescheduleUseCase bookingRescheduleUseCase,
                                BookingCancelUseCase bookingCancelUseCase) {
        this.bookingQueryUseCase = bookingQueryUseCase;
        this.memberBookingUseCase = memberBookingUseCase;
        this.bookingRescheduleUseCase = bookingRescheduleUseCase;
        this.bookingCancelUseCase = bookingCancelUseCase;
    }

    @GetMapping
    public List<MyBookingSummary> myBookings(HttpServletRequest request) {
        Long userId = getUserId(request);
        return bookingQueryUseCase.listMyBookings(userId).stream()
                .map(MyBookingSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MyBookingDetail myBooking(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        Booking booking = bookingQueryUseCase.findMyBooking(id, userId);
        return MyBookingDetail.from(booking);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyBookingSummary createBooking(@RequestBody @Valid CreateMemberBookingRequest req,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        Booking booking = memberBookingUseCase.createMemberBooking(
                userId, req.slotId(),
                req.depositAmount(),
                req.paymentMethod(), req.passId());
        return MyBookingSummary.from(booking);
    }

    @PatchMapping("/{id}/reschedule")
    public MyBookingSummary rescheduleBooking(@PathVariable Long id,
                                              @RequestBody @Valid MemberRescheduleRequest req,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        Booking booking = bookingRescheduleUseCase.rescheduleMemberBooking(id, userId, req.newSlotId());
        return MyBookingSummary.from(booking);
    }

    @DeleteMapping("/{id}")
    public CancelResponse cancelBooking(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        BookingCancelUseCase.CancelResult result = bookingCancelUseCase.cancelMemberBooking(id, userId);
        return CancelResponse.of(result.booking(), result.refundable());
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}

package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.booking.BookingCancelService;
import com.personal.happygallery.app.booking.BookingQueryService;
import com.personal.happygallery.app.booking.BookingRescheduleService;
import com.personal.happygallery.app.booking.MemberBookingService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.booking.dto.CancelResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
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

    private final BookingQueryService bookingQueryService;
    private final MemberBookingService memberBookingService;
    private final BookingRescheduleService bookingRescheduleService;
    private final BookingCancelService bookingCancelService;

    public MeBookingController(BookingQueryService bookingQueryService,
                                MemberBookingService memberBookingService,
                                BookingRescheduleService bookingRescheduleService,
                                BookingCancelService bookingCancelService) {
        this.bookingQueryService = bookingQueryService;
        this.memberBookingService = memberBookingService;
        this.bookingRescheduleService = bookingRescheduleService;
        this.bookingCancelService = bookingCancelService;
    }

    @GetMapping
    public List<MyBookingSummary> myBookings(HttpServletRequest request) {
        Long userId = getUserId(request);
        return bookingQueryService.listMyBookings(userId).stream()
                .map(MyBookingSummary::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MyBookingDetail myBooking(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        Booking booking = bookingQueryService.findMyBooking(id, userId);
        return MyBookingDetail.from(booking);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyBookingSummary createBooking(@RequestBody @Valid CreateMemberBookingRequest req,
                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        Booking booking = memberBookingService.createMemberBooking(
                userId, req.slotId(),
                req.depositAmount() != null ? req.depositAmount() : 0L,
                req.paymentMethod(), req.passId());
        return MyBookingSummary.from(booking);
    }

    @PatchMapping("/{id}/reschedule")
    public MyBookingSummary rescheduleBooking(@PathVariable Long id,
                                              @RequestBody @Valid MemberRescheduleRequest req,
                                              HttpServletRequest request) {
        Long userId = getUserId(request);
        Booking booking = bookingRescheduleService.rescheduleMemberBooking(id, userId, req.newSlotId());
        return MyBookingSummary.from(booking);
    }

    @DeleteMapping("/{id}")
    public CancelResponse cancelBooking(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        BookingCancelService.CancelResult result = bookingCancelService.cancelMemberBooking(id, userId);
        return CancelResponse.of(result.booking(), result.refundable());
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }

    // ── DTO ──

    public record MyBookingSummary(Long bookingId, String status, String className,
                                    LocalDateTime startAt, LocalDateTime endAt,
                                    long depositAmount) {
        static MyBookingSummary from(Booking b) {
            return new MyBookingSummary(b.getId(), b.getStatus().name(),
                    b.getBookingClass().getName(),
                    b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                    b.getDepositAmount());
        }
    }

    public record MyBookingDetail(Long bookingId, Long slotId, String status, String className,
                                   LocalDateTime startAt, LocalDateTime endAt,
                                   long depositAmount, long balanceAmount,
                                   String balanceStatus, boolean passBooking) {
        static MyBookingDetail from(Booking b) {
            return new MyBookingDetail(b.getId(), b.getSlot().getId(), b.getStatus().name(),
                    b.getBookingClass().getName(),
                    b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                    b.getDepositAmount(), b.getBalanceAmount(),
                    b.getBalanceStatus().name(), b.isPassBooking());
        }
    }

    public record CreateMemberBookingRequest(
            @NotNull Long slotId,
            @Positive(message = "예약금은 0보다 커야 합니다.") Long depositAmount,
            DepositPaymentMethod paymentMethod,
            Long passId) {}

    public record MemberRescheduleRequest(
            @NotNull Long newSlotId) {}
}

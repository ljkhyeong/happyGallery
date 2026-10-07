package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingSource;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        Long bookingId,
        String bookingNumber,
        CustomerSummary customerSummary,
        String className,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BookingStatus status,
        BookingSource source,
        int participantCount,
        long depositAmount,
        LocalDateTime depositPaidAt,
        long balanceAmount,
        BalanceStatus balanceStatus,
        LocalDateTime balancePaidAt,
        boolean arrears,
        boolean passBooking
) {

    public static AdminBookingResponse fromMember(Booking booking, User user) {
        return from(booking, new CustomerSummary("MEMBER", user.getName(), user.getPhone()));
    }

    public static AdminBookingResponse fromGuest(
            Booking booking,
            String guestName,
            String guestPhone
    ) {
        return from(booking, new CustomerSummary("GUEST", guestName, guestPhone));
    }

    private static AdminBookingResponse from(
            Booking booking,
            CustomerSummary customerSummary
    ) {
        return new AdminBookingResponse(
                booking.getId(),
                booking.bookingNumber(),
                customerSummary,
                booking.getBookingClass().getName(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getStatus(),
                booking.getSource(),
                booking.getParticipantCount(),
                booking.getDepositAmount(),
                booking.getDepositPaidAt(),
                booking.getBalanceAmount(),
                booking.getBalanceStatus(),
                booking.getBalancePaidAt(),
                booking.isArrearsFlag(),
                booking.isPassBooking()
        );
    }

    public record CustomerSummary(
            String type,
            String name,
            String phone
    ) {}
}

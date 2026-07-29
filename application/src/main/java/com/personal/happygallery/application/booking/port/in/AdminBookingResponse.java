package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        Long bookingId,
        String bookingNumber,
        CustomerSummary customerSummary,
        String className,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        String source,
        int participantCount,
        long depositAmount,
        LocalDateTime depositPaidAt,
        long balanceAmount,
        String balanceStatus,
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
                "BK-%08d".formatted(booking.getId()),
                customerSummary,
                booking.getBookingClass().getName(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getStatus().name(),
                booking.getSource().name(),
                booking.getParticipantCount(),
                booking.getDepositAmount(),
                booking.getDepositPaidAt(),
                booking.getBalanceAmount(),
                booking.getBalanceStatus().name(),
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

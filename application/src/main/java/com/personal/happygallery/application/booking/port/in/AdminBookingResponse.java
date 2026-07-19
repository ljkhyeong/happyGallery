package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        Long bookingId,
        String bookingNumber,
        String bookerType,
        String bookerName,
        String bookerPhone,
        String className,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        long depositAmount,
        LocalDateTime depositPaidAt,
        long balanceAmount,
        String balanceStatus,
        LocalDateTime balancePaidAt,
        boolean arrears,
        boolean passBooking
) {

    public static AdminBookingResponse from(Booking booking, User user, String guestName, String guestPhone) {
        boolean isMember = booking.getUserId() != null;
        String name;
        String phone;

        if (isMember) {
            name = user.getName();
            phone = user.getPhone();
        } else {
            name = guestName;
            phone = guestPhone;
        }

        return new AdminBookingResponse(
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                isMember ? "MEMBER" : "GUEST",
                name,
                phone,
                booking.getBookingClass().getName(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getStatus().name(),
                booking.getDepositAmount(),
                booking.getDepositPaidAt(),
                booking.getBalanceAmount(),
                booking.getBalanceStatus().name(),
                booking.getBalancePaidAt(),
                booking.isArrearsFlag(),
                booking.isPassBooking()
        );
    }
}

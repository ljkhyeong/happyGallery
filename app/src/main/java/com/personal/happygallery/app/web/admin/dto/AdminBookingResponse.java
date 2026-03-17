package com.personal.happygallery.app.web.admin.dto;

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
        long balanceAmount,
        boolean passBooking
) {

    public static AdminBookingResponse from(Booking booking, User user) {
        boolean isMember = booking.getUserId() != null;
        String name;
        String phone;

        if (isMember && user != null) {
            name = user.getName();
            phone = user.getPhone();
        } else if (booking.getGuest() != null) {
            name = booking.getGuest().getName();
            phone = booking.getGuest().getPhone();
        } else {
            name = "(알 수 없음)";
            phone = "";
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
                booking.getBalanceAmount(),
                booking.isPassBooking()
        );
    }
}

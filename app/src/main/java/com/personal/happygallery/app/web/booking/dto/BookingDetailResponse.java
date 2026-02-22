package com.personal.happygallery.app.web.booking.dto;

import com.personal.happygallery.domain.booking.Booking;
import java.time.LocalDateTime;

public record BookingDetailResponse(
        Long bookingId,
        String bookingNumber,
        Long slotId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String className,
        String status,
        long depositAmount,
        long balanceAmount,
        String guestName,
        String guestPhone  // 마스킹: 010****5678
) {
    public static BookingDetailResponse from(Booking booking) {
        String rawPhone = booking.getGuest().getPhone();
        String maskedPhone = maskPhone(rawPhone);
        return new BookingDetailResponse(
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                booking.getSlot().getId(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getBookingClass().getName(),
                booking.getStatus().name(),
                booking.getDepositAmount(),
                booking.getBalanceAmount(),
                booking.getGuest().getName(),
                maskedPhone
        );
    }

    /** 010-1234-5678 → 010****5678 (가운데 4자리 마스킹) */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

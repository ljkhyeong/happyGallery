package com.personal.happygallery.adapter.in.web.booking.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskPhoneMiddle;

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
        String maskedPhone = maskPhoneMiddle(rawPhone);
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
}

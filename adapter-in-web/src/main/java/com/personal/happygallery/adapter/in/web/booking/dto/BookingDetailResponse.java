package com.personal.happygallery.adapter.in.web.booking.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskPhoneMiddle;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import java.time.Clock;
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
        String guestPhone,  // 마스킹: 010****5678
        BookingCancelPolicyResponse cancelPolicy,
        RefundProgressResponse refund
) {
    public static BookingDetailResponse from(Booking booking,
                                             Refund refund,
                                             String guestName,
                                             String guestPhone,
                                             Clock clock) {
        String maskedPhone = maskPhoneMiddle(guestPhone);
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
                guestName,
                maskedPhone,
                BookingCancelPolicyResponse.from(booking, clock),
                refund != null ? RefundProgressResponse.from(refund) : null
        );
    }
}

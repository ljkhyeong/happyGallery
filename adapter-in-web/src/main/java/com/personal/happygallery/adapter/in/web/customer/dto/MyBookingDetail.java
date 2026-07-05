package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.booking.dto.BookingCancelPolicyResponse;
import com.personal.happygallery.domain.booking.Booking;
import java.time.Clock;
import java.time.LocalDateTime;

public record MyBookingDetail(Long bookingId, Long slotId, String status, String className,
                               LocalDateTime startAt, LocalDateTime endAt,
                               long depositAmount, long balanceAmount,
                               String balanceStatus, boolean passBooking,
                               BookingCancelPolicyResponse cancelPolicy) {
    public static MyBookingDetail from(Booking b, Clock clock) {
        return new MyBookingDetail(b.getId(), b.getSlot().getId(), b.getStatus().name(),
                b.getBookingClass().getName(),
                b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                b.getDepositAmount(), b.getBalanceAmount(),
                b.getBalanceStatus().name(), b.isPassBooking(),
                BookingCancelPolicyResponse.from(b, clock));
    }
}

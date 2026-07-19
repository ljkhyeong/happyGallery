package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.Booking;
import java.time.LocalDateTime;

public record BookingSettlementResponse(
        Long bookingId,
        String status,
        String balanceStatus,
        LocalDateTime balancePaidAt,
        boolean arrears
) {

    public static BookingSettlementResponse from(Booking booking) {
        return new BookingSettlementResponse(
                booking.getId(),
                booking.getStatus().name(),
                booking.getBalanceStatus().name(),
                booking.getBalancePaidAt(),
                booking.isArrearsFlag());
    }
}

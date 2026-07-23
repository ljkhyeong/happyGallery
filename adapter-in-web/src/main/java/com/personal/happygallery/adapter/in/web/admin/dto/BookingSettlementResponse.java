package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record BookingSettlementResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"BOOKED", "COMPLETED"})
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"UNPAID", "PAID"})
        String balanceStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime balancePaidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean arrears
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

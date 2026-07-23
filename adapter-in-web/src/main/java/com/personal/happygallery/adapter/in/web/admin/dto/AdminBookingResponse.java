package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"GUEST", "MEMBER"})
        String bookerType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookerName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String bookerPhone,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"})
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long depositAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime depositPaidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"UNPAID", "PAID"})
        String balanceStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime balancePaidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean arrears,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passBooking
) {

    public static AdminBookingResponse from(
            com.personal.happygallery.application.booking.port.in.AdminBookingResponse response) {
        return new AdminBookingResponse(
                response.bookingId(),
                response.bookingNumber(),
                response.bookerType(),
                response.bookerName(),
                response.bookerPhone(),
                response.className(),
                response.startAt(),
                response.endAt(),
                response.status(),
                response.depositAmount(),
                response.depositPaidAt(),
                response.balanceAmount(),
                response.balanceStatus(),
                response.balancePaidAt(),
                response.arrears(),
                response.passBooking());
    }
}

package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CustomerSummary customerSummary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"})
        String status,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"WEB", "PHONE", "NAVER_TALK", "KAKAO", "VISIT"})
        String source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        int participantCount,
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
                CustomerSummary.from(response.customerSummary()),
                response.className(),
                response.startAt(),
                response.endAt(),
                response.status(),
                response.source(),
                response.participantCount(),
                response.depositAmount(),
                response.depositPaidAt(),
                response.balanceAmount(),
                response.balanceStatus(),
                response.balancePaidAt(),
                response.arrears(),
                response.passBooking());
    }

    public record CustomerSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"GUEST", "MEMBER"})
            String type,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String phone
    ) {

        private static CustomerSummary from(
                com.personal.happygallery.application.booking.port.in.AdminBookingResponse.CustomerSummary summary) {
            return new CustomerSummary(summary.type(), summary.name(), summary.phone());
        }
    }
}

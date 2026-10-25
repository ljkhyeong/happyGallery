package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.AdminBookingView;
import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.BookingSource;
import com.personal.happygallery.domain.booking.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CustomerSummary customerSummary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingSource source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        int participantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long depositAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime depositPaidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BalanceStatus balanceStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime balancePaidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean arrears,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passBooking
) {

    public static AdminBookingResponse from(
            AdminBookingView view) {
        return new AdminBookingResponse(
                view.bookingId(),
                view.bookingNumber(),
                CustomerSummary.from(view.customerSummary()),
                view.className(),
                view.startAt(),
                view.endAt(),
                view.status(),
                view.source(),
                view.participantCount(),
                view.depositAmount(),
                view.depositPaidAt(),
                view.balanceAmount(),
                view.balanceStatus(),
                view.balancePaidAt(),
                view.arrears(),
                view.passBooking());
    }

    public record CustomerSummary(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"GUEST", "MEMBER"})
            String type,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String phone
    ) {

        private static CustomerSummary from(
                AdminBookingView.CustomerSummary summary) {
            return new CustomerSummary(summary.type(), summary.name(), summary.phone());
        }
    }
}

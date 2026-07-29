package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record AdminBookingSearchItemResponse(
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
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"WEB", "PHONE", "NAVER_TALK", "KAKAO", "VISIT"})
        String source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int participantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long depositAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        LocalDateTime depositPaidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"UNPAID", "PAID"})
        String balanceStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        LocalDateTime balancePaidAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean arrears,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean passBooking,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt
) {

    static AdminBookingSearchItemResponse from(AdminBookingSearchRow row) {
        return new AdminBookingSearchItemResponse(
                row.bookingId(),
                row.bookingNumber(),
                row.bookerType(),
                row.bookerName(),
                row.bookerPhone(),
                row.className(),
                row.startAt(),
                row.endAt(),
                row.status(),
                row.source(),
                row.participantCount(),
                row.depositAmount(),
                row.depositPaidAt(),
                row.balanceAmount(),
                row.balanceStatus(),
                row.balancePaidAt(),
                row.arrears(),
                row.passBooking(),
                row.createdAt());
    }
}

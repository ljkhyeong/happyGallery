package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record RescheduleResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long slotId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "BOOKED")
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        int participantCount
) {
    public static RescheduleResponse from(Booking booking) {
        return new RescheduleResponse(
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                booking.getSlot().getId(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt(),
                booking.getBookingClass().getName(),
                booking.getStatus().name(),
                booking.getParticipantCount()
        );
    }
}

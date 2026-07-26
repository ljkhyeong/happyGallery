package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.booking.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record MyBookingSummary(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"})
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "8")
        int participantCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long depositAmount
) {
    public static MyBookingSummary from(Booking b) {
        return new MyBookingSummary(b.getId(), b.getStatus().name(),
                b.getBookingClass().getName(),
                b.getSlot().getStartAt(), b.getSlot().getEndAt(),
                b.getParticipantCount(),
                b.getDepositAmount());
    }

    public static List<MyBookingSummary> fromAll(List<Booking> bookings) {
        return bookings.stream().map(MyBookingSummary::from).toList();
    }
}

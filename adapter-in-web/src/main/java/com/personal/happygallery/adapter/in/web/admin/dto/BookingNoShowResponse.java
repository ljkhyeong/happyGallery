package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.Booking;
import io.swagger.v3.oas.annotations.media.Schema;

public record BookingNoShowResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "NO_SHOW") String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "8")
        int participantCount
) {

    public static BookingNoShowResponse from(Booking booking) {
        return new BookingNoShowResponse(
                booking.getId(), booking.getStatus().name(), booking.getParticipantCount());
    }
}

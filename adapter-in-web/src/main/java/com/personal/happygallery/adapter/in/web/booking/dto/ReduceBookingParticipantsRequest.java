package com.personal.happygallery.adapter.in.web.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record ReduceBookingParticipantsRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        @Min(1) int participantCount
) {}

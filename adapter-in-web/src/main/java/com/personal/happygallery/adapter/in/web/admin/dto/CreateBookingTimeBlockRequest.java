package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.BookingTimeBlock;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateBookingTimeBlockRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalTime startTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalTime endTime,
        @Schema(nullable = true)
        @Size(max = BookingTimeBlock.MAX_REASON_LENGTH) String reason
) {}

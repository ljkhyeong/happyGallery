package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.DayOverrideMode;
import com.personal.happygallery.domain.booking.BookingDayOverride;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBookingCalendarDayRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull DayOverrideMode mode,
        @Schema(nullable = true)
        @Size(max = BookingDayOverride.MAX_REASON_LENGTH) String reason
) {}

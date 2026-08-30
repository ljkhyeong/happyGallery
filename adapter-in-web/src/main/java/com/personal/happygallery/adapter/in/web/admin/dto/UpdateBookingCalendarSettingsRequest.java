package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalTime;

public record UpdateBookingCalendarSettingsRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @PositiveOrZero Long expectedVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalTime openTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalTime closeTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(BookingCalendarSettings.MIN_SLOT_INTERVAL_MIN)
        @Max(BookingCalendarSettings.MAX_SLOT_INTERVAL_MIN)
        int slotIntervalMin,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean blockPublicHolidays
) {}

package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record BookingCalendarSettingsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalTime openTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalTime closeTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int slotIntervalMin,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean blockPublicHolidays,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version
) {
    public static BookingCalendarSettingsResponse from(BookingCalendarSettings settings) {
        return new BookingCalendarSettingsResponse(
                settings.getOpenTime(),
                settings.getCloseTime(),
                settings.getSlotIntervalMin(),
                settings.isBlockPublicHolidays(),
                settings.getVersion());
    }
}

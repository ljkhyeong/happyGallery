package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.CalendarDay;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.DayOverrideMode;
import com.personal.happygallery.domain.booking.BookingDayAvailability;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record BookingCalendarDayResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean publicHoliday,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingDayAvailability effectiveAvailability,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DayOverrideMode overrideMode,
        @Schema(nullable = true) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BookingTimeBlockResponse> timeBlocks
) {
    public static BookingCalendarDayResponse from(CalendarDay day) {
        return new BookingCalendarDayResponse(
                day.date(),
                day.publicHoliday(),
                day.effectiveAvailability(),
                day.overrideMode(),
                day.reason(),
                day.timeBlocks().stream().map(BookingTimeBlockResponse::from).toList());
    }
}

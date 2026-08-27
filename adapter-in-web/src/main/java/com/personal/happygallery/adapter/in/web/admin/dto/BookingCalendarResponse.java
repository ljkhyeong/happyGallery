package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.CalendarView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BookingCalendarResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingCalendarSettingsResponse settings,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BookingCalendarDayResponse> days
) {
    public static BookingCalendarResponse from(CalendarView view) {
        return new BookingCalendarResponse(
                BookingCalendarSettingsResponse.from(view.settings()),
                view.days().stream().map(BookingCalendarDayResponse::from).toList());
    }
}

package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.BookingTimeBlock;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingTimeBlockResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalTime startTime,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalTime endTime,
        @Schema(nullable = true) String reason
) {
    public static BookingTimeBlockResponse from(BookingTimeBlock block) {
        return new BookingTimeBlockResponse(
                block.getId(), block.getDate(), block.getStartTime(), block.getEndTime(), block.getReason());
    }
}

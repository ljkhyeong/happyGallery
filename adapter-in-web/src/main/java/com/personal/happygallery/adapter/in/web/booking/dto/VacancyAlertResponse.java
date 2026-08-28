package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.VacancyAlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record VacancyAlertResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long alertId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long slotId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) VacancyAlertStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String accessToken
) {
    public static VacancyAlertResponse from(BookingVacancyAlert alert, String accessToken) {
        Slot slot = alert.getSlot();
        return new VacancyAlertResponse(
                alert.getId(),
                slot.getId(),
                slot.getBookingClass().getName(),
                slot.getStartAt(),
                slot.getEndAt(),
                alert.getStatus(),
                accessToken);
    }
}

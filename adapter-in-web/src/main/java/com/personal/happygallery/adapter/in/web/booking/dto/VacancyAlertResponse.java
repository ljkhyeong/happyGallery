package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import com.personal.happygallery.domain.booking.VacancyAlertStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record VacancyAlertResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long alertId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long slotId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) VacancyAlertStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String accessToken
) {
    public static VacancyAlertResponse from(BookingVacancyAlert alert, String accessToken) {
        return new VacancyAlertResponse(
                alert.getId(),
                alert.getSlot().getId(),
                alert.getStatus(),
                accessToken);
    }
}

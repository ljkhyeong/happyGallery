package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminSlotSessionCancelResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int canceledBookings,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int passCreditsRestored,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int depositRefundsRequested,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int balanceSettlementsRequired,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int manualCompensationsRequired
) {
    public static AdminSlotSessionCancelResponse from(CancelSessionResult result) {
        return new AdminSlotSessionCancelResponse(
                result.canceledBookings(),
                result.passCreditsRestored(),
                result.depositRefundsRequested(),
                result.balanceSettlementsRequired(),
                result.manualCompensationsRequired());
    }
}

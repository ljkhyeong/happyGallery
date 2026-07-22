package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionResult;

public record AdminSlotSessionCancelResponse(
        int canceledBookings,
        int passCreditsRestored,
        int depositRefundsRequested,
        int balanceSettlementsRequired,
        int manualCompensationsRequired
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

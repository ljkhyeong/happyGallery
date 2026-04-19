package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;

public record PassRefundResponse(int canceledBookings, int refundCredits, long refundAmount) {

    public static PassRefundResponse from(PassRefundUseCase.PassRefundResult result) {
        return new PassRefundResponse(
                result.canceledBookings(),
                result.refundCredits(),
                result.refundAmount());
    }
}

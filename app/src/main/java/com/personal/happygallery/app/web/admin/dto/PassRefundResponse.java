package com.personal.happygallery.app.web.admin.dto;

import com.personal.happygallery.app.pass.PassRefundService;

public record PassRefundResponse(int canceledBookings, int refundCredits, long refundAmount) {

    public static PassRefundResponse from(PassRefundService.PassRefundResult result) {
        return new PassRefundResponse(
                result.canceledBookings(),
                result.refundCredits(),
                result.refundAmount());
    }
}

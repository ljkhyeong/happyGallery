package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;

public record CancelResponse(
        Long bookingId,
        String status,
        boolean refundable,
        long refundAmount,
        RefundProgressResponse refund
) {
    public static CancelResponse from(BookingCancelUseCase.CancelResult result) {
        return new CancelResponse(
                result.booking().getId(),
                result.booking().getStatus().name(),
                result.refundable(),
                result.refund() != null ? result.refund().getAmount() : 0L,
                result.refund() != null ? RefundProgressResponse.from(result.refund()) : null
        );
    }
}

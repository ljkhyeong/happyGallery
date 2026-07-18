package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;

public record RefundProgressResponse(long amount, RefundStatus status) {

    public static RefundProgressResponse from(Refund refund) {
        return new RefundProgressResponse(refund.getAmount(), refund.getStatus());
    }
}

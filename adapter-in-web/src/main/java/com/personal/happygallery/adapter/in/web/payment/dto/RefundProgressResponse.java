package com.personal.happygallery.adapter.in.web.payment.dto;

import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record RefundProgressResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long pgRefundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long rewardRestoreAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long rewardRevokeAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean restoreCoupon,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundStatus status
) {

    public static RefundProgressResponse from(Refund refund) {
        return new RefundProgressResponse(
                refund.getCustomerRefundAmount(),
                refund.getAmount(),
                refund.getRewardRestoreAmount(),
                refund.getRewardRevokeAmount(),
                refund.isRestoreCoupon(),
                refund.getStatus());
    }
}

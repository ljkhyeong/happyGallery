package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundStatusResponse;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderRejectResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus orderStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundStatusResponse refund
) {

    public static OrderRejectResponse from(OrderApprovalUseCase.RejectResult result) {
        return new OrderRejectResponse(
                result.order().getId(),
                result.order().getStatus(),
                RefundStatusResponse.from(result.refund()));
    }
}

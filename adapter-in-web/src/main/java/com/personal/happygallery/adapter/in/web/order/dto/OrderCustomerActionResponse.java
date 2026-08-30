package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrderCustomerActionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        RefundProgressResponse refund
) {
    public static OrderCustomerActionResponse from(
            OrderCustomerActionUseCase.ActionResult result) {
        return new OrderCustomerActionResponse(
                result.order().getId(),
                result.order().getStatus(),
                result.refund() == null ? null : RefundProgressResponse.from(result.refund()));
    }
}

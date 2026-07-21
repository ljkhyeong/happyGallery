package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundProgressResponse;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import com.personal.happygallery.domain.order.OrderStatus;

public record OrderCustomerActionResponse(
        Long orderId,
        OrderStatus status,
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

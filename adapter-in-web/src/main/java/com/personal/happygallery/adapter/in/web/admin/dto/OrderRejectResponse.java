package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundStatusResponse;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;

public record OrderRejectResponse(Long orderId,
                                  String orderStatus,
                                  RefundStatusResponse refund) {

    public static OrderRejectResponse from(OrderApprovalUseCase.RejectResult result) {
        return new OrderRejectResponse(
                result.order().getId(),
                result.order().getStatus().name(),
                RefundStatusResponse.from(result.refund()));
    }
}

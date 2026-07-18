package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundStatusResponse;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

public record OrderDelayCancellationResponse(Long orderId,
                                             OrderStatus orderStatus,
                                             LocalDate expectedShipDate,
                                             RefundStatusResponse refund) {

    public static OrderDelayCancellationResponse from(
            OrderProductionUseCase.DelayCancellationResult result) {
        return new OrderDelayCancellationResponse(
                result.production().orderId(),
                result.production().status(),
                result.production().expectedShipDate(),
                RefundStatusResponse.from(result.refund()));
    }
}

package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.payment.dto.RefundStatusResponse;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record OrderDelayCancellationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus orderStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate expectedShipDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RefundStatusResponse refund
) {

    public static OrderDelayCancellationResponse from(
            OrderProductionUseCase.DelayCancellationResult result) {
        return new OrderDelayCancellationResponse(
                result.production().orderId(),
                result.production().status(),
                result.production().expectedShipDate(),
                RefundStatusResponse.from(result.refund()));
    }
}

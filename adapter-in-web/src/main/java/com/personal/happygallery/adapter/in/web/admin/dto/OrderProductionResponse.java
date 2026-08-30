package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.OrderProductionUseCase.ProductionResult;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 예약 제작 관련 Admin 응답 */
public record OrderProductionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate expectedShipDate
) {
    public static OrderProductionResponse from(ProductionResult result) {
        return new OrderProductionResponse(result.orderId(), result.status(), result.expectedShipDate());
    }
}

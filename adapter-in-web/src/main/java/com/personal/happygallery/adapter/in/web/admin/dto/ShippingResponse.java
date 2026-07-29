package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.OrderShippingUseCase.ShippingResult;
import com.personal.happygallery.domain.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** 배송 관련 Admin 응답 */
public record ShippingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate expectedShipDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String carrier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingNumber
) {
    public static ShippingResponse from(ShippingResult result) {
        return new ShippingResponse(
                result.orderId(),
                result.status(),
                result.expectedShipDate(),
                result.carrier(),
                result.trackingNumber());
    }
}

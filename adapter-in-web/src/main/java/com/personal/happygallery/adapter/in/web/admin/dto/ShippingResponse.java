package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.OrderShippingUseCase.ShippingResult;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import com.personal.happygallery.domain.order.TrackingRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 배송 관련 Admin 응답 */
public record ShippingResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate expectedShipDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String carrier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) ShippingCarrier carrierCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        TrackingRegistrationStatus trackingRegistrationStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) ShipmentTrackingStatus trackingStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingStatusText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime trackingUpdatedAt
) {
    public static ShippingResponse from(ShippingResult result) {
        return new ShippingResponse(
                result.orderId(),
                result.status(),
                result.expectedShipDate(),
                result.carrier(),
                result.trackingNumber(),
                result.carrierCode(),
                result.trackingRegistrationStatus(),
                result.trackingStatus(),
                result.trackingStatusText(),
                result.trackingUpdatedAt());
    }
}

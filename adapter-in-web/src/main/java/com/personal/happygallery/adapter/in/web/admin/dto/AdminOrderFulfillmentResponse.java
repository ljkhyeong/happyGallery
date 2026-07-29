package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminOrderFulfillmentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FulfillmentType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Address shippingAddress,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate expectedShipDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime pickupDeadlineAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String carrier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingNumber
) {

    public static AdminOrderFulfillmentResponse from(
            com.personal.happygallery.application.order.port.in.AdminOrderFulfillmentResponse response) {
        return new AdminOrderFulfillmentResponse(
                response.orderId(),
                FulfillmentType.valueOf(response.type()),
                Address.from(response.shippingAddress()),
                response.expectedShipDate(),
                response.pickupDeadlineAt(),
                response.carrier(),
                response.trackingNumber());
    }

    @Schema(name = "AdminOrderShippingAddress")
    public record Address(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String recipientName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phone,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String postalCode,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String addressLine1,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String addressLine2
    ) {

        private static Address from(ShippingAddress address) {
            if (address == null) {
                return null;
            }
            return new Address(
                    address.recipientName(),
                    address.phone(),
                    address.postalCode(),
                    address.addressLine1(),
                    address.addressLine2());
        }
    }
}

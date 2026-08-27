package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.order.ShipmentTrackingEvent;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import com.personal.happygallery.domain.order.TrackingRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderFulfillmentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FulfillmentType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Address shippingAddress,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate expectedShipDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime pickupDeadlineAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String carrier,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) ShippingCarrier carrierCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        TrackingRegistrationStatus trackingRegistrationStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) ShipmentTrackingStatus trackingStatus,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String trackingStatusText,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime trackingUpdatedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TrackingEvent> trackingEvents
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
                response.trackingNumber(),
                response.carrierCode(),
                response.trackingRegistrationStatus(),
                response.trackingStatus(),
                response.trackingStatusText(),
                response.trackingUpdatedAt(),
                response.trackingEvents().stream().map(TrackingEvent::from).toList());
    }

    public record TrackingEvent(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime occurredAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ShipmentTrackingStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String statusText,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String location,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String description
    ) {
        private static TrackingEvent from(ShipmentTrackingEvent event) {
            return new TrackingEvent(
                    event.getOccurredAt(), event.getStatus(), event.getStatusText(),
                    event.getLocation(), event.getDescription());
        }
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

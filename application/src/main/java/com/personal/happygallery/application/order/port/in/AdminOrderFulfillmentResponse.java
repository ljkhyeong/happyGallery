package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.order.ShipmentTrackingEvent;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import com.personal.happygallery.domain.order.TrackingRegistrationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자에게만 노출하는 주문 이행 상세. */
public record AdminOrderFulfillmentResponse(
        Long orderId,
        String type,
        ShippingAddress shippingAddress,
        LocalDate expectedShipDate,
        LocalDateTime pickupDeadlineAt,
        String carrier,
        String trackingNumber,
        ShippingCarrier carrierCode,
        TrackingRegistrationStatus trackingRegistrationStatus,
        ShipmentTrackingStatus trackingStatus,
        String trackingStatusText,
        LocalDateTime trackingUpdatedAt,
        List<ShipmentTrackingEvent> trackingEvents
) {

    public AdminOrderFulfillmentResponse {
        trackingEvents = List.copyOf(trackingEvents);
    }

    public static AdminOrderFulfillmentResponse from(
            Fulfillment fulfillment,
            ShippingAddress shippingAddress,
            List<ShipmentTrackingEvent> trackingEvents) {
        return new AdminOrderFulfillmentResponse(
                fulfillment.getOrderId(),
                fulfillment.getType().name(),
                shippingAddress,
                fulfillment.getExpectedShipDate(),
                fulfillment.getPickupDeadlineAt(),
                fulfillment.getCarrier(),
                fulfillment.getTrackingNumber(),
                fulfillment.getCarrierCode(),
                fulfillment.getTrackingRegistrationStatus(),
                fulfillment.getTrackingStatus(),
                fulfillment.getTrackingStatusText(),
                fulfillment.getTrackingUpdatedAt(),
                trackingEvents);
    }
}

package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.ShippingAddress;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 관리자에게만 노출하는 주문 이행 상세. */
public record AdminOrderFulfillmentResponse(
        Long orderId,
        String type,
        ShippingAddress shippingAddress,
        LocalDate expectedShipDate,
        LocalDateTime pickupDeadlineAt,
        String carrier,
        String trackingNumber
) {

    public static AdminOrderFulfillmentResponse from(
            Fulfillment fulfillment, ShippingAddress shippingAddress) {
        return new AdminOrderFulfillmentResponse(
                fulfillment.getOrderId(),
                fulfillment.getType().name(),
                shippingAddress,
                fulfillment.getExpectedShipDate(),
                fulfillment.getPickupDeadlineAt(),
                fulfillment.getCarrier(),
                fulfillment.getTrackingNumber());
    }
}

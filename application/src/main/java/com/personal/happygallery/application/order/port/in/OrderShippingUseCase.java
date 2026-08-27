package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import com.personal.happygallery.domain.order.TrackingRegistrationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 배송 이행 관리 유스케이스.
 *
 * <p>배송 준비, 배송 출발, 배송 완료를 지원한다.
 */
public interface OrderShippingUseCase {

    record ShippingResult(
            Long orderId,
            OrderStatus status,
            LocalDate expectedShipDate,
            String carrier,
            String trackingNumber,
            ShippingCarrier carrierCode,
            TrackingRegistrationStatus trackingRegistrationStatus,
            ShipmentTrackingStatus trackingStatus,
            String trackingStatusText,
            LocalDateTime trackingUpdatedAt
    ) {
        public static ShippingResult of(Order order, Fulfillment fulfillment) {
            return new ShippingResult(
                    order.getId(),
                    order.getStatus(),
                    fulfillment.getExpectedShipDate(),
                    fulfillment.getCarrier(),
                    fulfillment.getTrackingNumber(),
                    fulfillment.getCarrierCode(),
                    fulfillment.getTrackingRegistrationStatus(),
                    fulfillment.getTrackingStatus(),
                    fulfillment.getTrackingStatusText(),
                    fulfillment.getTrackingUpdatedAt());
        }
    }

    ShippingResult prepareShipping(Long orderId, Long adminId);

    ShippingResult markShipped(
            Long orderId,
            ShippingCarrier carrierCode,
            String carrier,
            String trackingNumber,
            Long adminId);

    ShippingResult markShipped(Long orderId, String carrier, String trackingNumber, Long adminId);

    ShippingResult markDelivered(Long orderId, Long adminId);
}

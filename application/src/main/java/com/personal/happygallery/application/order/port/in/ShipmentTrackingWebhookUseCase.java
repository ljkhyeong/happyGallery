package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import java.time.LocalDateTime;
import java.util.List;

public interface ShipmentTrackingWebhookUseCase {

    record TrackingEvent(
            LocalDateTime occurredAt,
            ShipmentTrackingStatus status,
            String statusText,
            String location,
            String description
    ) {}

    record TrackingUpdate(
            Long orderId,
            ShippingCarrier carrier,
            String trackingNumber,
            ShipmentTrackingStatus status,
            String statusText,
            List<TrackingEvent> events
    ) {
        public TrackingUpdate {
            events = events == null ? null : List.copyOf(events);
        }
    }

    void apply(List<TrackingUpdate> updates);
}

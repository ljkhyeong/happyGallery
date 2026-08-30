package com.personal.happygallery.adapter.in.web.webhook.dto;

import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase.TrackingEvent;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase.TrackingUpdate;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public record DeliveryTrackingWebhookRequest(
        String event,
        String requestId,
        String timestamp,
        List<Item> items
) {
    private static final String ORDER_CLIENT_PREFIX = "order-";
    private static final DateTimeFormatter PROGRESS_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<TrackingUpdate> toUpdates() {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(Item::toUpdate)
                .flatMap(Optional::stream)
                .toList();
    }

    public record Item(
            String courierCode,
            String trackingNumber,
            String clientId,
            String currentStatus,
            String previousStatus,
            Boolean hasChanged,
            Boolean isDelivered,
            TrackingData trackingData
    ) {
        private Optional<TrackingUpdate> toUpdate() {
            Long orderId = orderId(clientId);
            ShippingCarrier carrier = ShippingCarrier.fromProviderCode(courierCode).orElse(null);
            if (orderId == null || carrier == null || trackingNumber == null) {
                return Optional.empty();
            }
            ShipmentTrackingStatus status = ShipmentTrackingStatus.fromProvider(currentStatus);
            String statusText = trackingData != null && trackingData.deliveryStatusText() != null
                    ? trackingData.deliveryStatusText()
                    : status.name();
            List<TrackingEvent> events = trackingData == null || trackingData.progresses() == null
                    ? null
                    : trackingData.progresses().stream()
                            .map(Progress::toEvent)
                            .flatMap(Optional::stream)
                            .toList();
            return Optional.of(new TrackingUpdate(
                    orderId, carrier, trackingNumber, status, statusText, events));
        }
    }

    public record TrackingData(
            String trackingNumber,
            String courierCode,
            String courierName,
            String deliveryStatus,
            String deliveryStatusText,
            Boolean isDelivered,
            String dateLastProgress,
            List<Progress> progresses,
            String queriedAt
    ) {}

    public record Progress(
            String dateTime,
            String location,
            String status,
            String statusCode,
            String description
    ) {
        private Optional<TrackingEvent> toEvent() {
            if (dateTime == null) {
                return Optional.empty();
            }
            try {
                ShipmentTrackingStatus trackingStatus = ShipmentTrackingStatus.fromProvider(statusCode);
                String text = status != null ? status : trackingStatus.name();
                return Optional.of(new TrackingEvent(
                        LocalDateTime.parse(dateTime, PROGRESS_TIME),
                        trackingStatus,
                        text,
                        location,
                        description));
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }
    }

    private static Long orderId(String clientId) {
        if (clientId == null || !clientId.startsWith(ORDER_CLIENT_PREFIX)) {
            return null;
        }
        try {
            return Long.valueOf(clientId.substring(ORDER_CLIENT_PREFIX.length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

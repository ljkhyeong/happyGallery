package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingEventPort;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.ShipmentTrackingEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultShipmentTrackingWebhookService implements ShipmentTrackingWebhookUseCase {

    private final FulfillmentPort fulfillmentPort;
    private final ShipmentTrackingEventPort trackingEventPort;
    private final Clock clock;

    public DefaultShipmentTrackingWebhookService(
            FulfillmentPort fulfillmentPort,
            ShipmentTrackingEventPort trackingEventPort,
            Clock clock) {
        this.fulfillmentPort = fulfillmentPort;
        this.trackingEventPort = trackingEventPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void apply(List<TrackingUpdate> updates) {
        LocalDateTime receivedAt = LocalDateTime.now(clock);
        for (TrackingUpdate update : updates) {
            Fulfillment fulfillment = fulfillmentPort.findByOrderId(update.orderId()).orElse(null);
            if (fulfillment == null
                    || !fulfillment.matchesTracking(update.carrier(), update.trackingNumber())) {
                continue;
            }
            fulfillment.applyTrackingUpdate(update.status(), update.statusText(), receivedAt);
            fulfillmentPort.save(fulfillment);
            replaceEvents(update);
        }
    }

    private void replaceEvents(TrackingUpdate update) {
        if (update.events() == null) {
            return;
        }
        trackingEventPort.deleteByOrderId(update.orderId());
        List<ShipmentTrackingEvent> events = update.events().stream()
                .map(event -> new ShipmentTrackingEvent(
                        update.orderId(),
                        event.occurredAt(),
                        event.status(),
                        event.statusText(),
                        event.location(),
                        event.description()))
                .toList();
        trackingEventPort.saveAll(events);
    }
}

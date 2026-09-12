package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase.TrackingUpdate;
import java.util.Optional;

public interface KoreaPostTrackingLookup {
    boolean isEnabled();
    Optional<TrackingUpdate> lookup(Long orderId, String trackingNumber);
}

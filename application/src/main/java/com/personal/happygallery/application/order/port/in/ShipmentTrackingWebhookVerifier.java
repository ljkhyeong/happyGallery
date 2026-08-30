package com.personal.happygallery.application.order.port.in;

public interface ShipmentTrackingWebhookVerifier {
    boolean verify(String timestamp, String signature, byte[] body);
}

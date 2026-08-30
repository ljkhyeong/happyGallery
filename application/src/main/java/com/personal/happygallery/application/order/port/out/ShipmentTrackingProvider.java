package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.ShippingCarrier;

public interface ShipmentTrackingProvider {

    record RegistrationCommand(Long orderId, ShippingCarrier carrier, String trackingNumber) {}

    record RegistrationResult(boolean success, boolean retryable, String requestId, String reason) {
        public static RegistrationResult success(String requestId) {
            return new RegistrationResult(true, false, requestId, null);
        }

        public static RegistrationResult retryableFailure(String reason) {
            return new RegistrationResult(false, true, null, reason);
        }

        public static RegistrationResult failure(String reason) {
            return new RegistrationResult(false, false, null, reason);
        }
    }

    boolean isEnabled();

    RegistrationResult register(RegistrationCommand command);
}

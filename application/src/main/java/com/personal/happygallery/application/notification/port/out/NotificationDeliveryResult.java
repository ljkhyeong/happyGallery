package com.personal.happygallery.application.notification.port.out;

public record NotificationDeliveryResult(
        NotificationDeliveryResultStatus status,
        String reason
) {

    public static NotificationDeliveryResult delivered() {
        return new NotificationDeliveryResult(NotificationDeliveryResultStatus.DELIVERED, null);
    }

    public static NotificationDeliveryResult failed(String reason) {
        return new NotificationDeliveryResult(NotificationDeliveryResultStatus.FAILED, reason);
    }

    public static NotificationDeliveryResult pending() {
        return new NotificationDeliveryResult(NotificationDeliveryResultStatus.PENDING, null);
    }

    public static NotificationDeliveryResult unavailable(String reason) {
        return new NotificationDeliveryResult(NotificationDeliveryResultStatus.UNAVAILABLE, reason);
    }
}

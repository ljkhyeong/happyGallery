package com.personal.happygallery.domain.notification;

public enum NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    DELIVERY_PENDING,
    DELIVERY_CHECKING,
    SENT,
    OBSOLETE,
    FAILED
}

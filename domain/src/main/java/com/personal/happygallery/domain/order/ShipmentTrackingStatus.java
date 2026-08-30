package com.personal.happygallery.domain.order;

/** 택배사가 제공하는 배송 진행 상태. */
public enum ShipmentTrackingStatus {
    PENDING,
    REGISTERED,
    PICKUP_READY,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    RETURNED,
    CANCELLED,
    HOLD,
    UNKNOWN;

    public static ShipmentTrackingStatus fromProvider(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}

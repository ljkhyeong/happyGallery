package com.personal.happygallery.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/** 택배사가 전달한 배송 진행 이력. */
@Entity
@Table(name = "shipment_tracking_events")
public class ShipmentTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentTrackingStatus status;

    @Column(name = "status_text", nullable = false, length = 100)
    private String statusText;

    @Column(length = 200)
    private String location;

    @Column(length = 500)
    private String description;

    protected ShipmentTrackingEvent() {}

    public ShipmentTrackingEvent(Long orderId,
                                 LocalDateTime occurredAt,
                                 ShipmentTrackingStatus status,
                                 String statusText,
                                 String location,
                                 String description) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.statusText = Objects.requireNonNull(statusText, "statusText must not be null");
        this.location = location;
        this.description = description;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public ShipmentTrackingStatus getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
}

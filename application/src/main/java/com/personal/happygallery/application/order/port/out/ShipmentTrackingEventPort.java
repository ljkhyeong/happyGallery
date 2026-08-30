package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.ShipmentTrackingEvent;
import java.util.List;

public interface ShipmentTrackingEventPort {
    List<ShipmentTrackingEvent> findByOrderIdOrderByOccurredAtAsc(Long orderId);
    <S extends ShipmentTrackingEvent> List<S> saveAll(Iterable<S> events);
    void deleteByOrderId(Long orderId);
}

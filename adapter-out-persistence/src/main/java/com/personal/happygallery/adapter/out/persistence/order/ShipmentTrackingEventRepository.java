package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.ShipmentTrackingEventPort;
import com.personal.happygallery.domain.order.ShipmentTrackingEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentTrackingEventRepository
        extends JpaRepository<ShipmentTrackingEvent, Long>, ShipmentTrackingEventPort {

    @Override
    List<ShipmentTrackingEvent> findByOrderIdOrderByOccurredAtAsc(Long orderId);

    @Override
    <S extends ShipmentTrackingEvent> List<S> saveAll(Iterable<S> events);

    @Override
    void deleteByOrderId(Long orderId);
}

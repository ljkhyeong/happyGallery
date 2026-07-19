package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.Fulfillment;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface FulfillmentPort {
    Fulfillment save(Fulfillment fulfillment);
    Optional<Fulfillment> findByOrderId(Long orderId);
    List<Fulfillment> findByOrderIdIn(Collection<Long> orderIds);
    List<Fulfillment> findAll();
    List<Fulfillment> findExpiredPickupsAfterId(LocalDateTime now, Long afterId, Pageable pageable);
    List<PickupReminderTarget> findPickupReminderTargets(LocalDateTime from, LocalDateTime to);
}

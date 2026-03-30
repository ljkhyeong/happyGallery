package com.personal.happygallery.app.order.port.out;

import com.personal.happygallery.domain.order.Fulfillment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface FulfillmentPort {
    Fulfillment save(Fulfillment fulfillment);
    Optional<Fulfillment> findByOrderId(Long orderId);
    List<Fulfillment> findAll();
    List<Fulfillment> findExpiredPickups(LocalDateTime now);
    List<Fulfillment> findExpiredPickups(LocalDateTime now, Pageable pageable);
    List<Fulfillment> findPickupsApproachingDeadline(LocalDateTime from, LocalDateTime to);
}

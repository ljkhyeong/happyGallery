package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {

    Optional<Fulfillment> findByOrderId(Long orderId);

    /** 픽업 만료 배치용 조회: status=PICKUP_READY AND pickupDeadlineAt &lt; deadline */
    List<Fulfillment> findByStatusAndPickupDeadlineAtBefore(OrderStatus status, LocalDateTime deadline);
}

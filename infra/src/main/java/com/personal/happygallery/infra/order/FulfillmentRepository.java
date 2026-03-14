package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.Fulfillment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {

    Optional<Fulfillment> findByOrderId(Long orderId);

    /** 픽업 만료 배치용 조회: Order.status=PICKUP_READY AND pickupDeadlineAt &lt; now */
    @Query("SELECT f FROM Fulfillment f, Order o "
            + "WHERE f.orderId = o.id "
            + "AND o.status = com.personal.happygallery.domain.order.OrderStatus.PICKUP_READY "
            + "AND f.pickupDeadlineAt < :now")
    List<Fulfillment> findExpiredPickups(@Param("now") LocalDateTime now);
}

package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {

    Optional<Fulfillment> findByOrderId(Long orderId);

    /** 픽업 만료 배치용 조회: Order.status=PICKUP_READY AND pickupDeadlineAt &lt; now */
    @Query("SELECT f FROM Fulfillment f JOIN Order o ON f.orderId = o.id "
            + "WHERE o.status = :status "
            + "AND f.pickupDeadlineAt < :now")
    List<Fulfillment> findExpiredPickups(@Param("status") OrderStatus status,
                                         @Param("now") LocalDateTime now);
}

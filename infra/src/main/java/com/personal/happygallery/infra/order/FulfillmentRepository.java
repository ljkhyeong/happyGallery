package com.personal.happygallery.infra.order;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.OrderStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {

    Optional<Fulfillment> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Fulfillment f WHERE f.orderId = :orderId")
    Optional<Fulfillment> findByOrderIdWithLock(@Param("orderId") Long orderId);

    /** 픽업 만료 배치용 조회: status=PICKUP_READY AND pickupDeadlineAt &lt; deadline */
    List<Fulfillment> findByStatusAndPickupDeadlineAtBefore(OrderStatus status, LocalDateTime deadline);
}

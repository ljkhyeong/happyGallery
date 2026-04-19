package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.domain.order.Fulfillment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long>, FulfillmentPort {

    @Override Fulfillment save(Fulfillment fulfillment);

    Optional<Fulfillment> findByOrderId(Long orderId);

    /** 픽업 만료 배치용 조회: Order.status=PICKUP_READY AND pickupDeadlineAt &lt; now */
    @Query("SELECT f FROM Fulfillment f JOIN Order o ON f.orderId = o.id "
            + "WHERE o.status = 'PICKUP_READY' "
            + "AND f.pickupDeadlineAt < :now")
    List<Fulfillment> findExpiredPickups(@Param("now") LocalDateTime now);

    /** 픽업 만료 배치 페이지네이션 조회 */
    @Query("SELECT f FROM Fulfillment f JOIN Order o ON f.orderId = o.id "
            + "WHERE o.status = 'PICKUP_READY' "
            + "AND f.pickupDeadlineAt < :now")
    List<Fulfillment> findExpiredPickups(@Param("now") LocalDateTime now, Pageable pageable);

    /** 픽업 마감 임박 알림용 조회: Order.status=PICKUP_READY AND pickupDeadlineAt BETWEEN from AND to */
    @Query("SELECT f FROM Fulfillment f JOIN Order o ON f.orderId = o.id "
            + "WHERE o.status = 'PICKUP_READY' "
            + "AND f.pickupDeadlineAt BETWEEN :from AND :to")
    List<Fulfillment> findPickupsApproachingDeadline(@Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);
}

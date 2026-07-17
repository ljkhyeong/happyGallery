package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.PickupReminderTarget;
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

    @Override Optional<Fulfillment> findByOrderId(Long orderId);

    /** 픽업 만료 배치용 조회: Order.status=PICKUP_READY AND pickupDeadlineAt &lt; now */
    @Override
    @Query("SELECT f FROM Fulfillment f JOIN Order o ON f.orderId = o.id "
            + "WHERE o.status = 'PICKUP_READY' "
            + "AND f.pickupDeadlineAt < :now")
    List<Fulfillment> findExpiredPickups(@Param("now") LocalDateTime now);

    /** 픽업 만료 배치 페이지네이션 조회 */
    @Override
    @Query("SELECT f FROM Fulfillment f JOIN Order o ON f.orderId = o.id "
            + "WHERE o.status = 'PICKUP_READY' "
            + "AND f.pickupDeadlineAt < :now")
    List<Fulfillment> findExpiredPickups(@Param("now") LocalDateTime now, Pageable pageable);

    /** 픽업 마감 임박 알림 대상과 주문 수신자를 한 번에 조회한다. */
    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.order.port.out.PickupReminderTarget(
                f.orderId, o.userId, o.guestId)
            FROM Fulfillment f
            JOIN Order o ON f.orderId = o.id
            WHERE o.status = 'PICKUP_READY'
              AND f.pickupDeadlineAt BETWEEN :from AND :to
            """)
    List<PickupReminderTarget> findPickupReminderTargets(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);
}

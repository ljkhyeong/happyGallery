package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.PickupReminderTarget;
import com.personal.happygallery.domain.order.Fulfillment;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {

    Optional<Fulfillment> findByOrderId(Long orderId);

    List<Fulfillment> findByOrderIdIn(Collection<Long> orderIds);

    /** 픽업 만료 배치 페이지네이션 조회 */
    @Query("SELECT f FROM Fulfillment f JOIN Order o ON f.orderId = o.id "
            + "WHERE o.status = 'PICKUP_READY' "
            + "AND f.pickupDeadlineAt < :now "
            + "AND f.id > :afterId "
            + "ORDER BY f.id ASC")
    List<Fulfillment> findExpiredPickupsAfterIdPage(@Param("now") LocalDateTime now,
                                                    @Param("afterId") Long afterId,
                                                    Pageable pageable);

    default List<Fulfillment> findExpiredPickupsAfterId(
            LocalDateTime now, Long afterId, int limit) {
        return findExpiredPickupsAfterIdPage(now, afterId, PageRequest.ofSize(limit));
    }

    /** 픽업 마감 임박 알림 대상과 주문 수신자를 한 번에 조회한다. */
    @Query("""
            SELECT new com.personal.happygallery.application.order.port.out.PickupReminderTarget(
                f.orderId, o.userId, o.guestId)
            FROM Fulfillment f
            JOIN Order o ON f.orderId = o.id
            WHERE o.status = 'PICKUP_READY'
              AND f.pickupDeadlineAt BETWEEN :from AND :to
              AND NOT EXISTS (
                  SELECT n.id
                  FROM NotificationOutbox n
                  WHERE n.eventType =
                      com.personal.happygallery.domain.notification.NotificationEventType.PICKUP_DEADLINE_REMINDER
                    AND n.aggregateType = 'ORDER'
                    AND n.aggregateId = f.orderId
              )
            """)
    List<PickupReminderTarget> findPickupReminderTargets(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);
}

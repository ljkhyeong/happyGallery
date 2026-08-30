package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.Fulfillment;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FulfillmentPort {
    <S extends Fulfillment> S save(S fulfillment);
    Optional<Fulfillment> findByOrderId(Long orderId);
    Optional<Fulfillment> findByIdForUpdate(Long id);
    List<Fulfillment> findByOrderIdIn(Collection<Long> orderIds);
    List<Long> findTrackingRegistrationCandidateIds(
            LocalDateTime now, LocalDateTime processingStaleBefore, int limit);
    List<Fulfillment> findExpiredPickupsAfterId(LocalDateTime now, Long afterId, int limit);
    List<PickupReminderTarget> findPickupReminderTargets(LocalDateTime from, LocalDateTime to);
}

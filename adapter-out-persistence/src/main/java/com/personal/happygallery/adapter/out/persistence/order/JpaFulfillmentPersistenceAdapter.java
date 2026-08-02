package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.PickupReminderTarget;
import com.personal.happygallery.domain.order.Fulfillment;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaFulfillmentPersistenceAdapter implements FulfillmentPort {

    private final FulfillmentRepository repository;

    JpaFulfillmentPersistenceAdapter(FulfillmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Fulfillment save(Fulfillment fulfillment) {
        return repository.save(fulfillment);
    }

    @Override
    public Optional<Fulfillment> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId);
    }

    @Override
    public List<Fulfillment> findByOrderIdIn(Collection<Long> orderIds) {
        return repository.findByOrderIdIn(orderIds);
    }

    @Override
    public List<Fulfillment> findExpiredPickupsAfterId(LocalDateTime now, Long afterId, int limit) {
        return repository.findExpiredPickupsAfterId(now, afterId, limit);
    }

    @Override
    public List<PickupReminderTarget> findPickupReminderTargets(
            LocalDateTime from, LocalDateTime to) {
        return repository.findPickupReminderTargets(from, to);
    }
}

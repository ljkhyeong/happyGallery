package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderClaimItemPort;
import com.personal.happygallery.application.order.port.out.OrderItemClaimedQuantity;
import com.personal.happygallery.domain.order.OrderClaimItem;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaOrderClaimItemPersistenceAdapter implements OrderClaimItemPort {

    private final OrderClaimItemRepository repository;

    JpaOrderClaimItemPersistenceAdapter(OrderClaimItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OrderClaimItem> saveAll(List<OrderClaimItem> items) {
        return repository.saveAll(items);
    }

    @Override
    public List<OrderClaimItem> findByClaimIdIn(Collection<Long> claimIds) {
        return repository.findByClaimIdIn(claimIds);
    }

    @Override
    public List<OrderItemClaimedQuantity> sumNonRejectedQuantities(
            Collection<Long> orderItemIds) {
        return repository.sumNonRejectedQuantities(orderItemIds);
    }
}

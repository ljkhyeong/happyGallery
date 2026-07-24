package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.OrderClaimItem;
import java.util.Collection;
import java.util.List;

public interface OrderClaimItemPort {

    List<OrderClaimItem> saveAll(List<OrderClaimItem> items);

    List<OrderClaimItem> findByClaimIdIn(Collection<Long> claimIds);

    List<OrderItemClaimedQuantity> sumNonRejectedQuantities(Collection<Long> orderItemIds);
}

package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.OrderClaimItem;
import java.util.Collection;
import java.util.List;

public interface OrderClaimItemPort {

    <S extends OrderClaimItem> List<S> saveAll(Iterable<S> items);

    List<OrderClaimItem> findByClaimIdIn(Collection<Long> claimIds);

    List<OrderItemClaimedQuantity> sumNonRejectedQuantities(Collection<Long> orderItemIds);

    List<OrderItemApprovedRefundState> summarizeApprovedRefunds(Collection<Long> orderItemIds);
}

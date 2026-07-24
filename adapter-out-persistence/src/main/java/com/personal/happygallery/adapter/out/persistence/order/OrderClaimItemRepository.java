package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.OrderClaimItemPort;
import com.personal.happygallery.application.order.port.out.OrderItemClaimedQuantity;
import com.personal.happygallery.domain.order.OrderClaimItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderClaimItemRepository extends JpaRepository<OrderClaimItem, Long>, OrderClaimItemPort {

    @Override
    default List<OrderClaimItem> saveAll(List<OrderClaimItem> items) {
        return saveAll((Iterable<OrderClaimItem>) items);
    }

    @Override List<OrderClaimItem> findByClaimIdIn(Collection<Long> claimIds);

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.order.port.out.OrderItemClaimedQuantity(
                item.orderItemId, SUM(item.quantity))
            FROM OrderClaimItem item
            JOIN OrderClaim claim ON claim.id = item.claimId
            WHERE item.orderItemId IN :orderItemIds
              AND claim.status <> com.personal.happygallery.domain.order.OrderClaimStatus.REJECTED
            GROUP BY item.orderItemId
            """)
    List<OrderItemClaimedQuantity> sumNonRejectedQuantities(
            @Param("orderItemIds") Collection<Long> orderItemIds);
}

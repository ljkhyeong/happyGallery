package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.SmartStoreProductOrderPort;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreProductOrderRepository
        extends JpaRepository<SmartStoreProductOrder, String>, SmartStoreProductOrderPort {

    @Override
    default Optional<SmartStoreProductOrder> findByProductOrderId(String productOrderId) {
        return findById(productOrderId);
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select channelOrder from SmartStoreProductOrder channelOrder "
            + "where channelOrder.productOrderId = :productOrderId")
    Optional<SmartStoreProductOrder> findByProductOrderIdWithLock(
            @Param("productOrderId") String productOrderId);

    @Override
    @Query(value = """
            select *
              from smartstore_product_orders
             where (:attentionOnly = false or attention_reason is not null)
             order by last_changed_at desc, product_order_id desc
             limit :limit
            """, nativeQuery = true)
    List<SmartStoreProductOrder> findRecent(
            @Param("attentionOnly") boolean attentionOnly,
            @Param("limit") int limit);

    @Override
    @Query("""
            select count(channelOrder) > 0 from SmartStoreProductOrder channelOrder
             where channelOrder.attentionReason in :reasons
               and (channelOrder.productId = :productId
                    or (channelOrder.productId is null
                        and (channelOrder.originProductNo in (
                                select mapping.originProductNo from SmartStoreStockMapping mapping
                                 where mapping.productId = :productId
                            )
                            or exists (
                                select history.id from SmartStoreOrderMappingHistory history
                                 where history.productId = :productId
                                   and history.originProductNo = channelOrder.originProductNo
                                   and history.closedAt >= coalesce(
                                       channelOrder.paymentDate, channelOrder.lastChangedAt)
                            ))))
            """)
    boolean existsInventoryAttentionForProduct(
            @Param("productId") Long productId,
            @Param("reasons") Collection<SmartStoreOrderAttentionReason> reasons);

    @Override
    <S extends SmartStoreProductOrder> S save(S order);
}

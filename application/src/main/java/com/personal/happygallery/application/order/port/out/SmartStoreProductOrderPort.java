package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SmartStoreProductOrderPort {

    Optional<SmartStoreProductOrder> findByProductOrderId(String productOrderId);

    Optional<SmartStoreProductOrder> findByProductOrderIdWithLock(String productOrderId);

    List<SmartStoreProductOrder> findRecentPage(
            boolean attentionOnly,
            SmartStoreOrderAttentionReason attentionReason,
            LocalDateTime cursorAt,
            String cursorProductOrderId,
            int limit);

    boolean existsInventoryAttentionForProduct(Long productId, Collection<SmartStoreOrderAttentionReason> reasons);

    <S extends SmartStoreProductOrder> S save(S order);
}

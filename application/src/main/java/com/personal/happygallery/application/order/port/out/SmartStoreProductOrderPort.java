package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.SmartStoreProductOrder;
import java.util.List;
import java.util.Optional;

public interface SmartStoreProductOrderPort {

    Optional<SmartStoreProductOrder> findByProductOrderIdWithLock(String productOrderId);

    List<SmartStoreProductOrder> findRecent(boolean attentionOnly, int limit);

    <S extends SmartStoreProductOrder> S save(S order);
}

package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.SmartStoreOrderActionHistory;
import java.util.List;
import java.util.Optional;

public interface SmartStoreOrderActionHistoryPort {

    <S extends SmartStoreOrderActionHistory> S save(S history);

    Optional<SmartStoreOrderActionHistory> findByIdWithLock(Long id);

    List<SmartStoreOrderActionHistory> findRecentByProductOrderId(String productOrderId);
}

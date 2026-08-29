package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.SmartStoreOrderSyncState;
import java.util.Optional;

public interface SmartStoreOrderSyncStatePort {

    Optional<SmartStoreOrderSyncState> findByIdWithLock(Long id);

    <S extends SmartStoreOrderSyncState> S save(S state);
}

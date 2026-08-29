package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.SmartStoreSettlementSyncState;
import java.util.Optional;

public interface SmartStoreSettlementSyncStatePort {

    Optional<SmartStoreSettlementSyncState> findByIdWithLock(Long id);
}

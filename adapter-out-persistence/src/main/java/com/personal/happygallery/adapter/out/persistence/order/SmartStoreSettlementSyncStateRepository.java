package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.SmartStoreSettlementSyncStatePort;
import com.personal.happygallery.domain.order.SmartStoreSettlementSyncState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreSettlementSyncStateRepository
        extends JpaRepository<SmartStoreSettlementSyncState, Long>,
        SmartStoreSettlementSyncStatePort {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from SmartStoreSettlementSyncState state where state.id = :id")
    Optional<SmartStoreSettlementSyncState> findByIdWithLock(@Param("id") Long id);
}

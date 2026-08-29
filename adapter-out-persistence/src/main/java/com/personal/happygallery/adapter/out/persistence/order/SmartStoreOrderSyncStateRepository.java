package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderSyncStatePort;
import com.personal.happygallery.domain.order.SmartStoreOrderSyncState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreOrderSyncStateRepository
        extends JpaRepository<SmartStoreOrderSyncState, Long>, SmartStoreOrderSyncStatePort {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from SmartStoreOrderSyncState state where state.id = :id")
    Optional<SmartStoreOrderSyncState> findByIdWithLock(@Param("id") Long id);
}

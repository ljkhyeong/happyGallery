package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderActionHistoryPort;
import com.personal.happygallery.domain.order.SmartStoreOrderActionHistory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreOrderActionHistoryRepository
        extends JpaRepository<SmartStoreOrderActionHistory, Long>, SmartStoreOrderActionHistoryPort {

    @Override
    <S extends SmartStoreOrderActionHistory> S save(S history);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select history from SmartStoreOrderActionHistory history where history.id = :id")
    Optional<SmartStoreOrderActionHistory> findByIdWithLock(@Param("id") Long id);

    List<SmartStoreOrderActionHistory> findTop50ByProductOrderIdOrderByRequestedAtDescIdDesc(
            String productOrderId);

    @Override
    default List<SmartStoreOrderActionHistory> findRecentByProductOrderId(String productOrderId) {
        return findTop50ByProductOrderIdOrderByRequestedAtDescIdDesc(productOrderId);
    }
}

package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncPort;
import com.personal.happygallery.domain.product.SmartStoreStockSync;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreStockSyncRepository
        extends JpaRepository<SmartStoreStockSync, Long>, SmartStoreStockSyncPort {

    @Override
    Optional<SmartStoreStockSync> findByProductId(Long productId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sync from SmartStoreStockSync sync where sync.productId = :productId")
    Optional<SmartStoreStockSync> findByProductIdWithLock(@Param("productId") Long productId);

    @Override
    @Query(value = """
            select product_id
             from smartstore_stock_syncs
             where (status = 'PENDING' and next_attempt_at <= :now)
                or (status = 'PROCESSING' and processing_started_at <= :staleBefore)
             order by next_attempt_at, product_id
             limit :limit
            """, nativeQuery = true)
    List<Long> findDueProductIds(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("limit") int limit);

    @Override
    <S extends SmartStoreStockSync> S save(S sync);

    @Override
    void deleteByProductId(Long productId);
}

package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.SmartStoreStockSync;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SmartStoreStockSyncPort {

    Optional<SmartStoreStockSync> findByProductId(Long productId);

    Optional<SmartStoreStockSync> findByProductIdWithLock(Long productId);

    List<Long> findDueProductIds(LocalDateTime now, LocalDateTime staleBefore, int limit);

    <S extends SmartStoreStockSync> S save(S sync);

    void deleteByProductId(Long productId);
}

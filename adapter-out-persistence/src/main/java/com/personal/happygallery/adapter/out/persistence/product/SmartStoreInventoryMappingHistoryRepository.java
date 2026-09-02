package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.SmartStoreInventoryMappingHistoryPort;
import com.personal.happygallery.domain.product.SmartStoreInventoryMappingHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartStoreInventoryMappingHistoryRepository
        extends JpaRepository<SmartStoreInventoryMappingHistory, Long>, SmartStoreInventoryMappingHistoryPort {

    @Override
    <S extends SmartStoreInventoryMappingHistory> S save(S history);

    List<SmartStoreInventoryMappingHistory> findTop20ByProductIdOrderByChangedAtDescIdDesc(Long productId);

    @Override
    default List<SmartStoreInventoryMappingHistory> findRecentByProductId(Long productId) {
        return findTop20ByProductIdOrderByChangedAtDescIdDesc(productId);
    }
}

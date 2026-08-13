package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.InventoryAdjustmentHistoryPort;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository
        extends JpaRepository<InventoryAdjustment, Long>, InventoryAdjustmentHistoryPort {

    @Override
    <S extends InventoryAdjustment> S save(S adjustment);

    @Override
    default List<InventoryAdjustment> findRecentByProductId(Long productId) {
        return findTop50ByProductIdOrderByAdjustedAtDescIdDesc(productId);
    }

    List<InventoryAdjustment> findTop50ByProductIdOrderByAdjustedAtDescIdDesc(Long productId);
}

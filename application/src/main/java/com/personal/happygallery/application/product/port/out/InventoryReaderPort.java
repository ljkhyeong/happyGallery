package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.Inventory;
import java.util.List;
import java.util.Optional;

/**
 * 재고 조회 포트.
 */
public interface InventoryReaderPort {

    Optional<Inventory> findByProductId(Long productId);

    List<Inventory> findByProductIdIn(List<Long> productIds);
}

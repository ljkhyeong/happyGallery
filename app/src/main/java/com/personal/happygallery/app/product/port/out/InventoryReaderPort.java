package com.personal.happygallery.app.product.port.out;

import com.personal.happygallery.domain.product.Inventory;
import java.util.Optional;

/**
 * 재고 조회 포트.
 */
public interface InventoryReaderPort {

    Optional<Inventory> findByProductId(Long productId);
}

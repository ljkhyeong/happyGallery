package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.Inventory;
import java.util.Optional;

/**
 * 재고 저장 포트. 비관적 락 조회도 쓰기 트랜잭션 안에서 사용되므로 여기에 포함한다.
 */
public interface InventoryStorePort {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByProductIdWithLock(Long productId);

    void deleteById(Long inventoryId);
}

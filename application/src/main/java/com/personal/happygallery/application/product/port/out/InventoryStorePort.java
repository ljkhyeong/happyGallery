package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.Inventory;
import java.util.List;

/**
 * 재고 저장 포트. 비관적 락 조회도 쓰기 트랜잭션 안에서 사용되므로 여기에 포함한다.
 */
public interface InventoryStorePort {

    <S extends Inventory> S save(S inventory);

    <S extends Inventory> List<S> saveAll(Iterable<S> inventories);

    List<Inventory> findByProductIdInWithLock(List<Long> productIds);

    void deleteById(Long inventoryId);
}

package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.domain.product.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, InventoryReaderPort, InventoryStorePort {

    @Override Inventory save(Inventory inventory);

    /** productId로 재고 조회 (읽기용) */
    Optional<Inventory> findByProductId(Long productId);

    /** 상품 ID 목록으로 재고 일괄 조회 */
    List<Inventory> findByProductIdIn(List<Long> productIds);

    /**
     * 비관적 쓰기 락 — 재고 차감/복구용. 반드시 트랜잭션 안에서 호출해야 한다.
     *
     * <p>여러 상품은 productId 오름차순으로 한 번에 잠가 교착 위험과 반복 조회를 줄인다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds ORDER BY i.productId")
    List<Inventory> findByProductIdInWithLock(@Param("productIds") List<Long> productIds);
}

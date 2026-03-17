package com.personal.happygallery.infra.product;

import com.personal.happygallery.domain.product.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** productId로 재고 조회 (읽기용) */
    Optional<Inventory> findByProductId(Long productId);

    /** 상품 ID 목록으로 재고 일괄 조회 */
    List<Inventory> findByProductIdIn(List<Long> productIds);

    /**
     * 비관적 쓰기 락 — 재고 차감용. 반드시 트랜잭션 안에서 호출해야 한다.
     *
     * <p>단일 작품 중복 판매 방지를 위해 {@code SELECT ... FOR UPDATE}로 row를 잠근다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);
}

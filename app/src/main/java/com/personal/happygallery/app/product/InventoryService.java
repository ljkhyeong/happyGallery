package com.personal.happygallery.app.product;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.infra.product.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 재고를 차감한다.
     *
     * <ol>
     *   <li>비관적 락({@code SELECT FOR UPDATE})으로 재고 row를 잠근다.</li>
     *   <li>{@link com.personal.happygallery.domain.product.InventoryPolicy#checkSufficient}로 수량 검증한다.</li>
     *   <li>수량을 차감하고 저장한다.</li>
     * </ol>
     *
     * <p>재고 부족 시 {@link com.personal.happygallery.common.error.InventoryNotEnoughException} (409).
     *
     * @param productId 상품 ID
     * @param qty       차감 수량
     * @return 차감 후 재고
     */
    public Inventory deduct(Long productId, int qty) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("재고"));
        inventory.deduct(qty);
        return inventoryRepository.save(inventory);
    }

    /**
     * 재고를 복구한다. 주문 거절/환불/자동취소 시 호출한다.
     *
     * @param productId 상품 ID
     * @param qty       복구 수량
     * @return 복구 후 재고
     */
    public Inventory restore(Long productId, int qty) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("재고"));
        inventory.restore(qty);
        return inventoryRepository.save(inventory);
    }
}

package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.Product;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final InventoryStorePort inventoryStorePort;
    private final SmartStoreStockSyncQueuePort smartStoreStockSyncQueuePort;
    private final Clock clock;

    public InventoryService(
            InventoryStorePort inventoryStorePort,
            SmartStoreStockSyncQueuePort smartStoreStockSyncQueuePort,
            Clock clock) {
        this.inventoryStorePort = inventoryStorePort;
        this.smartStoreStockSyncQueuePort = smartStoreStockSyncQueuePort;
        this.clock = clock;
    }

    /**
     * 재고 레코드를 생성한다. 상품 등록 시 호출한다.
     *
     * @param product  연결 상품
     * @param quantity 초기 수량
     * @return 생성된 재고
     */
    public Inventory create(Product product, int quantity) {
        Inventory saved = inventoryStorePort.save(new Inventory(product, quantity));
        requestSmartStoreSync(List.of(product.getId()));
        return saved;
    }

    /**
     * 재고를 차감한다.
     *
     * <ol>
     *   <li>비관적 락({@code SELECT FOR UPDATE})으로 재고 row를 잠근다.</li>
     *   <li>{@link Inventory#deduct(int)}로 수량 검증과 차감을 함께 처리한다.</li>
     *   <li>수량을 차감하고 저장한다.</li>
     * </ol>
     *
     * <p>재고 부족 시 {@link InventoryNotEnoughException} (409).
     *
     * @param productId 상품 ID
     * @param qty       차감 수량
     * @return 차감 후 재고
     */
    public Inventory deduct(Long productId, int qty) {
        return deductAll(List.of(new InventoryAdjustment(productId, qty))).getFirst();
    }

    /** 단일 상품의 부족 결과를 트랜잭션 밖으로 던지지 않아 채널 주문 확인 기록을 함께 저장한다. */
    public boolean tryDeduct(Long productId, int qty) {
        try {
            deduct(productId, qty);
            return true;
        } catch (InventoryNotEnoughException exception) {
            return false;
        }
    }

    /** 여러 상품 재고를 productId 순서로 한 번에 잠근 뒤 차감한다. */
    public List<Inventory> deductAll(List<InventoryAdjustment> adjustments) {
        return updateAll(adjustments, Inventory::deduct);
    }

    /**
     * 재고를 복구한다. 주문 거절/환불/자동취소 시 호출한다.
     *
     * @param productId 상품 ID
     * @param qty       복구 수량
     * @return 복구 후 재고
     */
    public Inventory restore(Long productId, int qty) {
        return restoreAll(List.of(new InventoryAdjustment(productId, qty))).getFirst();
    }

    /** 여러 상품 재고를 productId 순서로 한 번에 잠근 뒤 복구한다. */
    public List<Inventory> restoreAll(List<InventoryAdjustment> adjustments) {
        return updateAll(adjustments, Inventory::restore);
    }

    /** 관리자 수동 조정도 주문 차감과 같은 행 잠금으로 직렬화한다. */
    public InventoryChange adjust(Long productId, InventoryAdjustmentType type, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("재고 조정 수량은 1 이상이어야 합니다.");
        }

        Inventory inventory = inventoryStorePort.findByProductIdInWithLock(List.of(productId))
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException.supplier("재고"));
        int quantityBefore = inventory.getQuantity();
        switch (type) {
            case INCREASE -> inventory.restore(quantity);
            case DECREASE -> inventory.deduct(quantity);
        }
        Inventory saved = inventoryStorePort.save(inventory);
        requestSmartStoreSync(List.of(productId));
        return new InventoryChange(quantityBefore, saved.getQuantity());
    }

    private List<Inventory> updateAll(List<InventoryAdjustment> adjustments,
                                      BiConsumer<Inventory, Integer> update) {
        if (adjustments.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> quantitiesByProductId = new TreeMap<>();
        for (InventoryAdjustment adjustment : adjustments) {
            quantitiesByProductId.merge(
                    adjustment.productId(), adjustment.qty(),
                    Math::addExact);
        }

        List<Inventory> inventories = inventoryStorePort.findByProductIdInWithLock(
                List.copyOf(quantitiesByProductId.keySet()));
        if (inventories.size() != quantitiesByProductId.size()) {
            throw new NotFoundException("재고");
        }
        for (Inventory inventory : inventories) {
            update.accept(inventory, quantitiesByProductId.get(inventory.getProductId()));
        }

        List<Inventory> saved = List.copyOf(inventoryStorePort.saveAll(inventories));
        requestSmartStoreSync(saved.stream().map(Inventory::getProductId).toList());
        return saved;
    }

    private void requestSmartStoreSync(List<Long> productIds) {
        smartStoreStockSyncQueuePort.requestIfMapped(productIds, LocalDateTime.now(clock));
    }

    public record InventoryAdjustment(Long productId, int qty) {}

    public record InventoryChange(int quantityBefore, int quantityAfter) {}
}

package com.personal.happygallery.app.product;

import com.personal.happygallery.app.product.port.out.InventoryReaderPort;
import com.personal.happygallery.app.product.port.out.InventoryStorePort;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.infra.product.InventoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link InventoryRepository}(infra) → {@link InventoryReaderPort} + {@link InventoryStorePort}(app) 브릿지 어댑터.
 */
@Component
class InventoryPersistencePortAdapter implements InventoryReaderPort, InventoryStorePort {

    private final InventoryRepository inventoryRepository;

    InventoryPersistencePortAdapter(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    @Override
    public List<Inventory> findByProductIdIn(List<Long> productIds) {
        return inventoryRepository.findByProductIdIn(productIds);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    @Override
    public Optional<Inventory> findByProductIdWithLock(Long productId) {
        return inventoryRepository.findByProductIdWithLock(productId);
    }
}

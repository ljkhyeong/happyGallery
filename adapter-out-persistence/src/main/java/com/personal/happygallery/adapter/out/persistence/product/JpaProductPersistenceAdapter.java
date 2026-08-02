package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.InventoryAdjustmentHistoryPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.Product;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaProductPersistenceAdapter implements ProductStorePort,
        InventoryStorePort,
        InventoryAdjustmentHistoryPort {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    JpaProductPersistenceAdapter(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            InventoryAdjustmentRepository inventoryAdjustmentRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    @Override
    public List<Inventory> findByProductIdInWithLock(List<Long> productIds) {
        return inventoryRepository.findByProductIdInWithLock(productIds);
    }

    @Override
    public void deleteById(Long inventoryId) {
        inventoryRepository.deleteById(inventoryId);
    }

    @Override
    public InventoryAdjustment save(InventoryAdjustment adjustment) {
        return inventoryAdjustmentRepository.save(adjustment);
    }

    @Override
    public List<InventoryAdjustment> findRecentByProductId(Long productId) {
        return inventoryAdjustmentRepository.findRecentByProductId(productId);
    }
}

package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.application.product.port.out.InventoryAdjustmentHistoryPort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultProductAdminService implements ProductAdminUseCase {

    private final ProductStorePort productStorePort;
    private final ProductReaderPort productReaderPort;
    private final InventoryReaderPort inventoryReaderPort;
    private final InventoryAdjustmentHistoryPort adjustmentHistoryPort;
    private final InventoryService inventoryService;
    private final ImageMediaReferenceGuard imageMediaReferenceGuard;
    private final Clock clock;

    public DefaultProductAdminService(ProductStorePort productStorePort,
                                      ProductReaderPort productReaderPort,
                                      InventoryReaderPort inventoryReaderPort,
                                      InventoryAdjustmentHistoryPort adjustmentHistoryPort,
                                      InventoryService inventoryService,
                                      ImageMediaReferenceGuard imageMediaReferenceGuard,
                                      Clock clock) {
        this.productStorePort = productStorePort;
        this.productReaderPort = productReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.adjustmentHistoryPort = adjustmentHistoryPort;
        this.inventoryService = inventoryService;
        this.imageMediaReferenceGuard = imageMediaReferenceGuard;
        this.clock = clock;
    }

    /**
     * 상품을 등록한다.
     *
     * <ol>
     *   <li>Product 저장 (status=ACTIVE)</li>
     *   <li>Inventory 저장 (초기 수량)</li>
     * </ol>
     */
    @Override
    public ProductInventoryResult register(String name, ProductType type, String category, long price,
                                           int quantity, String description, String imageUrl,
                                           String specification, String careInstructions,
                                           Integer productionLeadDays) {
        Product product = new Product(
                name, type, category, price, description, imageUrl,
                specification, careInstructions, productionLeadDays);
        imageMediaReferenceGuard.validateAssignment(product.getImageUrl());
        product = productStorePort.save(product);
        Inventory inventory = inventoryService.create(product, quantity);
        return new ProductInventoryResult(product, inventory);
    }

    @Override
    public ProductInventoryResult update(Long productId, String name, String category, long price,
                                         String description, String imageUrl, String specification,
                                         String careInstructions, Integer productionLeadDays) {
        Product product = productReaderPort.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        product.updateDetails(
                name, category, price, description, imageUrl,
                specification, careInstructions, productionLeadDays);
        imageMediaReferenceGuard.validateAssignment(product.getImageUrl());
        Inventory inventory = inventoryReaderPort.findByProductId(productId)
                .orElseThrow(NotFoundException.supplier("재고"));
        return new ProductInventoryResult(productStorePort.save(product), inventory);
    }

    @Override
    public ProductInventoryResult changeStatus(Long productId, ProductStatus status) {
        Product product = productReaderPort.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        switch (status) {
            case ACTIVE -> product.activate();
            case INACTIVE -> product.deactivate();
        }
        Product saved = productStorePort.save(product);
        Inventory inventory = inventoryReaderPort.findByProductId(productId)
                .orElseThrow(NotFoundException.supplier("재고"));
        return new ProductInventoryResult(saved, inventory);
    }

    @Override
    public InventoryAdjustment adjustInventory(AdjustInventoryCommand command) {
        InventoryService.InventoryChange change = inventoryService.adjust(
                command.productId(), command.type(), command.quantity());
        return adjustmentHistoryPort.save(new InventoryAdjustment(
                command.productId(),
                command.type(),
                command.quantity(),
                change.quantityBefore(),
                change.quantityAfter(),
                command.reason(),
                command.adminUserId(),
                command.adjustedBy(),
                LocalDateTime.now(clock)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAdjustment> listRecentInventoryAdjustments(Long productId) {
        if (productReaderPort.findById(productId).isEmpty()) {
            throw new NotFoundException("상품");
        }
        return adjustmentHistoryPort.findRecentByProductId(productId);
    }
}

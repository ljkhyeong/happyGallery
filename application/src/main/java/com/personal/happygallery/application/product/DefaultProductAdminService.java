package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
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
    private final ProductVariantStockService variantStockService;
    private final ProductOptionConfigurationService optionConfigurationService;
    private final ImageMediaReferenceGuard imageMediaReferenceGuard;
    private final Clock clock;

    public DefaultProductAdminService(ProductStorePort productStorePort,
                                      ProductReaderPort productReaderPort,
                                      InventoryReaderPort inventoryReaderPort,
                                      InventoryAdjustmentHistoryPort adjustmentHistoryPort,
                                      InventoryService inventoryService,
                                      ProductVariantStockService variantStockService,
                                      ProductOptionConfigurationService optionConfigurationService,
                                      ImageMediaReferenceGuard imageMediaReferenceGuard,
                                      Clock clock) {
        this.productStorePort = productStorePort;
        this.productReaderPort = productReaderPort;
        this.inventoryReaderPort = inventoryReaderPort;
        this.adjustmentHistoryPort = adjustmentHistoryPort;
        this.inventoryService = inventoryService;
        this.variantStockService = variantStockService;
        this.optionConfigurationService = optionConfigurationService;
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
    public ProductResult register(SaveProductCommand command) {
        Product product = new Product(
                command.name(), command.type(), command.category(), command.price(),
                command.description(), command.imageUrl(), command.specification(),
                command.careInstructions(), command.productionLeadDays());
        imageMediaReferenceGuard.validateAssignment(product.getImageUrl());
        product = productStorePort.save(product);
        if (product.getType() == ProductType.READY_STOCK) {
            int quantity = requireReadyStockQuantity(command.quantity());
            Inventory inventory = inventoryService.create(product, quantity);
            return new ProductResult(product, inventory.getQuantity(), inventory.isAvailable(), ProductOptions.EMPTY);
        }
        inventoryService.create(product, 0);
        ProductOptions options = optionConfigurationService.configure(
                product, command.quantity(), command.optionGroups(), command.variants());
        return result(product, options);
    }

    @Override
    public ProductResult update(Long productId, SaveProductCommand command) {
        Product product = productReaderPort.findByIdWithLock(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        if (command.type() != null && command.type() != product.getType()) {
            throw new IllegalArgumentException("상품 유형은 변경할 수 없습니다.");
        }
        product.updateDetails(
                command.name(), command.category(), command.price(),
                command.description(), command.imageUrl(), command.specification(),
                command.careInstructions(), command.productionLeadDays());
        imageMediaReferenceGuard.validateAssignment(product.getImageUrl());
        Inventory inventory = inventoryReaderPort.findByProductId(productId)
                .orElseThrow(NotFoundException.supplier("재고"));
        Product saved = productStorePort.save(product);
        if (saved.getType() == ProductType.READY_STOCK) {
            return new ProductResult(
                    saved, inventory.getQuantity(), inventory.isAvailable(), ProductOptions.EMPTY);
        }
        ProductOptions options = optionConfigurationService.configure(
                saved, command.quantity(), command.optionGroups(), command.variants());
        return result(saved, options);
    }

    @Override
    public ProductResult changeStatus(Long productId, ProductStatus status) {
        Product product = productReaderPort.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        switch (status) {
            case ACTIVE -> product.activate();
            case INACTIVE -> product.deactivate();
        }
        Product saved = productStorePort.save(product);
        Inventory inventory = inventoryReaderPort.findByProductId(productId)
                .orElseThrow(NotFoundException.supplier("재고"));
        if (saved.getType() == ProductType.READY_STOCK) {
            return new ProductResult(
                    saved, inventory.getQuantity(), inventory.isAvailable(), ProductOptions.EMPTY);
        }
        return result(saved, optionConfigurationService.get(productId, true));
    }

    @Override
    public InventoryAdjustment adjustInventory(AdjustInventoryCommand command) {
        Product product = productReaderPort.findById(command.productId())
                .orElseThrow(NotFoundException.supplier("상품"));
        InventoryService.InventoryChange change;
        if (product.getType() == ProductType.MADE_TO_ORDER) {
            if (command.productVariantId() == null) {
                throw new IllegalArgumentException("주문제작 상품 재고 조정에는 옵션 조합이 필요합니다.");
            }
            change = variantStockService.adjust(
                    command.productId(), command.productVariantId(),
                    command.type(), command.quantity());
        } else {
            if (command.productVariantId() != null) {
                throw new IllegalArgumentException("기성품에는 옵션 조합 재고를 지정할 수 없습니다.");
            }
            change = inventoryService.adjust(
                    command.productId(), command.type(), command.quantity());
        }
        return adjustmentHistoryPort.save(new InventoryAdjustment(
                command.productId(),
                command.productVariantId(),
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

    private static int requireReadyStockQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("기성품 초기 재고는 1개 이상이어야 합니다.");
        }
        return quantity;
    }

    private static ProductResult result(Product product, ProductOptions options) {
        return new ProductResult(product, options.quantity(), options.available(), options);
    }
}

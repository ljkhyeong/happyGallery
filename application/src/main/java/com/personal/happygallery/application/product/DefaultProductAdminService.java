package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.SaveProductCommand;
import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.application.product.port.out.InventoryAdjustmentHistoryPort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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
    public ProductView register(SaveProductCommand command) {
        Product product = new Product(
                command.name(), command.type(), command.category(), command.price(),
                command.description(), command.imageUrl(), command.specification(),
                command.careInstructions(), command.productionLeadDays());
        imageMediaReferenceGuard.validateAssignment(product.getImageUrl());
        product = productStorePort.save(product);
        if (product.getType() == ProductType.READY_STOCK) {
            int quantity = requireReadyStockQuantity(command.quantity());
            Inventory inventory = inventoryService.create(product, quantity);
            return ProductView.readyStock(product, inventory);
        }
        inventoryService.create(product, 0);
        ProductOptions options = optionConfigurationService.configure(
                product, command.quantity(), command.optionGroups(), command.variants());
        return ProductView.withOptions(product, options);
    }

    @Override
    public ProductView update(Long productId, SaveProductCommand command) {
        Product product = productReaderPort.findByIdWithLock(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        if (command.type() != null && command.type() != product.getType()) {
            throw invalid("상품 유형은 변경할 수 없습니다.");
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
            return ProductView.readyStock(saved, inventory);
        }
        ProductOptions options = optionConfigurationService.configure(
                saved, command.quantity(), command.optionGroups(), command.variants());
        return ProductView.withOptions(saved, options);
    }

    @Override
    public ProductView changeStatus(Long productId, ProductStatus status) {
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
            return ProductView.readyStock(saved, inventory);
        }
        return ProductView.withOptions(saved, optionConfigurationService.get(productId, true));
    }

    @Override
    public InventoryAdjustment adjustInventory(AdjustInventoryCommand command) {
        Product product = productReaderPort.findById(command.productId())
                .orElseThrow(NotFoundException.supplier("상품"));
        InventoryService.InventoryChange change;
        if (product.getType() == ProductType.MADE_TO_ORDER) {
            if (command.productVariantId() == null) {
                throw invalid("주문제작 상품 재고 조정에는 옵션 조합이 필요합니다.");
            }
            change = variantStockService.adjust(
                    command.productId(), command.productVariantId(),
                    command.type(), command.quantity());
        } else {
            if (command.productVariantId() != null) {
                throw invalid("기성품에는 옵션 조합 재고를 지정할 수 없습니다.");
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
        if (!productReaderPort.existsById(productId)) {
            throw new NotFoundException("상품");
        }
        return adjustmentHistoryPort.findRecentByProductId(productId);
    }

    private static int requireReadyStockQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw invalid("기성품 초기 재고는 1개 이상이어야 합니다.");
        }
        return quantity;
    }

    /** 관리자가 고칠 수 있는 입력 오류라 500이 아니라 400 INVALID_INPUT으로 알린다. */
    private static HappyGalleryException invalid(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }
}

package com.personal.happygallery.application.order;

import com.personal.happygallery.application.product.InventoryService;
import com.personal.happygallery.application.product.InventoryService.InventoryAdjustment;
import com.personal.happygallery.application.product.ProductVariantStockService;
import com.personal.happygallery.application.product.ProductVariantStockService.VariantAdjustment;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderStockService {

    private final InventoryService inventoryService;
    private final ProductVariantStockService variantStockService;

    OrderStockService(
            InventoryService inventoryService,
            ProductVariantStockService variantStockService) {
        this.inventoryService = inventoryService;
        this.variantStockService = variantStockService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void deductAll(List<StockAdjustment> adjustments) {
        inventoryService.deductAll(productAdjustments(adjustments));
        variantStockService.deductAll(variantAdjustments(adjustments));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void restoreAll(List<StockAdjustment> adjustments) {
        inventoryService.restoreAll(productAdjustments(adjustments));
        variantStockService.restoreAll(variantAdjustments(adjustments));
    }

    private static List<InventoryAdjustment> productAdjustments(
            List<StockAdjustment> adjustments) {
        return adjustments.stream()
                .filter(adjustment -> adjustment.productVariantId() == null)
                .map(adjustment -> new InventoryAdjustment(
                        adjustment.productId(), adjustment.qty()))
                .toList();
    }

    private static List<VariantAdjustment> variantAdjustments(
            List<StockAdjustment> adjustments) {
        return adjustments.stream()
                .filter(adjustment -> adjustment.productVariantId() != null)
                .map(adjustment -> new VariantAdjustment(
                        adjustment.productVariantId(), adjustment.qty()))
                .toList();
    }

    record StockAdjustment(Long productId, Long productVariantId, int qty) {}
}

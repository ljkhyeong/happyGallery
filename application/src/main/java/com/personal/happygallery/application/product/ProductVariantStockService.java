package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.out.ProductVariantStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.ProductVariant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductVariantStockService {

    private final ProductVariantStorePort variantStorePort;

    public ProductVariantStockService(ProductVariantStorePort variantStorePort) {
        this.variantStorePort = variantStorePort;
    }

    public List<ProductVariant> deductAll(List<VariantAdjustment> adjustments) {
        return updateAll(adjustments, ProductVariant::deduct);
    }

    public List<ProductVariant> restoreAll(List<VariantAdjustment> adjustments) {
        return updateAll(adjustments, ProductVariant::restore);
    }

    public InventoryService.InventoryChange adjust(
            Long productId, Long variantId, InventoryAdjustmentType type, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("재고 조정 수량은 1 이상이어야 합니다.");
        }
        ProductVariant variant = variantStorePort.findByIdInWithLock(List.of(variantId)).stream()
                .findFirst()
                .filter(found -> found.getProductId().equals(productId))
                .orElseThrow(NotFoundException.supplier("상품 옵션 조합"));
        int quantityBefore = variant.getQuantity();
        switch (type) {
            case INCREASE -> variant.restore(quantity);
            case DECREASE -> variant.deduct(quantity);
        }
        ProductVariant saved = variantStorePort.save(variant);
        return new InventoryService.InventoryChange(quantityBefore, saved.getQuantity());
    }

    private List<ProductVariant> updateAll(
            List<VariantAdjustment> adjustments,
            BiConsumer<ProductVariant, Integer> update) {
        if (adjustments.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> quantitiesByVariantId = new TreeMap<>();
        for (VariantAdjustment adjustment : adjustments) {
            quantitiesByVariantId.merge(adjustment.variantId(), adjustment.qty(), Math::addExact);
        }
        List<ProductVariant> variants = variantStorePort.findByIdInWithLock(
                quantitiesByVariantId.keySet());
        if (variants.size() != quantitiesByVariantId.size()) {
            throw new NotFoundException("상품 옵션 조합");
        }
        for (ProductVariant variant : variants) {
            update.accept(variant, quantitiesByVariantId.get(variant.getId()));
        }
        return List.copyOf(variantStorePort.saveAll(variants));
    }

    public record VariantAdjustment(Long variantId, int qty) {}
}

package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.out.ProductVariantStorePort;
import com.personal.happygallery.application.product.port.out.SmartStoreStockSyncQueuePort;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.ProductVariant;
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
public class ProductVariantStockService {

    private final ProductVariantStorePort variantStorePort;
    private final SmartStoreStockSyncQueuePort smartStoreStockSyncQueuePort;
    private final Clock clock;

    public ProductVariantStockService(
            ProductVariantStorePort variantStorePort,
            SmartStoreStockSyncQueuePort smartStoreStockSyncQueuePort,
            Clock clock) {
        this.variantStorePort = variantStorePort;
        this.smartStoreStockSyncQueuePort = smartStoreStockSyncQueuePort;
        this.clock = clock;
    }

    public List<ProductVariant> deductAll(List<VariantAdjustment> adjustments) {
        return updateAll(adjustments, ProductVariant::deduct);
    }

    /** 단일 SKU만 시도하므로 재고 부족일 때 앞서 변경한 다른 SKU가 남지 않는다. */
    public boolean tryDeduct(Long variantId, int quantity) {
        try {
            deductAll(List.of(new VariantAdjustment(variantId, quantity)));
            return true;
        } catch (InventoryNotEnoughException exception) {
            return false;
        }
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
        requestSmartStoreSync(List.of(saved));
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
        List<ProductVariant> saved = List.copyOf(variantStorePort.saveAll(variants));
        requestSmartStoreSync(saved);
        return saved;
    }

    private void requestSmartStoreSync(List<ProductVariant> variants) {
        smartStoreStockSyncQueuePort.requestIfMapped(
                variants.stream().map(ProductVariant::getProductId).distinct().toList(),
                LocalDateTime.now(clock));
    }

    public record VariantAdjustment(Long variantId, int qty) {}
}

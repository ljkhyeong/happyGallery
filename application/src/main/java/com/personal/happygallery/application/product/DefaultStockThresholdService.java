package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.StockThresholdUseCase;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantStorePort;
import com.personal.happygallery.application.product.port.out.StockLevelReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultStockThresholdService implements StockThresholdUseCase {
    private final ProductReaderPort products;
    private final InventoryStorePort inventory;
    private final ProductVariantStorePort variants;
    private final StockLevelReaderPort levels;

    public DefaultStockThresholdService(ProductReaderPort products, InventoryStorePort inventory,
                                       ProductVariantStorePort variants, StockLevelReaderPort levels) {
        this.products = products;
        this.inventory = inventory;
        this.variants = variants;
        this.levels = levels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLevel> list(Long productId) { return levels.findStockLevels(productId); }

    @Override
    public void update(Long productId, Long productVariantId, Integer minimumStock, long expectedVersion) {
        var product = products.findByIdWithLock(productId).orElseThrow(NotFoundException.supplier("상품"));
        if (product.getType() == ProductType.READY_STOCK) {
            if (productVariantId != null) throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "기성품에는 옵션 조합을 지정할 수 없습니다.");
            var row = inventory.findByProductIdInWithLock(List.of(productId)).stream().findFirst()
                    .orElseThrow(NotFoundException.supplier("재고"));
            row.changeMinimumStock(minimumStock, expectedVersion);
            inventory.save(row);
        } else {
            if (productVariantId == null) throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "옵션 조합을 선택해 주세요.");
            var row = variants.findByIdInWithLock(List.of(productVariantId)).stream()
                    .filter(value -> value.getProductId().equals(productId)).findFirst()
                    .orElseThrow(NotFoundException.supplier("옵션 조합"));
            row.changeMinimumStock(minimumStock, expectedVersion);
            variants.save(row);
        }
    }
}

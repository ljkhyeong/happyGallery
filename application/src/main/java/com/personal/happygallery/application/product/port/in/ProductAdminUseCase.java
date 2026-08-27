package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.application.product.ProductOptions;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;

/**
 * 상품 관리 유스케이스.
 *
 * <p>운영자가 상품 상태와 온라인·오프라인 공유 재고를 관리한다.
 */
public interface ProductAdminUseCase {

    record ProductResult(Product product, long quantity, boolean available, ProductOptions options) {}

    record SaveProductCommand(
            String name,
            ProductType type,
            String category,
            long price,
            Integer quantity,
            String description,
            String imageUrl,
            String specification,
            String careInstructions,
            Integer productionLeadDays,
            List<OptionGroupDefinition> optionGroups,
            List<VariantDefinition> variants
    ) {
        public SaveProductCommand {
            optionGroups = optionGroups == null ? List.of() : List.copyOf(optionGroups);
            variants = variants == null ? List.of() : List.copyOf(variants);
        }
    }

    record OptionGroupDefinition(
            String key,
            ProductOptionType type,
            String name,
            boolean required,
            int sortOrder,
            String inputPlaceholder,
            Integer inputMaxLength,
            Long inputPriceAdjustment,
            List<OptionValueDefinition> values
    ) {
        public OptionGroupDefinition {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    record OptionValueDefinition(String key, String name, int sortOrder) {}

    record VariantDefinition(
            List<SelectionDefinition> selections,
            long priceAdjustment,
            int quantity,
            boolean active
    ) {
        public VariantDefinition {
            selections = selections == null ? List.of() : List.copyOf(selections);
        }
    }

    record SelectionDefinition(String groupKey, String valueKey) {}

    record AdjustInventoryCommand(
            Long productId,
            Long productVariantId,
            InventoryAdjustmentType type,
            int quantity,
            String reason,
            Long adminUserId,
            String adjustedBy
    ) {}

    /** 카테고리를 포함하여 상품 등록. */
    ProductResult register(SaveProductCommand command);

    ProductResult update(Long productId, SaveProductCommand command);

    ProductResult changeStatus(Long productId, ProductStatus status);

    InventoryAdjustment adjustInventory(AdjustInventoryCommand command);

    List<InventoryAdjustment> listRecentInventoryAdjustments(Long productId);
}

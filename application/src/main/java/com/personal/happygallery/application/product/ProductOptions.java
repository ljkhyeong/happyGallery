package com.personal.happygallery.application.product;

import com.personal.happygallery.domain.product.ProductOptionType;
import com.personal.happygallery.domain.product.Product;
import java.util.List;

public record ProductOptions(
        List<OptionGroup> groups,
        List<Variant> variants
) {
    public static final ProductOptions EMPTY = new ProductOptions(List.of(), List.of());

    public ProductOptions {
        groups = List.copyOf(groups);
        variants = List.copyOf(variants);
    }

    public long quantity() {
        long total = 0L;
        for (Variant variant : variants) {
            if (variant.active()) {
                total = Math.addExact(total, variant.quantity());
            }
        }
        return total;
    }

    public boolean available() {
        return variants.stream().anyMatch(variant -> variant.active() && variant.quantity() > 0);
    }

    public record OptionGroup(
            String key,
            ProductOptionType type,
            String name,
            boolean required,
            int sortOrder,
            String inputPlaceholder,
            Integer inputMaxLength,
            Long inputPriceAdjustment,
            List<OptionValue> values
    ) {
        public OptionGroup {
            values = List.copyOf(values);
        }
    }

    public record OptionValue(String key, String name, int sortOrder) {}

    public record Variant(
            Long id,
            long priceAdjustment,
            int quantity,
            boolean active,
            List<Selection> selections
    ) {
        public Variant {
            selections = List.copyOf(selections);
        }
    }

    public record Selection(String groupKey, String valueKey) {}

    public record TextInput(String groupKey, String value) {}

    public record ResolvedTextInput(
            Long groupId,
            String groupKey,
            String value,
            int sortOrder
    ) {}

    public record OptionSnapshot(
            ProductOptionType type,
            String groupName,
            String value,
            long priceAdjustment,
            int sortOrder
    ) {}

    public record ResolvedPurchase(
            Long variantId,
            long basePrice,
            long variantPriceAdjustment,
            long textOptionPriceAdjustment,
            long unitPrice,
            boolean variantActive,
            int availableQuantity,
            List<OptionSnapshot> optionSnapshots,
            List<ResolvedTextInput> textInputs
    ) {
        public ResolvedPurchase {
            optionSnapshots = List.copyOf(optionSnapshots);
            textInputs = List.copyOf(textInputs);
        }
    }

    public record PurchaseRequest(
            int index,
            Long productId,
            Long variantId,
            List<TextInput> textInputs
    ) {
        public PurchaseRequest {
            textInputs = textInputs == null ? List.of() : List.copyOf(textInputs);
        }
    }

    public record ResolvedLine(int index, Product product, ResolvedPurchase purchase) {}
}

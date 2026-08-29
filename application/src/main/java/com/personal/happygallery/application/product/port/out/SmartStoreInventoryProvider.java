package com.personal.happygallery.application.product.port.out;

import java.util.List;

public interface SmartStoreInventoryProvider {

    boolean isEnabled();

    SyncResult sync(StockCommand command);

    CatalogPage listProducts(int page, int size);

    ChannelProduct getProduct(Long originProductNo);

    SyncResult applyProduct(ProductCommand command);

    record StockCommand(
            Long originProductNo,
            Integer stockQuantity,
            List<OptionStock> options
    ) {
        public StockCommand {
            options = options == null ? List.of() : List.copyOf(options);
        }

        public boolean optionProduct() {
            return !options.isEmpty();
        }
    }

    record OptionStock(Long optionId, int stockQuantity) {}

    record ChannelProduct(long salePrice, String status, List<ChannelOption> options) {
        public ChannelProduct {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    record CatalogPage(
            List<CatalogProduct> products,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public CatalogPage {
            products = products == null ? List.of() : List.copyOf(products);
        }
    }

    record CatalogProduct(
            Long originProductNo,
            String name,
            String status,
            long salePrice,
            Integer stockQuantity,
            String imageUrl
    ) {}

    record ChannelOption(
            Long optionId,
            String name,
            int stockQuantity,
            long price,
            boolean usable
    ) {}

    record ProductCommand(
            Long originProductNo,
            long salePrice,
            String targetStatus,
            Integer stockQuantity,
            List<ProductOption> options
    ) {
        public ProductCommand {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    record ProductOption(Long optionId, int stockQuantity, long price, boolean usable) {}

    record SyncResult(boolean success, String reason) {
        public static SyncResult completed() {
            return new SyncResult(true, null);
        }

        public static SyncResult failure(String reason) {
            return new SyncResult(false, reason);
        }
    }
}

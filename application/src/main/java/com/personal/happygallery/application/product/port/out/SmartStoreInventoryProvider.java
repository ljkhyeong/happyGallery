package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.application.product.dto.SmartStoreCatalogPage;
import com.personal.happygallery.application.product.dto.SmartStoreChannelOption;
import com.personal.happygallery.application.product.dto.SmartStoreInspectionPage;
import java.util.List;

public interface SmartStoreInventoryProvider {

    boolean isEnabled();

    SyncResult sync(StockCommand command);

    SmartStoreCatalogPage listProducts(int page, int size);

    SmartStoreInspectionPage listInspectionProducts(int page, int size);

    void restoreInspectionProduct(Long channelProductNo);

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

    record ChannelProduct(long salePrice, String status, List<SmartStoreChannelOption> options) {
        public ChannelProduct {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

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

package com.personal.happygallery.application.product.port.out;

import java.util.List;

public interface SmartStoreInventoryProvider {

    boolean isEnabled();

    SyncResult sync(StockCommand command);

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

    record SyncResult(boolean success, String reason) {
        public static SyncResult completed() {
            return new SyncResult(true, null);
        }

        public static SyncResult failure(String reason) {
            return new SyncResult(false, reason);
        }
    }
}

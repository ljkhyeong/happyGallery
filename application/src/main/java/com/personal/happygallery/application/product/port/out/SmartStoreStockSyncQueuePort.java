package com.personal.happygallery.application.product.port.out;

import java.time.LocalDateTime;
import java.util.Collection;

public interface SmartStoreStockSyncQueuePort {

    void requestIfMapped(Collection<Long> productIds, LocalDateTime now);
}

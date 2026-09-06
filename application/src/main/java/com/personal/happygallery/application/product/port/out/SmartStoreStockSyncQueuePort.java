package com.personal.happygallery.application.product.port.out;

import java.time.LocalDateTime;
import java.util.Collection;

public interface SmartStoreStockSyncQueuePort {

    /** 활성 매핑이 있는 상품마다 중복 없이 한 번씩 동기화를 요청한다. */
    void requestIfMapped(Collection<Long> productIds, LocalDateTime now);
}

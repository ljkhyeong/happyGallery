package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.application.batch.BatchResult;

public interface SmartStoreStockSyncBatchUseCase {

    BatchResult syncPendingStocks();
}

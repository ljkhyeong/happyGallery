package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.application.product.StockLevel;
import java.util.List;

public interface StockThresholdUseCase {
    List<StockLevel> list(Long productId);
    void update(Long productId, Long productVariantId, Integer minimumStock, long expectedVersion);
}

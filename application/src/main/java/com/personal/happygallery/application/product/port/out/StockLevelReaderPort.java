package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.application.product.StockLevel;
import java.util.List;

public interface StockLevelReaderPort {
    List<StockLevel> findStockLevels(Long productId);
}

package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.InventoryAdjustment;
import java.util.List;

public interface InventoryAdjustmentHistoryPort {

    InventoryAdjustment save(InventoryAdjustment adjustment);

    List<InventoryAdjustment> findRecentByProductId(Long productId);
}

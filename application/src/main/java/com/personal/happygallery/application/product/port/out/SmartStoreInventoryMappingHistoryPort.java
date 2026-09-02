package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.SmartStoreInventoryMappingHistory;
import java.util.List;

public interface SmartStoreInventoryMappingHistoryPort {

    <S extends SmartStoreInventoryMappingHistory> S save(S history);

    List<SmartStoreInventoryMappingHistory> findRecentByProductId(Long productId);
}

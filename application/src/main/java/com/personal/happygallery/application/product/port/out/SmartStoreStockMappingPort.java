package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import java.util.List;

public interface SmartStoreStockMappingPort {

    List<SmartStoreStockMapping> findByProductIdOrderByProductVariantIdAsc(Long productId);

    <S extends SmartStoreStockMapping> List<S> saveAll(Iterable<S> mappings);

    void deleteByProductId(Long productId);
}

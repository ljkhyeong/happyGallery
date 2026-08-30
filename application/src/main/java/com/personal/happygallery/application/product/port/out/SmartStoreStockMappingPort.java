package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import java.util.List;
import java.util.Optional;

public interface SmartStoreStockMappingPort {

    List<SmartStoreStockMapping> findByProductIdOrderByProductVariantIdAsc(Long productId);

    Optional<SmartStoreStockMapping> findByOriginProductNoAndProductVariantIdIsNull(Long originProductNo);

    Optional<SmartStoreStockMapping> findByOriginProductNoAndOptionId(Long originProductNo, Long optionId);

    <S extends SmartStoreStockMapping> List<S> saveAll(Iterable<S> mappings);

    void deleteByProductId(Long productId);
}

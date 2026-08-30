package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.SmartStoreStockMappingPort;
import com.personal.happygallery.domain.product.SmartStoreStockMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreStockMappingRepository
        extends JpaRepository<SmartStoreStockMapping, Long>, SmartStoreStockMappingPort {

    @Override
    List<SmartStoreStockMapping> findByProductIdOrderByProductVariantIdAsc(Long productId);

    @Override
    Optional<SmartStoreStockMapping> findByOriginProductNoAndProductVariantIdIsNull(Long originProductNo);

    @Override
    Optional<SmartStoreStockMapping> findByOriginProductNoAndOptionId(Long originProductNo, Long optionId);

    @Override
    <S extends SmartStoreStockMapping> List<S> saveAll(Iterable<S> mappings);

    @Override
    @Modifying(flushAutomatically = true)
    @Query("delete from SmartStoreStockMapping mapping where mapping.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}

package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.ProductVariantReaderPort;
import com.personal.happygallery.application.product.port.out.ProductVariantStorePort;
import com.personal.happygallery.domain.product.ProductVariant;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>,
        ProductVariantReaderPort, ProductVariantStorePort {

    @Override
    @Query("""
            select distinct variant
              from ProductVariant variant
              left join fetch variant.selections
             where variant.id = :id
            """)
    Optional<ProductVariant> findWithSelectionsById(@Param("id") Long id);

    @Override
    @Query("""
            select distinct variant
              from ProductVariant variant
              left join fetch variant.selections
             where variant.productId in :productIds
             order by variant.productId, variant.id
            """)
    List<ProductVariant> findWithSelectionsByProductIdIn(
            @Param("productIds") Collection<Long> productIds);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct variant
              from ProductVariant variant
              left join fetch variant.selections
             where variant.id in :ids
             order by variant.id
            """)
    List<ProductVariant> findByIdInWithLock(@Param("ids") Collection<Long> ids);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct variant
              from ProductVariant variant
              left join fetch variant.selections
             where variant.productId = :productId
             order by variant.id
            """)
    List<ProductVariant> findByProductIdWithLock(@Param("productId") Long productId);
}

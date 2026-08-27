package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.ProductVariant;
import java.util.Collection;
import java.util.List;

public interface ProductVariantStorePort {

    <S extends ProductVariant> S save(S variant);

    <S extends ProductVariant> List<S> saveAll(Iterable<S> variants);

    List<ProductVariant> findByIdInWithLock(Collection<Long> ids);

    List<ProductVariant> findByProductIdWithLock(Long productId);
}

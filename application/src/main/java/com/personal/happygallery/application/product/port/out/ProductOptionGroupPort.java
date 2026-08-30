package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.ProductOptionGroup;
import java.util.Collection;
import java.util.List;

public interface ProductOptionGroupPort {

    List<ProductOptionGroup> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    List<ProductOptionGroup> findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(
            Collection<Long> productIds);

    <S extends ProductOptionGroup> List<S> saveAll(Iterable<S> groups);
}

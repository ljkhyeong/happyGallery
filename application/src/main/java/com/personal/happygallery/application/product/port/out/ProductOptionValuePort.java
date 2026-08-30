package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.ProductOptionValue;
import java.util.Collection;
import java.util.List;

public interface ProductOptionValuePort {

    List<ProductOptionValue> findByGroupIdInOrderByGroupIdAscSortOrderAscIdAsc(
            Collection<Long> groupIds);

    <S extends ProductOptionValue> List<S> saveAll(Iterable<S> values);
}

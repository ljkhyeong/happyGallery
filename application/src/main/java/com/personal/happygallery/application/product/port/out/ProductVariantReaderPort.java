package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.domain.product.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantReaderPort {

    Optional<ProductVariant> findWithSelectionsById(Long id);

    List<ProductVariant> findWithSelectionsByIdIn(Collection<Long> ids);

    List<ProductVariant> findWithSelectionsByProductId(Long productId);

    List<ProductVariant> findWithSelectionsByProductIdIn(Collection<Long> productIds);

    Optional<ProductVariant> findDefaultByProductId(Long productId);
}

package com.personal.happygallery.app.product.port.out;

import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import java.util.List;
import java.util.Optional;

/**
 * 상품 조회 포트.
 */
public interface ProductReaderPort {

    Optional<Product> findById(Long id);

    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);
}

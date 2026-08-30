package com.personal.happygallery.application.product.port.out;

import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.domain.product.Product;
import java.util.List;
import java.util.Optional;

/**
 * 상품 조회 포트.
 */
public interface ProductReaderPort {

    Optional<Product> findById(Long id);

    Optional<Product> findByIdWithLock(Long id);

    Optional<Product> findActiveById(Long id);

    List<Product> findAllById(Iterable<Long> ids);

    List<Product> findActiveProductsByCreatedAtDesc();

    List<Product> findAllProductsByCreatedAtDesc();

    /** 필터 조건에 따른 ACTIVE 상품 목록 조회. */
    List<Product> findActiveByFilter(ProductFilter filter);

    /** ACTIVE 상품의 카테고리 목록 (distinct, non-null). */
    List<String> findDistinctActiveCategories();
}

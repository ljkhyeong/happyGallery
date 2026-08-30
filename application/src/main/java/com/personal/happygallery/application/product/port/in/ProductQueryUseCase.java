package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.application.product.ProductOptions;
import com.personal.happygallery.domain.product.Product;
import java.util.List;

/**
 * 상품 조회 유스케이스.
 *
 * <p>공개 ACTIVE 상품 조회와 관리자 전체 상품 조회를 제공한다.
 */
public interface ProductQueryUseCase {

    record ProductView(Product product, long quantity, boolean available, ProductOptions options) {}

    ProductView getProduct(Long productId);

    /** 전체 ACTIVE 상품 (최신순). HomePage 등 필터 불필요 시 사용. */
    List<ProductView> listActiveProducts();

    /** 관리자용 전체 상품 (ACTIVE/INACTIVE, 최신순). */
    List<ProductView> listAllProducts();

    /** 필터 조건에 따른 ACTIVE 상품 목록. */
    List<ProductView> listActiveProducts(ProductFilter filter);

    /** ACTIVE 상품에 존재하는 카테고리 목록 (distinct). */
    List<String> listActiveCategories();
}

package com.personal.happygallery.app.product.port.in;

import com.personal.happygallery.app.product.ProductFilter;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import java.util.List;

/**
 * 상품 조회 유스케이스.
 *
 * <p>고객/관리자 공통으로 ACTIVE 상품 목록·단건 조회를 제공한다.
 */
public interface ProductQueryUseCase {

    record ProductWithInventory(Product product, Inventory inventory) {}

    ProductWithInventory getProduct(Long productId);

    /** 전체 ACTIVE 상품 (최신순). HomePage 등 필터 불필요 시 사용. */
    List<ProductWithInventory> listActiveProducts();

    /** 필터 조건에 따른 ACTIVE 상품 목록. */
    List<ProductWithInventory> listActiveProducts(ProductFilter filter);

    /** ACTIVE 상품에 존재하는 카테고리 목록 (distinct). */
    List<String> listActiveCategories();
}

package com.personal.happygallery.app.product.port.in;

import com.personal.happygallery.app.product.ProductQueryService.ProductWithInventory;
import java.util.List;

/**
 * 상품 조회 유스케이스.
 *
 * <p>고객/관리자 공통으로 ACTIVE 상품 목록·단건 조회를 제공한다.
 */
public interface ProductQueryUseCase {

    ProductWithInventory getProduct(Long productId);

    List<ProductWithInventory> listActiveProducts();
}

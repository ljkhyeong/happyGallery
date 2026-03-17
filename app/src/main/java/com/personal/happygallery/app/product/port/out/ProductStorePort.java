package com.personal.happygallery.app.product.port.out;

import com.personal.happygallery.domain.product.Product;

/**
 * 상품 저장 포트.
 */
public interface ProductStorePort {

    Product save(Product product);
}

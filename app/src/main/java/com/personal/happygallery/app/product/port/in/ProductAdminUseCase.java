package com.personal.happygallery.app.product.port.in;

import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;

/**
 * 상품 관리 유스케이스.
 *
 * <p>운영자가 상품을 등록한다.
 */
public interface ProductAdminUseCase {

    record RegisterResult(Product product, Inventory inventory) {}

    RegisterResult register(String name, ProductType type, long price, int quantity);
}

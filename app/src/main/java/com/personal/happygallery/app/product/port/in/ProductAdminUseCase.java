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

    /** 카테고리 없이 상품 등록. */
    default RegisterResult register(String name, ProductType type, long price, int quantity) {
        return register(name, type, null, price, quantity);
    }

    /** 카테고리를 포함하여 상품 등록. */
    RegisterResult register(String name, ProductType type, String category, long price, int quantity);
}

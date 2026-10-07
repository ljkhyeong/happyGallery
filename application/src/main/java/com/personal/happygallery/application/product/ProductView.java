package com.personal.happygallery.application.product;

import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;

/**
 * 공개 조회와 관리자 응답이 함께 쓰는 상품·재고 묶음.
 *
 * <p>기성품은 상품 단위 재고, 주문제작 상품은 옵션 조합 재고 합계를 수량으로 쓴다.
 */
public record ProductView(Product product, long quantity, boolean available, ProductOptions options) {

    public static ProductView readyStock(Product product, Inventory inventory) {
        return new ProductView(
                product, inventory.getQuantity(), inventory.isAvailable(), ProductOptions.EMPTY);
    }

    public static ProductView withOptions(Product product, ProductOptions options) {
        return new ProductView(product, options.quantity(), options.available(), options);
    }
}

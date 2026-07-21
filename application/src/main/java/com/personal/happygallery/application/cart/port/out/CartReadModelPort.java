package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;

public interface CartReadModelPort {

    List<CartItemDetail> findDetailsByUserId(Long userId);

    record CartItemDetail(
            Long cartItemId,
            Long productId,
            String productName,
            ProductType productType,
            long price,
            int qty,
            ProductStatus productStatus,
            Integer inventoryQuantity
    ) {}
}

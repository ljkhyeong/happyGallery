package com.personal.happygallery.application.cart.port.out;

import com.personal.happygallery.domain.product.ProductStatus;
import java.util.List;

public interface CartReadModelPort {

    List<CartItemDetail> findDetailsByUserId(Long userId);

    record CartItemDetail(
            Long productId,
            String productName,
            long price,
            int qty,
            ProductStatus productStatus,
            Integer inventoryQuantity
    ) {}
}

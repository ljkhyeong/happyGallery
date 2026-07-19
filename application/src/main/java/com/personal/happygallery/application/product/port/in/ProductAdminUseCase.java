package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;

/**
 * 상품 관리 유스케이스.
 *
 * <p>운영자가 상품 상태와 온라인·오프라인 공유 재고를 관리한다.
 */
public interface ProductAdminUseCase {

    record RegisterResult(Product product, Inventory inventory) {}

    record StatusChangeResult(Product product, Inventory inventory) {}

    record AdjustInventoryCommand(
            Long productId,
            InventoryAdjustmentType type,
            int quantity,
            String reason,
            Long adminUserId,
            String adjustedBy
    ) {}

    /** 카테고리를 포함하여 상품 등록. */
    RegisterResult register(String name, ProductType type, String category, long price, int quantity);

    StatusChangeResult changeStatus(Long productId, ProductStatus status);

    InventoryAdjustment adjustInventory(AdjustInventoryCommand command);

    List<InventoryAdjustment> listRecentInventoryAdjustments(Long productId);
}

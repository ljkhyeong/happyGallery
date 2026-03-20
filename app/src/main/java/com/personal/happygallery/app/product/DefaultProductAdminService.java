package com.personal.happygallery.app.product;

import com.personal.happygallery.app.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.app.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultProductAdminService implements ProductAdminUseCase {

    private final ProductStorePort productStorePort;
    private final InventoryService inventoryService;

    public DefaultProductAdminService(ProductStorePort productStorePort,
                                      InventoryService inventoryService) {
        this.productStorePort = productStorePort;
        this.inventoryService = inventoryService;
    }

    /**
     * 상품을 등록한다.
     *
     * <ol>
     *   <li>Product 저장 (status=ACTIVE)</li>
     *   <li>Inventory 저장 (초기 수량)</li>
     * </ol>
     *
     * @param name     상품명
     * @param type     상품 유형
     * @param price    가격 (원)
     * @param quantity 초기 재고 수량 (단일 작품은 1)
     */
    public RegisterResult register(String name, ProductType type, long price, int quantity) {
        Product product = productStorePort.save(new Product(name, type, price));
        Inventory inventory = inventoryService.create(product, quantity);
        return new RegisterResult(product, inventory);
    }

}

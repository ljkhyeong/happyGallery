package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
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
     */
    public RegisterResult register(String name, ProductType type, String category, long price, int quantity) {
        Product product = productStorePort.save(new Product(name, type, category, price));
        Inventory inventory = inventoryService.create(product, quantity);
        return new RegisterResult(product, inventory);
    }
}

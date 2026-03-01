package com.personal.happygallery.app.product;

import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductAdminService(ProductRepository productRepository,
                               InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
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
        Product product = productRepository.save(new Product(name, type, price));
        Inventory inventory = inventoryRepository.save(new Inventory(product, quantity));
        return new RegisterResult(product, inventory);
    }

    public record RegisterResult(Product product, Inventory inventory) {}
}

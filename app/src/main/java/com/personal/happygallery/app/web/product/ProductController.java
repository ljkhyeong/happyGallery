package com.personal.happygallery.app.web.product;

import com.personal.happygallery.app.web.product.dto.ProductDetailResponse;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductController(ProductRepository productRepository,
                             InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /** GET /products/{id} — 상품 상세 + 재고 가용 여부 */
    @GetMapping("/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("상품"));
        Inventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new NotFoundException("재고"));
        return ProductDetailResponse.from(product, inventory);
    }
}

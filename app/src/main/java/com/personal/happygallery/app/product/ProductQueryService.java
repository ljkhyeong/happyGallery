package com.personal.happygallery.app.product;

import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductQueryService(ProductRepository productRepository,
                               InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public record ProductWithInventory(Product product, Inventory inventory) {}

    /** 상품 단건 조회 */
    public ProductWithInventory getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품"));
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("재고"));
        return new ProductWithInventory(product, inventory);
    }

    /** ACTIVE 상품 목록 조회 — 최신 등록순 */
    public List<ProductWithInventory> listActiveProducts() {
        return productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE)
                .stream()
                .map(p -> {
                    Inventory inv = inventoryRepository.findByProductId(p.getId())
                            .orElseThrow(() -> new NotFoundException("재고"));
                    return new ProductWithInventory(p, inv);
                })
                .toList();
    }
}
